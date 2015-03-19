/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.roi;

import java.io.Serializable;

/**
 *
 * @author Petr Jecmen
 */
public abstract class ROI implements Serializable {

    static final String SEPARATOR = ";";

    public static ROI generateROI(final String data) {
        final String[] split = data.split(SEPARATOR);
        if (split.length == 4) {
            return new RectangleROI(Double.valueOf(split[0]), Double.valueOf(split[1]), Double.valueOf(split[2]), Double.valueOf(split[3]));
        } else if (split.length == 3) {
            return new CircularROI(Double.valueOf(split[0]), Double.valueOf(split[1]), Double.valueOf(split[2]));
        } else {
            throw new IllegalArgumentException("3 or 4 values required for ROI - " + data);
        }
    }

    public ROI() {

    }

    public abstract int getX1();

    public abstract int getY1();

    public abstract int getX2();

    public abstract int getY2();

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract boolean isAreaInside(double x1, double y1, double x2, double y2);
    
    public abstract boolean isPointInside(double x, double y);
    
    @Override
    public abstract String toString();

}
