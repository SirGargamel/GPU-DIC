/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.roi;

import java.io.Serializable;

public class CircularROI extends AbstractROI implements Serializable {

    private final double centerX, centerY, radius;

    public CircularROI(final double centerX, final double centerY, final double radius) {
        super();

        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
    }

    @Override
    public int getX1() {
        return (int) Math.floor(centerX - radius);
    }

    @Override
    public int getY1() {
        return (int) Math.floor(centerY - radius);
    }

    @Override
    public int getX2() {
        return (int) Math.ceil(centerX + radius);
    }

    @Override
    public int getY2() {
        return (int) Math.ceil(centerY + radius);
    }

    @Override
    public int getWidth() {
        return (int) Math.ceil(2 * radius);
    }

    @Override
    public int getHeight() {
        return (int) Math.ceil(2 * radius);
    }

    @Override
    public boolean isAreaInside(final double x1, final double y1, final double x2, final double y2) {
        final double maxDist2 = radius * radius;
        
        return dist2(x1, y1, centerX, centerY) <= maxDist2
                && dist2(x1, y2, centerX, centerY) <= maxDist2
                && dist2(x2, y1, centerX, centerY) <= maxDist2
                && dist2(x2, y2, centerX, centerY) <= maxDist2;
    }

    private static double dist2(final double x1, final double y1, final double x2, final double y2) {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }
    
    @Override
    public boolean isPointInside(final double x, final double y) {
        return dist2(x, y, centerX, centerY) <= (radius * radius);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(centerX);
        sb.append(SEPARATOR);
        sb.append(centerY);
        sb.append(SEPARATOR);
        sb.append(radius);
        return sb.toString();
    }    

    public double getCenterX() {
        return centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public double getRadius() {
        return radius;
    }

}
