package cz.tul.dic.data.task;

import cz.tul.dic.data.deformation.DeformationDegree;

/**
 *
 * @author Petr Jecmen
 */
public class TaskContainerUtils {

    public static int getDeformationCount(final TaskContainer tc) {
        int result = tc.getDeformations().length;

        final DeformationDegree dd = (DeformationDegree) tc.getParameter(TaskParameter.DEFORMATION_DEGREE);
        switch (dd) {
            case ZERO:
                result /= 2;
                break;
            case FIRST:
                result /= 6;
                break;
            case SECOND:
                result /= 12;
                break;
            default:
                throw new IllegalArgumentException("Deformation parameters not set.");
        }

        return result;
    }
    
    public static double[] extractDeformation(final TaskContainer tc, final int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Negative index not allowed.");
        }
        
        final int size;
        final DeformationDegree dd = (DeformationDegree) tc.getParameter(TaskParameter.DEFORMATION_DEGREE);
        switch (dd) {
            case ZERO:
                size = 2;
                break;
            case FIRST:
                size = 6;
                break;
            case SECOND:
                size = 12;
                break;
            default:
                throw new IllegalArgumentException("Deformation parameters not set.");
        }
        
        final double[] result = new double[size];
        
        System.arraycopy(tc.getDeformations(), size * index, result, 0, size);
        
        return result;
    }
    
}
