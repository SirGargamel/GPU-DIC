package cz.tul.dic.engine;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLException;
import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
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
import cz.tul.dic.engine.opencl.kernels.Kernel;
import cz.tul.dic.engine.opencl.kernels.KernelType;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public final class CorrelationCalculator extends Observable {

    private static final String CL_MEM_ERROR = "CL_OUT_OF_RESOURCES";
    private final CLContext context;
    private final CLDevice device;
    // dynamic
    private KernelType kernelType;
    private Interpolation interpolation;
    private TaskSplitMethod taskSplitVariant;
    private Kernel kernel;
    private boolean stop;

    public CorrelationCalculator() {

        device = DeviceManager.getDevice();
        context = DeviceManager.getContext();

        kernelType = TaskDefaultValues.DEFAULT_KERNEL;
        interpolation = TaskDefaultValues.DEFAULT_INTERPOLATION;
    }

    public List<CorrelationResult> computeCorrelations(
            Image image1, Image image2,
            ROI roi, List<Facet> facets,
            List<double[]> deformationLimits, DeformationDegree defDegree,
            int facetSize, Object taskSplitValue) throws ComputationException {
        stop = false;

        kernel = Kernel.createKernel(kernelType);
        Logger.trace("Kernel prepared - {0}", kernel);

        final List<CorrelationResult> result = computeCorrelations(image1, image2, roi, kernel, facets, deformationLimits, defDegree, facetSize, taskSplitValue);

        kernel.finishComputation();

        return result;

    }

    private List<CorrelationResult> computeCorrelations(
            Image image1, Image image2,
            ROI roi, final Kernel kernel, List<Facet> facets,
            List<double[]> deformationLimits, DeformationDegree defDegree,
            int facetSize, Object taskSplitValue) throws ComputationException {
        final List<CorrelationResult> result = new ArrayList<>(facets.size());
        for (int i = 0; i < facets.size(); i++) {
            result.add(null);
        }

        try {
            kernel.prepareKernel(context, device, facetSize, defDegree, interpolation);

            TaskSplitter ts = TaskSplitter.prepareSplitter(image1, image2, facets, deformationLimits, roi, taskSplitVariant, taskSplitValue);
            ts.resetTaskSize();
            boolean finished = false;
            CLException lastEx = null;
            while (ts.isSplitterReady() && !finished) {
                try {
                    ComputationTask ct;
                    CorrelationResult bestSubResult = null;
                    while (ts.hasNext()) {
                        if (stop) {
                            return result;
                        }

                        ct = ts.next();
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

                    finished = true;
                } catch (CLException ex) {
                    if (ex.getCLErrorString().contains(CL_MEM_ERROR)) {
                        ts.signalTaskSizeTooBig();
                        ts = TaskSplitter.prepareSplitter(image1, image2, facets, deformationLimits, roi, taskSplitVariant, taskSplitValue);
                        lastEx = ex;
                    } else {
                        throw ex;
                    }
                }
            }

            if (!finished && lastEx != null) {
                throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, lastEx.getLocalizedMessage());
            }
        } catch (CLException ex) {
            throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, ex.getLocalizedMessage());
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

    public void stop() {
        stop = true;
        if (kernel != null) {
            kernel.stop();
        }
        Logger.debug("Stopping correlation counter.");
    }

}
