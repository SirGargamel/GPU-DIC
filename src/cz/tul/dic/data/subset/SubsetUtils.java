/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.subset;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.deformation.DeformationDirection;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Petr Jecmen
 */
public final class SubsetUtils {

    private static final Map<int[], double[]> CACHE;

    static {
        CACHE = new HashMap<>();
    }

    private SubsetUtils() {
    }

    public static Map<int[], double[]> deformSubset(final AbstractSubset subset, final double[] deformation) throws ComputationException {
        final int[] data = subset.getData();
        final double[] center = subset.getCenter();
        final int subsetArea = data.length / Coordinates.DIMENSION;
        final DeformationDegree degree = DeformationUtils.getDegreeFromValue(deformation);

        if (CACHE.size() != subsetArea) {
            if (CACHE.size() > subsetArea) {
                CACHE.clear();
            }

            while (CACHE.size() < subsetArea) {
                CACHE.put(new int[Coordinates.DIMENSION], new double[Coordinates.DIMENSION]);
            }
        }

        int x, y, i = 0;
        double dx, dy;
        final double[] newCoords = new double[Coordinates.DIMENSION];
        int[] pos;
        double[] def;
        for (Entry<int[], double[]> e : CACHE.entrySet()) {
            x = data[i * 2];
            y = data[i * 2 + 1];

            dx = x - center[Coordinates.X];
            dy = y - center[Coordinates.Y];

            deform(x, y, dx, dy, deformation, newCoords, degree);

            pos = e.getKey();
            pos[Coordinates.X] = x;
            pos[Coordinates.Y] = y;

            def = e.getValue();
            def[Coordinates.X] = newCoords[Coordinates.X] - x;
            def[Coordinates.Y] = newCoords[Coordinates.Y] - y;

            i++;
        }

        return CACHE;
    }

    private static void deform(final int x, final int y, final double dx, final double dy, final double[] deformation, final double[] result, final DeformationDegree degree) throws ComputationException {
        result[Coordinates.X] = x;
        result[Coordinates.Y] = y;
        switch (degree) {
            case SECOND:
                addZeroOrderDeformations(result, deformation);
                addFirstOrderDeformations(result, deformation, dx, dy);
                addSecondOrderDeformations(result, deformation, dx, dy);
                break;
            case FIRST:
                addZeroOrderDeformations(result, deformation);
                addFirstOrderDeformations(result, deformation, dx, dy);
                break;
            case ZERO:
                addZeroOrderDeformations(result, deformation);
                break;
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported degree of deformation.");
        }
    }

    private static double[] addZeroOrderDeformations(final double[] result, final double[] deformation) {
        result[Coordinates.X] += deformation[DeformationDirection.U];
        result[Coordinates.Y] += deformation[DeformationDirection.V];
        return result;
    }

    private static double[] addFirstOrderDeformations(final double[] result, final double[] deformation, final double dx, final double dy) {
        result[Coordinates.X] += deformation[DeformationDirection.UX] * dx + deformation[DeformationDirection.UY] * dy;
        result[Coordinates.Y] += deformation[DeformationDirection.VX] * dx + deformation[DeformationDirection.VY] * dy;
        return result;
    }

    private static double[] addSecondOrderDeformations(final double[] result, final double[] deformation, final double dx, final double dy) {
        result[Coordinates.X] += 0.5 * deformation[DeformationDirection.UXX] * dx * dx + 0.5 * deformation[DeformationDirection.UYY] * dy * dy + deformation[DeformationDirection.UXY] * dx * dy;
        result[Coordinates.Y] += 0.5 * deformation[DeformationDirection.VXX] * dx * dx + 0.5 * deformation[DeformationDirection.VYY] * dy * dy + deformation[DeformationDirection.VXY] * dx * dy;
        return result;
    }

    public static boolean isPointInsideSubset(final AbstractSubset f, final int x, final int y) {
        boolean result = false;

        final int[] pointData = f.getData();
        for (int i = 0; i < pointData.length / 2; i++) {
            if (x == pointData[i * 2] && y == pointData[i * 2 + 1]) {
                result = true;
                break;
            }
        }

        return result;
    }

    public static boolean areLinesInsideSubset(final AbstractSubset f, final int yStart, final int yEnd) {
        boolean result = false;

        final int[] pointData = f.getData();
        for (int i = 0; i < pointData.length / 2; i++) {
            if (pointData[i * 2 + 1] >= yStart && pointData[i * 2 + 1] <= yEnd) {
                result = true;
                break;
            }
        }

        return result;
    }

    public static int computeSubsetCoordCount(final int subsetSize) {
        return (subsetSize * 2 + 1) * (subsetSize * 2 + 1);
    }
    
    public static int computeSubsetWidth(final int subsetSize) {
        return subsetSize * 2 + 1;
    }
}
