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
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.data.task.splitter.TaskSplitter;
import cz.tul.dic.engine.opencl.DeviceManager;
import cz.tul.dic.engine.opencl.Kernel;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public final class CorrelationCalculator extends Observable {

    private static final int BEST_RESULT_COUNT_MAX = 50;
    private static final Utils.ResultCounter COUNTER;
    private final CLContext context;
    private final CLDevice device;
    // dynamic
    private KernelType kernelType;
    private Interpolation interpolation;
    private TaskSplitMethod taskSplitVariant;

    static {
        COUNTER = new Utils.ResultCounter();
    }

    public CorrelationCalculator() {
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
            List<CorrelationResult> bestSubResults = new ArrayList<>();
            while (it.hasNext()) {
                ct = it.next();
                ct.setResults(kernel.compute(ct.getImageA(), ct.getImageB(), ct.getFacets(), ct.getDeformationLimits(), defArrayLength));
                kernel.finishRound();
                // pick best results for this computation task and discard ct data 
                if (ct.isSubtask()) {
                    bestSubResults.addAll(ct.getResults());
                } else if (!bestSubResults.isEmpty()) {
                    // find best sub result, store it and find best global result
                    bestSubResults.addAll(ct.getResults());
                    // sort result according to correlation
                    Collections.sort(bestSubResults, (CorrelationResult o1, CorrelationResult o2) -> Float.compare(o1.getValue(), o2.getValue()));
                    Collections.reverse(bestSubResults);
                    // count results with same correlation
                    int count = 1;
                    for (int i = 1; i < bestSubResults.size(); i++) {
                        if (bestSubResults.get(i - 1).getValue() == bestSubResults.get(i).getValue()) {
                            count++;
                        } else {
                            break;
                        }
                    }
                    count = Math.min(count, BEST_RESULT_COUNT_MAX);
                    // keep only the best results
                    bestSubResults = bestSubResults.subList(0, count);
                    // store result
                    final int globalFacetIndex = facets.indexOf(ct.getFacets().get(0));
                    if (globalFacetIndex < 0) {
                        throw new IllegalArgumentException("Local facet not found in global registry.");
                    }
                    final double[][] bestResult = new double[bestSubResults.size()][];
                    for (int i = 0; i < bestSubResults.size(); i++) {
                        bestResult[i] = bestSubResults.get(i).getDeformation();
                    }
                    result.set(globalFacetIndex, bestResult);
                    bestSubResults.clear();
                } else {
                    pickBestResultsForTask(ct, result, facets);
                }
            }
        } catch (CLException ex) {
            throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, ex.getLocalizedMessage());
        }

        Logger.trace("Correlations computed.");
        return result;
    }

    private void pickBestResultsForTask(final ComputationTask task, final List<double[][]> bestResults, final List<Facet> globalFacets) throws ComputationException {
        final List<Facet> localFacets = task.getFacets();
        final int facetCount = localFacets.size();

        int globalFacetIndex;
        final List<CorrelationResult> taskResults = task.getResults();
        double[][] bestResult;
        for (int localFacetIndex = 0; localFacetIndex < facetCount; localFacetIndex++) {
            globalFacetIndex = globalFacets.indexOf(localFacets.get(localFacetIndex));
            if (globalFacetIndex < 0) {
                throw new IllegalArgumentException("Local facet not found in global registry.");
            }

            if (localFacetIndex >= taskResults.size()) {
                Logger.warn("No best value found for facet nr." + globalFacetIndex);
                bestResults.set(globalFacetIndex, new double[][]{{0, 0}});
            } else {
                bestResult = new double[][]{taskResults.get(localFacetIndex).getDeformation()};
                bestResults.set(globalFacetIndex, bestResult);
                COUNTER.inc(bestResult[0]);
            }
        }
    }

    public void setKernel(KernelType kernel) {
        this.kernelType = kernel;
    }

    public void setInterpolation(Interpolation interpolation) {
        this.interpolation = interpolation;
    }

    public void setTaskSplitVariant(TaskSplitMethod taskSplitVariant) {
        this.taskSplitVariant = taskSplitVariant;
    }

    public static void dumpCounterStats() {
        Logger.info(COUNTER);
        COUNTER.reset();
    }

}
