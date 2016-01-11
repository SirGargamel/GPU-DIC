/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.subset;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import cz.tul.dic.data.Coordinates;
import java.util.Arrays;

/**
 *
 * @author Petr Jecmen
 */
@XStreamAlias("SquareSubset")
public final class SquareSubset2D extends AbstractSubset {

    private static final int DIMENSION = 2;

    public SquareSubset2D(final int size, final double... center) {
        super(center, size, generateData(center, size));
    }

    private static int[] generateData(final double[] center, final int size) {
        if (center == null || center.length < 2) {
            throw new IllegalArgumentException("Illegal center data - " + Arrays.toString(center));
        }
        final int lineLenght = 2 * size + 1;
        final int pointCount = lineLenght * lineLenght;
        final int[] data = new int[pointCount * DIMENSION];
        final int[] localCenter = new int[]{
            (int) Math.round(center[0]), (int) Math.round(center[1])};
        int index = 0;
        for (int x = -size; x <= size; x++) {
            for (int y = -size; y <= size; y++) {
                data[index] = localCenter[Coordinates.X] + x;
                data[index + 1] = localCenter[Coordinates.Y] + y;

                index += DIMENSION;
            }
        }
        return data;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Subset -- ");
        sb.append(Arrays.toString(getCenter()));
        sb.append(", size : ");
        sb.append(getSize());
        return sb.toString();
    }

}
