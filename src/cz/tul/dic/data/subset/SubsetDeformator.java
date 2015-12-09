/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.subset;

import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.deformation.DeformationOrder;
import cz.tul.dic.data.deformation.DeformationDirection;
import cz.tul.dic.data.deformation.DeformationUtils;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Lenam s.r.o.
 */
public class SubsetDeformator {
    
    private final Map<int[], double[]> dataCache;
    
    public SubsetDeformator() {
        dataCache = new LinkedHashMap<>();
    }
    
    public Map<int[], double[]> deformSubset(final AbstractSubset subset, final double[] deformation) {
        final int[] data = subset.getData();
        final double[] center = subset.getCenter();
        final int subsetArea = data.length / Coordinates.DIMENSION;
        final DeformationOrder degree = DeformationUtils.getDegreeFromValue(deformation);

        if (dataCache.size() != subsetArea) {
            if (dataCache.size() > subsetArea) {
                dataCache.clear();
            }

            while (dataCache.size() < subsetArea) {
                dataCache.put(new int[Coordinates.DIMENSION], new double[Coordinates.DIMENSION]);
            }
        }

        int x, y, i = 0;
        double dx, dy;
        final double[] newCoords = new double[Coordinates.DIMENSION];
        int[] pos;
        double[] def;
        for (Map.Entry<int[], double[]> e : dataCache.entrySet()) {
            x = data[i * 2];
            y = data[i * 2 + 1];

            dx = x - center[Coordinates.X];
            dy = y - center[Coordinates.Y];

            deform(x, y, dx, dy, deformation, newCoords, degree);

            pos = e.getKey();
            pos[Coordinates.X] = x;
            pos[Coordinates.Y] = y;

            def = e.getValue();
            def[Coordinates.X] = newCoords[Coordinates.X];
            def[Coordinates.Y] = newCoords[Coordinates.Y];

            i++;
        }
        
        return dataCache;
    }
    
    public Map<int[], double[]> computePixelDeformationValues(final AbstractSubset subset, final double[] deformation) {
        final int[] data = subset.getData();
        final double[] center = subset.getCenter();
        final int subsetArea = data.length / Coordinates.DIMENSION;
        final DeformationOrder degree = DeformationUtils.getDegreeFromValue(deformation);

        if (dataCache.size() != subsetArea) {
            if (dataCache.size() > subsetArea) {
                dataCache.clear();
            }

            while (dataCache.size() < subsetArea) {
                dataCache.put(new int[Coordinates.DIMENSION], new double[Coordinates.DIMENSION]);
            }
        }

        int x, y, i = 0;
        double dx, dy;
        final double[] newCoords = new double[Coordinates.DIMENSION];
        int[] pos;
        double[] def;
        for (Map.Entry<int[], double[]> e : dataCache.entrySet()) {
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

        return dataCache;
    }
    
    private static void deform(final int x, final int y, final double dx, final double dy, final double[] deformation, final double[] result, final DeformationOrder degree) {
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
                throw new IllegalArgumentException("Unsupported degree of deformation - " + degree);
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
    
}
