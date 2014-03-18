package cz.tul.dic.engine;

import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import java.util.Comparator;

/**
 *
 * @author Petr Jecmen
 */
public class DeformationResultSorter implements Comparator<Integer> {

    private final TaskContainer tc;
    private final int round;
    private final ROI roi;

    public DeformationResultSorter(TaskContainer tc, final int round, final ROI roi) {
        this.tc = tc;
        this.round = round;
        this.roi = roi;
    }

    @Override
    public int compare(Integer o1, Integer o2) {
        final double[] deformations = tc.getDeformations(round, roi);
        final int deformationLength = TaskContainerUtils.getDeformationArrayLength(tc, round, roi);
        return Double.compare(
                DeformationUtils.getAbs(deformations, o1, deformationLength),
                DeformationUtils.getAbs(deformations, o2, deformationLength));
    }

}
