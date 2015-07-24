/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.deformation;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Petr Jecmen
 */
public final class DeformationUtils {

    private DeformationUtils() {
    }

    public static double getAbs(final double[] deformation) {
        double result = 0;
        for (double d : deformation) {
            result += d * d;
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

    public static long findMaxDeformationCount(final List<long[]> counts) {
        long max = 0;
        for (long[] iA : counts) {
            max = Math.max(max, iA[iA.length - 1]);
        }
        return max;
    }            

    public static List<long[]> generateDeformationCounts(final List<double[]> deformationLimits) {
        final List<long[]> result = new ArrayList<>(deformationLimits.size());
        for (double[] limits : deformationLimits) {
            result.add(generateDeformationCounts(limits));
        }
        return result;
    }

    public static long[] generateDeformationCounts(final double[] deformationLimits) {
        final int l = deformationLimits.length / 3;
        final long[] counts = new long[l + 1];

        long total = 1;
        for (int i = 0; i < l; i++) {
            counts[i] = Math.round((deformationLimits[i * 3 + 1] - deformationLimits[i * 3]) / deformationLimits[i * 3 + 2]) + 1;
            total *= counts[i];
        }
        counts[l] = total;
        return counts;
    }
    
    public static double[] extractDeformation(final int index, final double[] deformationLimits, final long[] deformationCounts) throws ComputationException {
        if (index < 0) {
            throw new IllegalArgumentException("Negative index not allowed.");
        }

        final int l = getDeformationCoeffCount(getDegreeFromLimits(deformationLimits));
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

    public static int getDeformationLimitsArrayLength(final DeformationDegree deg) {
        return getDeformationCoeffCount(deg) * 3;
    }

}
