package cz.tul.dic.engine;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLException;
import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.Utils;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.splitter.TaskSplit;
import cz.tul.dic.data.task.splitter.TaskSplitter;
import cz.tul.dic.engine.opencl.DeviceManager;
import cz.tul.dic.engine.opencl.Kernel;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public final class Engine extends Observable {

    private static final int BEST_RESULT_COUNT_MAX = 50;
    private static final Utils.ResultCounter COUNTER;
    private final CLContext context;
    private final CLDevice device;
    // dynamic
    private KernelType kernelType;
    private Interpolation interpolation;
    private TaskSplit taskSplitVariant;

    static {
        COUNTER = new Utils.ResultCounter();
    }

    public Engine() {
        device = DeviceManager.getDevice();
        context = DeviceManager.getContext();

        kernelType = KernelType.CL_1D_I_V_LL_MC_D;
        interpolation = Interpolation.BILINEAR;
    }

    public List<double[][]> computeCorrelations(
            Image image1, Image image2,
            ROI roi, List<Facet> facets,
            double[] deformationLimits, DeformationDegree defDegree, int defArrayLength,
            int facetSize, Object taskSplitValue) throws ComputationException {
        final Kernel kernel = Kernel.createKernel(kernelType);
        Logger.trace("Kernel prepared.");

        final List<double[][]> result = computeCorrelations(image1, image2, roi, kernel, facets, deformationLimits, defArrayLength, defDegree, facetSize, taskSplitValue);

        kernel.finishComputation();

        return result;

    }

    private List<double[][]> computeCorrelations(
            Image image1, Image image2,
            ROI roi, final Kernel kernel, List<Facet> facets,
            double[] deformationLimits, int defArrayLength, DeformationDegree defDegree,
            int facetSize, Object taskSplitValue) throws ComputationException {
        final List<double[][]> result = new ArrayList<>(facets.size());
        for (int i = 0; i < facets.size(); i++) {
            result.add(null);
        }

        try {
            kernel.prepareKernel(context, device, facetSize, defDegree, defArrayLength, interpolation);

            final Iterator<ComputationTask> it = TaskSplitter.prepareSplitter(image1, image2, facets, deformationLimits, roi, taskSplitVariant, taskSplitValue);
            ComputationTask ct;
            List<double[]> bestSubResults = new ArrayList<>();
            while (it.hasNext()) {
                ct = it.next();
                ct.setResults(kernel.compute(ct.getImageA(), ct.getImageB(), ct.getFacets(), ct.getDeformations(), defArrayLength));
                kernel.finishRound();
                // pick best results for this computation task and discard ct data 
                if (ct.isSubtask()) {
                    bestSubResults.addAll(findBestSubResult(ct, defArrayLength));
                } else if (!bestSubResults.isEmpty()) {
                    // find best sub result, store it and find best global result
                    bestSubResults.addAll(findBestSubResult(ct, defArrayLength));
                    // sort result according to correlation
                    Collections.sort(bestSubResults, new Comparator<double[]>() {

                        @Override
                        public int compare(double[] o1, double[] o2) {
                            return Double.compare(o1[0], o2[0]);
                        }
                    });
                    Collections.reverse(bestSubResults);
                    // count results with same correlation
                    int count = 1;
                    for (int i = 1; i < bestSubResults.size(); i++) {
                        if (bestSubResults.get(i - 1)[0] == bestSubResults.get(i)[1]) {
                            count++;
                        } else {
                            break;
                        }
                    }
                    count = Math.min(count, BEST_RESULT_COUNT_MAX);
                    // keep only the best results
                    bestSubResults = bestSubResults.subList(0, count);
                    // remove correlation value
                    double[] val, newVal;
                    for (int i = 0; i < count; i++) {
                        val = bestSubResults.get(i);
                        newVal = new double[val.length - 1];
                        System.arraycopy(val, 1, newVal, 0, newVal.length);
                        bestSubResults.set(i, newVal);
                    }
                    // sort results according to absolute deformation
                    Collections.sort(bestSubResults, new DeformationSorter());
                    // sotre result
                    final int globalFacetIndex = facets.indexOf(ct.getFacets().get(0));
                    if (globalFacetIndex < 0) {
                        throw new IllegalArgumentException("Local facet not found in global registry.");
                    }
                    result.set(globalFacetIndex, bestSubResults.toArray(new double[][]{}));
                    bestSubResults.clear();
                } else {
                    pickBestResultsForTask(ct, result, facets, defArrayLength);
                }
            }
        } catch (CLException ex) {
            throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, ex.getLocalizedMessage());
        }

        Logger.trace("Correlations computed.");
        return result;
    }

    private List<double[]> findBestSubResult(final ComputationTask task, int defArrayLength) throws ComputationException {
        final double[] deformations = task.getDeformations();
        final int deformationCount = deformations.length / defArrayLength;
        final Comparator<Integer> candidatesComparator = new DeformationResultSorter(defArrayLength, deformations);
        final float[] taskResults = task.getResults();

        float val, best = -Float.MAX_VALUE;
        final List<Integer> candidates = new LinkedList<>();
        for (int def = 0; def < deformationCount; def++) {
            val = taskResults[def];
            if (val > best) {
                best = val;

                candidates.clear();
                candidates.add(def);
            } else if (val == best) {
                candidates.add(def);
            }
        }

        final List<double[]> result;
        if (candidates.isEmpty()) {
            Logger.warn("No best value found for sub task {0}", task);
            result = new ArrayList<>(1);
            result.add(new double[]{0, 0, 0});
        } else {
            if (candidates.size() > 1) {
                Collections.sort(candidates, candidatesComparator);
            }

            final int l = Math.min(candidates.size(), BEST_RESULT_COUNT_MAX);
            result = new ArrayList<>(l);
            double[] res, tmp;
            for (int i = 0; i < l; i++) {
                tmp = extractDeformation(candidates.get(i), deformations, defArrayLength);
                res = new double[defArrayLength + 1];
                res[0] = best;
                System.arraycopy(tmp, 0, res, 1, defArrayLength);
                result.add(res);
            }
        }

        return result;
    }

    private void pickBestResultsForTask(final ComputationTask task, final List<double[][]> bestResults, final List<Facet> globalFacets, int defArrayLength) throws ComputationException {
        final double[] deformations = task.getDeformations();
        final Comparator<Integer> candidatesComparator = new DeformationResultSorter(defArrayLength, deformations);

        final List<Facet> localFacets = task.getFacets();
        final int facetCount = localFacets.size();
        final int deformationCount = deformations.length / defArrayLength;

        float val, best;
        final List<Integer> candidates = new ArrayList<>();
        int baseIndex, globalFacetIndex, l;
        float[] taskResults;
        double[][] bestResult;
        for (int localFacetIndex = 0; localFacetIndex < facetCount; localFacetIndex++) {
            best = -Float.MAX_VALUE;

            taskResults = task.getResults();

            // DEBUG
//            System.out.println(Arrays.toString(taskResults));
//            final float[] sort = new float[taskResults.length];
//            System.arraycopy(taskResults, 0, sort, 0, taskResults.length);
//            Arrays.sort(sort);
//            System.out.println(Arrays.toString(sort));
            baseIndex = localFacetIndex * deformationCount;

            for (int def = 0; def < deformationCount; def++) {
                val = taskResults[baseIndex + def];
                if (val > best) {
                    best = val;

                    candidates.clear();
                    candidates.add(def);
                } else if (val == best) {
                    candidates.add(def);
                }
            }

            globalFacetIndex = globalFacets.indexOf(localFacets.get(localFacetIndex));
            if (globalFacetIndex < 0) {
                throw new IllegalArgumentException("Local facet not found in global registry.");
            }

            if (candidates.isEmpty()) {
                Logger.warn("No best value found for facet nr." + globalFacetIndex);
                bestResults.set(globalFacetIndex, new double[][]{{0, 0}});
            } else {
                if (candidates.size() > 1) {
                    Collections.sort(candidates, candidatesComparator);
                }

                l = Math.min(candidates.size(), BEST_RESULT_COUNT_MAX);
                bestResult = new double[l][];
                for (int i = 0; i < l; i++) {
                    bestResult[i] = extractDeformation(candidates.get(i), deformations, defArrayLength);
                }

                bestResults.set(globalFacetIndex, bestResult);
                COUNTER.inc(bestResult[0]);
            }
        }
    }

    private double[] extractDeformation(final int index, final double[] deformations, int defArrayLength) throws ComputationException {
        if (index < 0) {
            throw new IllegalArgumentException("Negative index not allowed.");
        }

        final double[] result = new double[defArrayLength];
        System.arraycopy(deformations, defArrayLength * index, result, 0, defArrayLength);

        return result;
    }

    public void setKernel(KernelType kernel) {
        this.kernelType = kernel;
    }

    public void setInterpolation(Interpolation interpolation) {
        this.interpolation = interpolation;
    }

    public void setTaskSplitVariant(TaskSplit taskSplitVariant) {
        this.taskSplitVariant = taskSplitVariant;
    }

    public static void dumpCounterStats() {
        Logger.info(COUNTER);
        COUNTER.reset();
    }

}
