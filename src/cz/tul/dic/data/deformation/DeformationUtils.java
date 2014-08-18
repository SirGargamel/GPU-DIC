package cz.tul.dic.data.deformation;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;

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

    public static DeformationDegree getDegreeFromLimits(final double[] limits) throws ComputationException {
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
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Illegal count of deformation limits - " + limits.length);
        }
        return result;
    }

    public static DeformationDegree getDegreeFromValue(final double[] deformation) throws ComputationException {
        final DeformationDegree result;
        switch (deformation.length) {
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
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Illegal count of deformations - " + deformation.length);
        }
        return result;
    }

    public static long calculateDeformationCount(final double[] limits) {
        final int coeffCount = limits.length / 3;
        long size = 1;
        size *= computeSize(limits, 0);
        size *= computeSize(limits, 3);
        if (coeffCount > 2) {
            size *= computeSize(limits, 6);
            size *= computeSize(limits, 9);
            size *= computeSize(limits, 12);
            size *= computeSize(limits, 15);
            if (coeffCount > 6) {
                size *= computeSize(limits, 18);
                size *= computeSize(limits, 21);
                size *= computeSize(limits, 24);
                size *= computeSize(limits, 27);
                size *= computeSize(limits, 30);
                size *= computeSize(limits, 33);
            }
        }
        return size;
    }

    private static int computeSize(final double[] limits, final int base) {
        final int result;
        if (limits[base + 2] != 0 && limits[base] != limits[base + 1]) {
            result = (int) ((limits[base + 1] - limits[base]) / limits[base + 2] + 1);
        } else {
            result = 1;
        }
        return result;
    }

}
