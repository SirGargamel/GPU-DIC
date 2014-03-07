package cz.tul.dic.data.task;

import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationDegree;
import java.util.Map;

/**
 *
 * @author Petr Jecmen
 */
public class TaskContainerUtils {

    public static int getRoundCount(final TaskContainer tc) {
        int counter = 0;
        for (Image img : tc.getImages()) {
            if (img.isEnabled()) {
                counter++;
            }
        }
        return Math.max(counter - 1, 0);
    }
    
    public static int getDeformationCount(final TaskContainer tc) {
        final int deformationArrayLength = getDeformationArrayLength(tc);
        final int result = tc.getDeformations().length / deformationArrayLength;

        return result;
    }

    public static int getDeformationArrayLength(final TaskContainer tc) {
        int result;

        final DeformationDegree dd = (DeformationDegree) tc.getParameter(TaskParameter.DEFORMATION_DEGREE);
        switch (dd) {
            case ZERO:
                result = 2;
                break;
            case FIRST:
                result = 6;
                break;
            case SECOND:
                result = 12;
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

        final int deformationArrayLength = getDeformationArrayLength(tc);
        final double[] result = new double[deformationArrayLength];
        System.arraycopy(tc.getDeformations(), deformationArrayLength * index, result, 0, deformationArrayLength);

        return result;
    }
    
    public static TaskContainer loadTaskContainer(final Map<String, String> config) {
        // obtain input file name
        // create instance
        // facet size
        // ROIs
        // deformation
        // parameters
        // exports
        
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public static void storeTaskContainer(final TaskContainer tc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
