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
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskDefaultValues;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.data.task.splitter.TaskSplitter;
import cz.tul.dic.engine.opencl.DeviceManager;
import cz.tul.dic.engine.opencl.Kernel;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public final class CorrelationCalculator extends Observable {

    private final Utils.ResultCounter roundCounterGood, roundCounterNotGood, roundQuality;
    private final Utils.ResultCounter counterGood, counterNotGood, quality;
    private final CLContext context;
    private final CLDevice device;
    // dynamic
    private KernelType kernelType;
    private Interpolation interpolation;
    private TaskSplitMethod taskSplitVariant;

    public CorrelationCalculator() {
        roundCounterGood = new Utils.ResultCounter();
        roundCounterNotGood = new Utils.ResultCounter();
        roundQuality = new Utils.ResultCounter();
        counterGood = new Utils.ResultCounter();
        counterNotGood = new Utils.ResultCounter();
        quality = new Utils.ResultCounter();

        device = DeviceManager.getDevice();
        context = DeviceManager.getContext();

        kernelType = TaskDefaultValues.DEFAULT_KERNEL;
        interpolation = TaskDefaultValues.DEFAULT_INTERPOLATION;
    }

    public List<CorrelationResult> computeCorrelations(
            Image image1, Image image2,
            ROI roi, List<Facet> facets,
            double[] deformationLimits, DeformationDegree defDegree,
            int facetSize, double resultQuality, Object taskSplitValue) throws ComputationException {
        final Kernel kernel = Kernel.createKernel(kernelType);
        Logger.trace("Kernel prepared - {0}", kernel);

        final List<CorrelationResult> result = computeCorrelations(image1, image2, roi, kernel, facets, deformationLimits, defDegree, facetSize, resultQuality, taskSplitValue);

        kernel.finishComputation();

        return result;

    }

    private List<CorrelationResult> computeCorrelations(
            Image image1, Image image2,
            ROI roi, final Kernel kernel, List<Facet> facets,
            double[] deformationLimits, DeformationDegree defDegree,
            int facetSize, double resultQuality, Object taskSplitValue) throws ComputationException {
        final List<CorrelationResult> result = new ArrayList<>(facets.size());
        for (int i = 0; i < facets.size(); i++) {
            result.add(null);
        }
        
        try {
            kernel.prepareKernel(context, device, facetSize, defDegree, interpolation);

            final Iterator<ComputationTask> it = TaskSplitter.prepareSplitter(image1, image2, facets, deformationLimits, roi, taskSplitVariant, taskSplitValue);
            ComputationTask ct;
            CorrelationResult bestSubResult = null;
            while (it.hasNext()) {
                ct = it.next();
                ct.setResults(kernel.compute(ct.getImageA(), ct.getImageB(), ct.getFacets(), ct.getDeformationLimits(), DeformationUtils.getDeformationArrayLength(defDegree)));
                kernel.finishRound();
                // pick best results for this computation task and discard ct data 
                if (ct.isSubtask()) {
                    bestSubResult = pickBetterResult(bestSubResult, ct.getResults().get(0));
                } else if (bestSubResult != null) {
                    bestSubResult = pickBetterResult(bestSubResult, ct.getResults().get(0));
                    // store result
                    final int globalFacetIndex = facets.indexOf(ct.getFacets().get(0));
                    if (globalFacetIndex < 0) {
                        throw new IllegalArgumentException("Local facet not found in global registry.");
                    }
                    result.set(globalFacetIndex, bestSubResult);
                    bestSubResult = null;
                } else {
                    pickBestResultsForTask(ct, result, facets);
                }
            }
        } catch (CLException ex) {
            throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, ex.getLocalizedMessage());
        }

        CorrelationResult cr;
        int val, count = 0;
        for (int i = 0; i < facets.size(); i++) {
            cr = result.get(i);
            if (cr != null) {
                val = (int) (cr.getValue() * 10);
                roundQuality.inc(val / (double) 10);
                quality.inc(val / (double) 10);
                if (cr.getValue() < resultQuality) {
                    result.set(i, null);
                    roundCounterNotGood.inc(cr.getDeformation());
                    counterNotGood.inc(cr.getDeformation());
                    count++;
                } else {
                    roundCounterGood.inc(cr.getDeformation());
                    counterGood.inc(cr.getDeformation());
                }
            } else {
                roundCounterNotGood.inc();
                roundQuality.inc();
                counterNotGood.inc();
                quality.inc();
            }
        }
        if (count > 0) {
            Logger.warn("Found {0} result with quality lower than {1} (for ROI {2}).", count, resultQuality, roi);
        }

        Logger.trace("{0} correlations computed.", result.size());
        return result;
    }

    private CorrelationResult pickBetterResult(final CorrelationResult r1, final CorrelationResult r2) {
        final CorrelationResult result;
        if (r1 == null) {
            result = r2;
        } else if (r1.getValue() == r2.getValue()) {
            result = DeformationUtils.getAbs(r1.getDeformation()) < DeformationUtils.getAbs(r2.getDeformation()) ? r1 : r2;
        } else {
            result = r1.getValue() > r2.getValue() ? r1 : r2;
        }
        return result;
    }

    private void pickBestResultsForTask(final ComputationTask task, final List<CorrelationResult> bestResults, final List<Facet> globalFacets) throws ComputationException {
        final List<Facet> localFacets = task.getFacets();
        final int facetCount = localFacets.size();

        int globalFacetIndex;
        final List<CorrelationResult> taskResults = task.getResults();
        for (int localFacetIndex = 0; localFacetIndex < facetCount; localFacetIndex++) {
            globalFacetIndex = globalFacets.indexOf(localFacets.get(localFacetIndex));
            if (globalFacetIndex < 0) {
                throw new IllegalArgumentException("Local facet not found in global registry.");
            }

            if (localFacetIndex >= taskResults.size()) {
                Logger.warn("No best value found for facet nr." + globalFacetIndex);
                bestResults.set(globalFacetIndex, new CorrelationResult(-1, null));
            } else {
                bestResults.set(globalFacetIndex, taskResults.get(localFacetIndex));
                roundCounterGood.inc(bestResults.get(globalFacetIndex).getDeformation());
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

    public void dumpRoundCounterStats() {
        final StringBuilder sb = new StringBuilder();
        sb.append("--- Resulting deformations statistics --- ROUND\n");
        sb.append("-- GOOD --");
        sb.append(roundCounterGood.toString());
        sb.append("\n-- NOT GOOD --");
        sb.append(roundCounterNotGood.toString());
        sb.append("\n-- QUALITY STATS --");
        sb.append(roundQuality.toString());
        Logger.trace(sb.toString());
        roundCounterGood.reset();
        roundCounterNotGood.reset();
        roundQuality.reset();
    }

    public void dumpTaskCounterStats() {
        final StringBuilder sb = new StringBuilder();
        sb.append("--- Resulting deformations statistics --- TASK\n");
        sb.append("-- GOOD --");
        sb.append(counterGood.toString());
        sb.append("\n-- NOT GOOD --");
        sb.append(counterNotGood.toString());
        sb.append("\n-- QUALITY STATS --");
        sb.append(quality.toString());
        Logger.trace(sb.toString());
        counterGood.reset();
        counterNotGood.reset();
        quality.reset();
    }

    public void enableStatLogging(boolean enable) {
        counterGood.setEnabled(enable);
        roundCounterGood.setEnabled(enable);
        counterNotGood.setEnabled(enable);
        roundCounterNotGood.setEnabled(enable);                
        quality.setEnabled(enable);
        roundQuality.setEnabled(enable);
    }

}
