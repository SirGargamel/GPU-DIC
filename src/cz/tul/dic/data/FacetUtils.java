/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.deformation.Deformation;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Petr Jecmen
 */
public class FacetUtils {

    private static final Map<int[], double[]> CACHE;

    static {
        CACHE = new HashMap<>();
    }

    public static Map<int[], double[]> deformFacet(final Facet facet, final double[] deformation) throws ComputationException {
        final int[] data = facet.getData();
        final double[] center = facet.getCenter();
        final int facetArea = data.length / Coordinates.DIMENSION;
        final DeformationDegree degree = DeformationUtils.getDegreeFromValue(deformation);

        if (CACHE.size() != facetArea) {
            if (CACHE.size() > facetArea) {
                CACHE.clear();
            }

            while (CACHE.size() < facetArea) {
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
                result[Coordinates.X] += 0.5 * deformation[Deformation.UXX] * dx * dx + 0.5 * deformation[Deformation.UYY] * dy * dy + deformation[Deformation.UXY] * dx * dy;
                result[Coordinates.Y] += 0.5 * deformation[Deformation.VXX] * dx * dx + 0.5 * deformation[Deformation.VYY] * dy * dy + deformation[Deformation.VXY] * dx * dy;
            case FIRST:
                result[Coordinates.X] += deformation[Deformation.UX] * dx + deformation[Deformation.UY] * dy;
                result[Coordinates.Y] += deformation[Deformation.VX] * dx + deformation[Deformation.VY] * dy;
            case ZERO:
                result[Coordinates.X] += deformation[Deformation.U];
                result[Coordinates.Y] += deformation[Deformation.V];
                break;
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported degree of deformation.");

        }
    }

    public static boolean isPointInsideFacet(final Facet f, final int x, final int y) {
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
    
    public static boolean areLinesInsideFacet(final Facet f, final int yStart, final int yEnd) {
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

}
