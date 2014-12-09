package cz.tul.dic.data.deformation;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import java.util.ArrayList;
import java.util.List;

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

    public static double getAbs(final int deformationIndex, final double[] deformationLimits, final int[] deformationCounts) {
        return getAbs(extractDeformation(deformationIndex, deformationLimits, deformationCounts));
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
    
    public static int findMaxDeformationCount(final List<int[]> counts) {
        double max = 0;
        for (int[] iA : counts) {
            max = Math.max(max, iA[iA.length - 1]);
        }
        return (int) max;
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

    public static double[] extractDeformation(final int index, final double[] deformationLimits, final int[] deformationCounts) {
        if (index < 0) {
            throw new IllegalArgumentException("Negative index not allowed.");
        }

        final int l = deformationLimits.length / 3;
        final double[] result = new double[l];
        int counter = index;
        for (int i = 0; i < l; i++) {
            result[i] = counter % deformationCounts[i];
            counter /= deformationCounts[i];
        }
        for (int i = 0; i < l; i++) {
            result[i] = deformationLimits[i * 3] + result[i] * deformationLimits[i * 3 + 2];
        }

        return result;
    }

    public static List<int[]> generateDeformationCounts(final List<double[]> deformationLimits) {
        final List<int[]> result = new ArrayList<>(deformationLimits.size());
        for (double[] limits : deformationLimits) {
            result.add(generateDeformationCounts(limits));
        }
        return result;
    }
    
    public static int[] generateDeformationCounts(final double[] deformationLimits) {
        final int l = deformationLimits.length / 3;
        final int[] counts = new int[l + 1];

        int total = 1;
        for (int i = 0; i < l; i++) {
            counts[i] = (int) Math.round((deformationLimits[i * 3 + 1] - deformationLimits[i * 3]) / deformationLimits[i * 3 + 2]) + 1;
            total *= counts[i];
        }
        counts[l] = total;
        return counts;
    }
    
    public static int getDeformationCoeffCount(final DeformationDegree deg) {
        final int result;
        switch (deg) {
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
                throw new IllegalArgumentException("Unsupported degree of deformation.");
        }
        return result;
    }

}
