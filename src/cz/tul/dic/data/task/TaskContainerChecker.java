package cz.tul.dic.data.task;

import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.splitter.TaskSplit;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.generators.facet.FacetGeneratorMode;
import java.util.Set;

/**
 *
 * @author Petr Jecmen
 */
public class TaskContainerChecker {

    public static void checkTaskValidity(final TaskContainer tc) {
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
                System.err.println("Adding default ROI.");
                tc.addRoi(new RectangleROI(0, 0, img.getWidth() - 1, img.getHeight() - 1), round);
            } else {
                for (ROI roi : rois) {
//                    if (roi.getX1() < 0 || roi.getY1() < 0) {
//                        throw new IllegalArgumentException("ROI coords must be positive.");
//                    }
//                    if (roi.getX2() >= img.getWidth() || roi.getY2() > img.getHeight()) {
//                        throw new IllegalArgumentException("ROI cannot be larger than image.");
//                    }
                }
            }
        }

        final Object ts = tc.getParameter(TaskParameter.TASK_SPLIT_VARIANT);
        if (ts == null) {
            System.err.println("Adding default TaskSplit.");
            tc.setParameter(TaskParameter.TASK_SPLIT_VARIANT, TaskSplit.NONE);
        }

        final Object kernel = tc.getParameter(TaskParameter.KERNEL);
        if (kernel == null) {
            System.err.println("Adding default kernel.");
            tc.setParameter(TaskParameter.KERNEL, KernelType.CL_1D_I_V_LL_MC_D);
        }
        
        final Object facetGenMode = tc.getParameter(TaskParameter.FACET_GENERATOR_MODE);
        if (facetGenMode == null) {
            System.err.println("Adding default facet generator.");
            tc.setParameter(TaskParameter.FACET_GENERATOR_MODE, FacetGeneratorMode.TIGHT);
            tc.setParameter(TaskParameter.FACET_GENERATOR_SPACING, 2);
        }
    }

}
