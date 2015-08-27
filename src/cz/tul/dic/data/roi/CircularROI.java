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
    public double getX1() {
        return centerX - radius;
    }

    @Override
    public double getY1() {
        return centerY - radius;
    }

    @Override
    public double getX2() {
        return centerX + radius;
    }

    @Override
    public double getY2() {
        return centerY + radius;
    }

    @Override
    public double getWidth() {
        return 2 * radius;
    }

    @Override
    public double getHeight() {
        return 2 * radius;
    }

    @Override
    public boolean isAreaInside(final double x1, final double y1, final double x2, final double y2) {
        final double maxDist2 = radius * radius;
        
        return dist2FromCenter(x1, y1) <= maxDist2
                && dist2FromCenter(x1, y2) <= maxDist2
                && dist2FromCenter(x2, y1) <= maxDist2
                && dist2FromCenter(x2, y2) <= maxDist2;
    }

    private double dist2FromCenter(final double x1, final double y1) {
        return (x1 - centerX) * (x1 - centerX) + (y1 - centerY) * (y1 - centerY);
    }
    
    @Override
    public boolean isPointInside(final double x, final double y) {
        return dist2FromCenter(x, y) <= (radius * radius);
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
