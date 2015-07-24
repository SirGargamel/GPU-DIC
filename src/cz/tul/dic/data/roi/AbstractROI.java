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
public abstract class AbstractROI implements Serializable {

    protected static final String SEPARATOR = ";";

    public static AbstractROI generateROI(final String data) {
        final String[] split = data.split(SEPARATOR);
        AbstractROI result;
        if (split.length == 4) {
            result = new RectangleROI(Double.valueOf(split[0]), Double.valueOf(split[1]), Double.valueOf(split[2]), Double.valueOf(split[3]));
        } else if (split.length == 3) {
            result = new CircularROI(Double.valueOf(split[0]), Double.valueOf(split[1]), Double.valueOf(split[2]));
        } else {
            throw new IllegalArgumentException("3 or 4 values required for ROI - " + data);
        }
        return result;
    }

    public abstract double getX1();

    public abstract double getY1();

    public abstract double getX2();

    public abstract double getY2();

    public abstract double getWidth();

    public abstract double getHeight();

    public abstract boolean isAreaInside(double x1, double y1, double x2, double y2);
    
    public abstract boolean isPointInside(double x, double y);
    
    @Override
    public abstract String toString();

}
