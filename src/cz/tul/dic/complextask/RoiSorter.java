/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.complextask;

import cz.tul.dic.data.roi.ROI;
import java.io.Serializable;
import java.util.Comparator;

/**
 *
 * @author Petr Jecmen
 */
public class RoiSorter implements Comparator<ROI>, Serializable {

    @Override
    public int compare(ROI o1, ROI o2) {
        final int y11 = o1.getY1();
        final int y12 = o1.getY2();
        final int center1 = (y11 + y12) / 2;
        final int y21 = o2.getY1();
        final int y22 = o2.getY2();

        final int result;
        if (y11 >= y21 && y11 <= y22 || y12 >= y21 && y12 <= y22 || center1 >= y21 && center1 <= y22) {
            result = Integer.compare(o1.getX1(), o2.getX1());
        } else {
            result = Integer.compare(y11, y21);
        }

        return result;
    }

}
