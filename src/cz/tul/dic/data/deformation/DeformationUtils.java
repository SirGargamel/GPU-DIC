/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.deformation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Petr Jecmen
 */
public final class DeformationUtils {

    public static double getAbs(final double[] deformation) {
        double result = 0;
        for (double d : deformation) {
            result += d * d;
        }
        return Math.sqrt(result);
    }

    public static DeformationOrder getOrderFromLimits(final double[] limits) {
        final DeformationOrder result;
        switch (limits.length) {
            case 6:
                result = DeformationOrder.ZERO;
                break;
            case 18:
                result = DeformationOrder.FIRST;
                break;
            case 36:
                result = DeformationOrder.SECOND;
                break;
            default:
                throw new IllegalArgumentException("Illegal count of deformation limits - " + limits.length);
        }
        return result;
    }

    public static DeformationOrder getDegreeFromValue(final double[] deformation) {
        final DeformationOrder result;
        switch (deformation.length) {
            case 2:
                result = DeformationOrder.ZERO;
                break;
            case 6:
                result = DeformationOrder.FIRST;
                break;
            case 12:
                result = DeformationOrder.SECOND;
                break;
            default:
                throw new IllegalArgumentException("Illegal count of deformations - " + deformation.length);
        }
        return result;
    }

    public static long findMaxDeformationCount(final List<double[]> deformations, final DeformationOrder order, final boolean usesLimits) {
        long max = 0;
        if (usesLimits) {
            final List<long[]> counts = DeformationUtils.generateDeformationCounts(deformations);
            for (long[] iA : counts) {
                max = Math.max(max, iA[iA.length - 1]);
            }
        } else {
            final int coueffCount = DeformationUtils.getDeformationCoeffCount(order);
            for (double[] dA : deformations) {
                max = Math.max(max, dA.length / coueffCount);
            }
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
            if (deformationLimits[i * 3 + 2] != 0) {
                counts[i] = Math.round((deformationLimits[i * 3 + 1] - deformationLimits[i * 3]) / deformationLimits[i * 3 + 2]) + 1;
            } else {
                counts[i] = 1;
            }
            total *= counts[i];
        }
        counts[l] = total;
        return counts;
    }

    public static double[] extractDeformationFromLimits(final int index, final double[] deformationLimits, final long[] deformationCounts) {
        if (index < 0) {
            throw new IllegalArgumentException("Negative index not allowed.");
        }

        final int l = getDeformationCoeffCount(getOrderFromLimits(deformationLimits));
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

    public static double[] extractDeformationFromValues(final int index, final double[] deformationValues, final DeformationOrder order) {
        final int coeffCount = getDeformationCoeffCount(order);
        final double[] result = new double[coeffCount];
        System.arraycopy(deformationValues, index * coeffCount, result, 0, coeffCount);
        return result;
    }

    public static int getDeformationCoeffCount(final DeformationOrder deg) {
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

    public static int getDeformationLimitsArrayLength(final DeformationOrder deg) {
        return getDeformationCoeffCount(deg) * 3;
    }

    public static List<double[]> generateDeformationsFromLimits(final List<double[]> deformationLimits, final DeformationOrder order) {
        List<long[]> counts = DeformationUtils.generateDeformationCounts(deformationLimits);
        List<double[]> result = new ArrayList<>(deformationLimits.size());
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(order);
        double[] limits;
        double[] deformation;
        long[] count;
        long counter;
        List<double[]> deformations = new LinkedList<>();
        for (int i = 0; i < deformationLimits.size(); i++) {
            deformations.clear();
            limits = deformationLimits.get(i);
            count = counts.get(i);
            // generate deformations
            for (int j = 0; j < count[count.length - 1]; j++) {
                deformation = new double[coeffCount];
                counter = j;
                for (int k = 0; k < coeffCount; k++) {
                    deformation[k] = counter % count[k];
                    counter = counter / count[k];
                }
                for (int k = 0; k < coeffCount; k++) {
                    deformation[k] = limits[k * 3] + deformation[k] * limits[k * 3 + 2];
                }
                deformations.add(deformation);
            }
            result.add(condenseDeformations(deformations, coeffCount));
        }
        return result;
    }

    private static double[] condenseDeformations(final List<double[]> deformations, final int coeffCount) {
        final double[] result = new double[deformations.size() * coeffCount];
        double[] deformation;
        for (int i = 0; i < deformations.size(); i++) {
            deformation = deformations.get(i);
            System.arraycopy(deformation, 0, result, i * coeffCount, coeffCount);
        }
        return result;
    }

    private DeformationUtils() {
    }

}
