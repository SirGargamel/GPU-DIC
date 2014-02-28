package cz.tul.dic.data;

import cz.tul.dic.data.deformation.DeformationDegree;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Petr Jecmen
 */
public class FacetUtils {

    public static Map<int[], double[]> deformFacet(final Facet facet, final double[] deformation, final DeformationDegree degree) {
        final int[] data = facet.getData();
        final float[] center = facet.getCenter();
        final int facetArea = data.length / Coordinates.DIMENSION;

        final Map<int[], double[]> result = new HashMap<>(facetArea);

        int x, y;
        float dx, dy;
        final float[] newCoords = new float[Coordinates.DIMENSION];
        for (int i = 0; i < facetArea; i++) {
            x = data[i * 2];
            y = data[i * 2 + 1];

            dx = x - center[Coordinates.X];
            dy = y - center[Coordinates.Y];

            deform(x, y, dx, dy, deformation, newCoords, degree);

            result.put(new int[]{x, y}, new double[]{newCoords[Coordinates.X] - x, newCoords[Coordinates.Y] - y});
        }

        return result;
    }

    private static void deform(final int x, final int y, final float dx, final float dy, final double[] deformation, final float[] result, final DeformationDegree degree) {
        result[Coordinates.X] = x;
        result[Coordinates.Y] = y;
        switch (degree) {
            case FIRST:
                result[Coordinates.X] += deformation[2] * dx + deformation[4] * dy;
                result[Coordinates.Y] += deformation[3] * dx + deformation[5] * dy;
            case ZERO:
                result[Coordinates.X] += deformation[0];
                result[Coordinates.Y] += deformation[1];
                break;
            case SECOND:
            default:
                throw new IllegalArgumentException("Unsupported degree of deformation.");

        }
    }

}
