package cz.tul.dic.data;

import java.io.Serializable;

/**
 *
 * @author Petr Jecmen
 */
public class Facet implements Serializable {

    private final int[] data;
    private final double[] center;
    private final int size;

    private Facet(int[] data, double[] center, int size) {
        this.data = data;
        this.center = center;
        this.size = size;
    }

    public int[] getData() {
        return data;
    }

    public double[] getCenter() {
        return center;
    }

    public int getSize() {
        return size;
    }

    public static Facet createFacet(int size, int... topLeft) {
        if (topLeft.length < Coordinates.DIMENSION) {
            throw new IllegalArgumentException("Not enough coordinates for facet center (" + (Coordinates.DIMENSION - topLeft.length) + " more needed).");
        }

        final int halfSize = size / 2;
        final double centerX = topLeft[Coordinates.X] + halfSize;
        final double centerY = topLeft[Coordinates.Y] + halfSize;

        final int[] data = new int[size * size * Coordinates.DIMENSION];

        int index;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                index = (x * size + y) * Coordinates.DIMENSION;

                data[index] = topLeft[Coordinates.X] + x;
                data[index + 1] = topLeft[Coordinates.Y] + y;
            }
        }
        final Facet result = new Facet(data, new double[]{centerX, centerY}, size);
        return result;
    }

}
