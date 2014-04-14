package cz.tul.dic.engine;

import cz.tul.dic.ComputationException;
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
    private final double[] deformations;

    public DeformationResultSorter(TaskContainer tc, final int round, final ROI roi, final double[] deformations) {
        this.tc = tc;
        this.round = round;
        this.roi = roi;
        this.deformations = deformations;
    }

    @Override
    public int compare(Integer o1, Integer o2) {
        try {            
            final int deformationLength = TaskContainerUtils.getDeformationArrayLength(tc, round, roi);
            return Double.compare(
                    DeformationUtils.getAbs(deformations, o1, deformationLength),
                    DeformationUtils.getAbs(deformations, o2, deformationLength));
        } catch (ComputationException ex) {
            return 0;
        }
    }

}
