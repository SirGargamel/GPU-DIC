package cz.tul.dic.data;

import cz.tul.dic.Coordinates;

/**
 *
 * @author Petr Jecmen
 */
public class Facet {

    private final int[] data;
    private final float[] center;
    private final int size;

    private Facet(int[] data, float[] center, int size) {
        this.data = data;
        this.center = center;
        this.size = size;
    }

    public int[] getData() {
        return data;
    }

    public float[] getCenter() {
        return center;
    }

    public int getSize() {
        return size;
    }

    public static Facet createFacet(int size, float... center) {
        if (center.length < Coordinates.DIMENSION) {
            throw new IllegalArgumentException("Not enough coordinates for facet center (" + (Coordinates.DIMENSION - center.length) + " more needed).");
        }

        final int halfSize = size / 2;
        final int centerX = (int) Math.floor(center[Coordinates.X]);
        final int topLeftX = centerX - halfSize;
        
        final int centerY = (int) Math.floor(center[Coordinates.Y]);
        final int topLeftY = centerY - halfSize;

        final int[] data = new int[size * size * Coordinates.DIMENSION];

        int index;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                index = (x * size + y) * Coordinates.DIMENSION;

                data[index] = topLeftX + x;
                data[index + 1] = topLeftY + y;
            }
        }
        final Facet result = new Facet(data, center, size);
        return result;
    }

}
