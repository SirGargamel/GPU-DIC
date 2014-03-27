package cz.tul.dic.data.task;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.splitter.TaskSplit;
import cz.tul.dic.engine.opencl.KernelType;
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
        final Object name = tc.getParameter(TaskParameter.NAME);
        if (name == null) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_CONTAINER, "no name");
        }
        
        final Object dir = tc.getParameter(TaskParameter.DIR);
        if (name == null) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_CONTAINER, "no dir");
        }

        // null data
        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        if (roundCount < 1) {
            throw new IllegalArgumentException("Not enough enabled input images.");
        }

        Image img;
        Set<ROI> rois;        
        for (int round = 0; round < roundCount; round++) {
            img = tc.getImage(round);
            if (img == null) {
                throw new IllegalArgumentException("NULL image found.");
            }

            rois = tc.getRois(round);
            if (rois == null || rois.isEmpty()) {
                Logger.warn("Adding default ROI.");
                tc.addRoi(new RectangleROI(0, 0, img.getWidth() - 1, img.getHeight() - 1), round);
            }
            
            for (ROI roi : tc.getRois(round)) {
                try {
                    tc.getFacetSize(round, roi);
                } catch (NullPointerException ex) {
                    tc.addFacetSize(round, roi, DEFAULT_FACET_SIZE);
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
    }

}
