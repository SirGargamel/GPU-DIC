package cz.tul.dic.engine;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLException;
import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
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
import java.util.List;
import java.util.Observable;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public final class Engine extends Observable {

    private static final int BEST_RESULT_COUNT_MAX = 50;
    private final CLContext context;
    private final CLDevice device;
    // dynamic
    private KernelType kernelType;
    private Interpolation interpolation;
    private TaskSplit taskSplitVariant;

    public Engine() {
        device = DeviceManager.getDevice();
        context = DeviceManager.getContext();

        kernelType = KernelType.CL_1D_I_V_LL_MC_D;
        interpolation = Interpolation.BILINEAR;
    }

    public List<double[][]> computeCorrelations(
            Image image1, Image image2,
            ROI roi, List<Facet> facets,
            double[] deformations, DeformationDegree defDegree, int defArrayLength,
            int facetSize, Object taskSplitValue) throws ComputationException {
        final Kernel kernel = Kernel.createKernel(kernelType);
        Logger.trace("Kernel prepared.");

        final List<double[][]> result = computeCorrelations(image1, image2, roi, kernel, facets, deformations, defArrayLength, defDegree, facetSize, taskSplitValue);

        kernel.finishComputation();

        return result;

    }

    private List<double[][]> computeCorrelations(
            Image image1, Image image2,
            ROI roi, final Kernel kernel, List<Facet> facets,
            double[] deformations, int defArrayLength, DeformationDegree defDegree,
            int facetSize, Object taskSplitValue) throws ComputationException {
        final List<double[][]> result = new ArrayList<>(facets.size());
        for (int i = 0; i < facets.size(); i++) {
            result.add(null);
        }

        try {
            kernel.prepareKernel(context, device, facetSize, defDegree, defArrayLength, interpolation);

            final Iterator<ComputationTask> it = TaskSplitter.prepareSplitter(image1, image2, facets, deformations, roi, taskSplitVariant, taskSplitValue);
            ComputationTask ct;
            while (it.hasNext()) {
                ct = it.next();
                ct.setResults(kernel.compute(ct.getImageA(), ct.getImageB(), ct.getFacets(), ct.getDeformations(), defArrayLength));
                kernel.finishRound();
                // pick best results for this computation task and discard ct data                          
                pickBestResultsForTask(ct, result, facets, deformations, defArrayLength);
            }
        } catch (CLException ex) {
            throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, ex.getCLErrorString());
        }

        Logger.trace("Correlations computed.");
        return result;
    }

    private void pickBestResultsForTask(final ComputationTask task, final List<double[][]> bestResults, final List<Facet> globalFacets, final double[] deformations, int defArrayLength) throws ComputationException {
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

}
