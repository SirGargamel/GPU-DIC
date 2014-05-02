package cz.tul.dic.data.task;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.splitter.TaskSplit;
import cz.tul.dic.engine.ResultCompilation;
import cz.tul.dic.engine.displacement.DisplacementCalculationType;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import cz.tul.dic.engine.strain.StrainEstimationType;
import cz.tul.dic.generators.facet.FacetGeneratorMode;
import java.util.Set;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class TaskContainerChecker {

    private static final int DEFAULT_FACET_SIZE = 10;

    public static void checkTaskValidity(final TaskContainer tc) throws ComputationException {
        final Object in = tc.getParameter(TaskParameter.IN);
        if (in == null) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "No input.");
        }

        final Object roundData = tc.getParameter(TaskParameter.ROUND_LIMITS);
        final int roundCount;
        if (roundData == null) {
            tc.setParameter(TaskParameter.ROUND_LIMITS, new int[]{0, tc.getImages().size() - 1});
        } else {
            final int[] limit = (int[]) roundData;
            if (limit.length % 2 != 0) {
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Illegal limits for enabled rounds.");
            }
            for (int i = 0; i < limit.length; i += 2) {
                if (limit[i + 1] <= limit[i]) {
                    throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Illegal limits for enabled rounds.");
                }
            }
        }
        roundCount = TaskContainerUtils.getRoundCount(tc);
        if (roundCount < 1) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "No rounds for computation.");
        }

        final Object fs = tc.getParameter(TaskParameter.FACET_SIZE);
        final int facetSize;
        if (fs == null) {
            Logger.warn("Adding default facetSize.");
            tc.setParameter(TaskParameter.FACET_SIZE, DEFAULT_FACET_SIZE);
            facetSize = DEFAULT_FACET_SIZE;
        } else {
            facetSize = (int) fs;
        }

        Image img;
        Set<ROI> rois;
        int fsz;
        for (int round = 0; round < roundCount; round++) {
            img = tc.getImage(round);
            if (img == null) {
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "NULL image found.");
            }

            rois = tc.getRois(round);
            if (rois == null || rois.isEmpty()) {
                Logger.warn("Adding default ROI.");
                tc.addRoi(round, new RectangleROI(0, 0, img.getWidth() - 1, img.getHeight() - 1));
            }

            for (ROI roi : tc.getRois(round)) {
                try {
                    fsz = tc.getFacetSize(round, roi);
                    if (fsz == -1) {
                        tc.addFacetSize(round, roi, facetSize);
                    }
                } catch (NullPointerException ex) {
                    tc.addFacetSize(round, roi, facetSize);
                }
            }
        }

        final Object ts = tc.getParameter(TaskParameter.TASK_SPLIT_VARIANT);
        if (ts == null) {
            Logger.warn("Adding default TaskSplit.");
            tc.setParameter(TaskParameter.TASK_SPLIT_VARIANT, TaskSplit.NONE);
        }

        final Object kernel = tc.getParameter(TaskParameter.KERNEL);
        if (kernel == null) {
            Logger.warn("Adding default kernel.");
            tc.setParameter(TaskParameter.KERNEL, KernelType.CL_1D_I_V_LL_MC_D);
        }

        final Object facetGenMode = tc.getParameter(TaskParameter.FACET_GENERATOR_MODE);
        if (facetGenMode == null) {
            Logger.warn("Adding default facet generator.");
            tc.setParameter(TaskParameter.FACET_GENERATOR_MODE, FacetGeneratorMode.TIGHT);
            tc.setParameter(TaskParameter.FACET_GENERATOR_SPACING, 2);
        }

        final Object interpolation = tc.getParameter(TaskParameter.INTERPOLATION);
        if (interpolation == null) {
            Logger.warn("Adding default interpolation.");
            tc.setParameter(TaskParameter.INTERPOLATION, Interpolation.BICUBIC);
        }

        final Object resultCompilation = tc.getParameter(TaskParameter.RESULT_COMPILATION);
        if (resultCompilation == null) {
            Logger.warn("Adding default result compilator.");
            tc.setParameter(TaskParameter.RESULT_COMPILATION, ResultCompilation.MAJOR_AVERAGING);
        }
        
        final Object strainEstimation = tc.getParameter(TaskParameter.STRAIN_ESTIMATION_METHOD);
        if (strainEstimation == null) {
            Logger.warn("Adding default strain estimator.");
            tc.setParameter(TaskParameter.STRAIN_ESTIMATION_METHOD, StrainEstimationType.LOCAL_LEAST_SQUARES);
        }
        
        final Object displacementCalculator = tc.getParameter(TaskParameter.DISPLACEMENT_CALCULATION_TYPE);
        if (displacementCalculator == null) {
            Logger.warn("Adding default displacement calculator.");
            tc.setParameter(TaskParameter.DISPLACEMENT_CALCULATION_TYPE, DisplacementCalculationType.FIND_MAX_AND_AVERAGE);
        }
    }

}
