package cz.tul.dic.data.deformation;

/**
 *
 * @author Petr Jecmen
 */
public class DeformationUtils {

    public static double getAbs(final double[] deformation) {
        double result = 0;
        for (double d : deformation) {
            result += d * d;
        }
        return Math.sqrt(result);
    }

    public static double getAbs(final double[] deformations, final int deformationIndex, final int deformationLength) {
        double result = 0;
        final int base = deformationIndex * deformationLength;
        for (int i = 0; i < deformationLength; i++) {
            result += deformations[base + i] * deformations[base + i];
        }
        return Math.sqrt(result);
    }

    public static DeformationDegree getDegreeFromLimits(final double[] limits) {
        final DeformationDegree result;
        switch (limits.length) {
            case 6:
                result = DeformationDegree.ZERO;
                break;
            case 18:
                result = DeformationDegree.FIRST;
                break;
            case 36:
                result = DeformationDegree.SECOND;
                break;
            default:
                throw new IllegalArgumentException("Illegal count of deofmation limits - " + limits.length);
        }
        return result;
    }
    
    public static DeformationDegree getDegree(final double[] limits) {
        final DeformationDegree result;
        switch (limits.length) {
            case 2:
                result = DeformationDegree.ZERO;
                break;
            case 6:
                result = DeformationDegree.FIRST;
                break;
            case 12:
                result = DeformationDegree.SECOND;
                break;
            default:
                throw new IllegalArgumentException("Illegal count of deofmation limits - " + limits.length);
        }
        return result;
    }

}
