/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.subset;

/**
 *
 * @author Petr Jecmen
 */
public final class SubsetUtils {

    private SubsetUtils() {
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
