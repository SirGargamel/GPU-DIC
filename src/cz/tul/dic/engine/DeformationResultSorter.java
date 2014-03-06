package cz.tul.dic.engine;

import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import java.util.Comparator;

/**
 *
 * @author Petr Jecmen
 */
public class DeformationResultSorter implements Comparator<Integer> {

    private final TaskContainer tc;

    public DeformationResultSorter(TaskContainer tc) {
        this.tc = tc;
    }

    @Override
    public int compare(Integer o1, Integer o2) {
//        final double[] def1 = TaskContainerUtils.extractDeformation(tc, o1);
//        final double[] def2 = TaskContainerUtils.extractDeformation(tc, o2);
//        
//        return Double.compare(DeformationUtils.getAbs(def1), DeformationUtils.getAbs(def2));
        
        final double[] deformations = tc.getDeformations();
        final int deformationLength = TaskContainerUtils.getDeformationArrayLength(tc);
        return Double.compare(
                DeformationUtils.getAbs(deformations, o1, deformationLength), 
                DeformationUtils.getAbs(deformations, o2, deformationLength));
    }
    
    

}
