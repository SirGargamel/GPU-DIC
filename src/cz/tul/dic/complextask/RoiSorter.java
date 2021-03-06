/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.complextask;

import cz.tul.dic.data.roi.AbstractROI;
import java.io.Serializable;
import java.util.Comparator;

/**
 *
 * @author Petr Jecmen
 */
public class RoiSorter implements Comparator<AbstractROI>, Serializable {

    @Override
    public int compare(final AbstractROI o1, final AbstractROI o2) {
        final double y11 = o1.getY1();
        final double y12 = o1.getY2();
        final double center1 = (y11 + y12) / 2;
        final double y21 = o2.getY1();
        final double y22 = o2.getY2();

        final int result;
        if (areROIsAreVerticallyAligned(y11, y21, y12, y22, center1)) {
            result = Double.compare(o1.getX1(), o2.getX1());
        } else {
            result = Double.compare(y11, y21);
        }

        return result;
    }

    private static boolean areROIsAreVerticallyAligned(final double y11, final double y21, final double y12, final double y22, final double center1) {
        return (y11 >= y21 && y11 <= y22)
                || (y12 >= y21 && y12 <= y22)
                || (y11 <= y21 && y21 >= y22)
                || (center1 >= y21 && center1 <= y22);
    }
}
