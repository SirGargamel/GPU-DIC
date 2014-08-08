package cz.tul.dic.data.task;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import java.util.Set;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class TaskContainerChecker {    

    public static void checkTaskValidity(final TaskContainer tc) throws ComputationException {
        final Object in = tc.getParameter(TaskParameter.IN);
        if (in == null) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "No input.");
        }

        final Object roundData = tc.getParameter(TaskParameter.ROUND_LIMITS);
        if (roundData == null) {
            tc.setParameter(TaskParameter.ROUND_LIMITS, new int[]{0, tc.getImages().size() - 1});
        } else {
            final int[] limit = (int[]) roundData;
            if ((limit.length != 2) || (limit[0] > limit[1])) {
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Illegal limits for enabled rounds.");
            }
        }
        final int roundCount = TaskContainerUtils.getRounds(tc).size();
        if (roundCount < 1) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "No rounds for computation.");
        }

        final Object fs = tc.getParameter(TaskParameter.FACET_SIZE);
        final int facetSize;
        if (fs == null) {
            Logger.warn("Adding default facetSize.");
            facetSize = DefaultValues.DEFAULT_FACET_SIZE;
            tc.setParameter(TaskParameter.FACET_SIZE, facetSize);            
        } else {
            facetSize = (int) fs;
        }

        Image img;
        Set<ROI> rois;
        int fsz;
        double[] limits;
        for (int round : TaskContainerUtils.getRounds(tc).keySet()) {
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

                limits = tc.getDeformationLimits(round, roi);
                if (limits == null) {
                    Logger.warn("Adding default deformation limits for {0} in round {1}.", roi, round);
                    tc.setDeformationLimits(round, roi, DefaultValues.DEFAULT_DEFORMATION_LIMITS_FIRST);
                }
            }
        }

        final Object ts = tc.getParameter(TaskParameter.TASK_SPLIT_METHOD);
        if (ts == null) {
            Logger.warn("Adding default TaskSplit.");
            tc.setParameter(TaskParameter.TASK_SPLIT_METHOD, DefaultValues.DEFAULT_TASK_SPLIT_METHOD);
            tc.setParameter(TaskParameter.TASK_SPLIT_PARAM, DefaultValues.DEFAULT_TASK_SPLIT_PARAMETER);
        }

        final Object kernel = tc.getParameter(TaskParameter.KERNEL);
        if (kernel == null) {
            Logger.warn("Adding default kernel.");
            tc.setParameter(TaskParameter.KERNEL, DefaultValues.DEFAULT_KERNEL);
        }

        final Object facetGenMode = tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD);
        if (facetGenMode == null) {
            Logger.warn("Adding default facet generator.");
            tc.setParameter(TaskParameter.FACET_GENERATOR_METHOD, DefaultValues.DEFAULT_FACET_GENERATOR);
            tc.setParameter(TaskParameter.FACET_GENERATOR_PARAM, DefaultValues.DEFAULT_FACET_SPACING);
        }

        final Object interpolation = tc.getParameter(TaskParameter.INTERPOLATION);
        if (interpolation == null) {
            Logger.warn("Adding default interpolation.");
            tc.setParameter(TaskParameter.INTERPOLATION, DefaultValues.DEFAULT_INTERPOLATION);
        }

        final Object resultCompilation = tc.getParameter(TaskParameter.RESULT_COMPILATION);
        if (resultCompilation == null) {
            Logger.warn("Adding default result compilator.");
            tc.setParameter(TaskParameter.RESULT_COMPILATION, DefaultValues.DEFAULT_RESULT_COMPILATION);
        }

        final Object strainEstimation = tc.getParameter(TaskParameter.STRAIN_ESTIMATION_METHOD);
        if (strainEstimation == null) {
            Logger.warn("Adding default strain estimator.");
            tc.setParameter(TaskParameter.STRAIN_ESTIMATION_METHOD, DefaultValues.DEFAULT_STRAIN_ESTIMATION_METHOD);
            tc.setParameter(TaskParameter.STRAIN_ESTIMATION_PARAM, DefaultValues.DEFAULT_STRAIN_ESTIMATION_PARAMETER);
        }

        final Object displacementCalculator = tc.getParameter(TaskParameter.DISPLACEMENT_CALCULATION);
        if (displacementCalculator == null) {
            Logger.warn("Adding default displacement calculator.");
            tc.setParameter(TaskParameter.DISPLACEMENT_CALCULATION, DefaultValues.DEFAULT_DISPLACEMENT_CALCULATION);
        }
        
        final Object ws = tc.getParameter(TaskParameter.WINDOW_SIZE);
        if (ws == null) {
            Logger.warn("Adding default local search window size.");
            tc.setParameter(TaskParameter.WINDOW_SIZE, DefaultValues.DEFAULT_WINDOW_SIZE);
        }
    }

}
