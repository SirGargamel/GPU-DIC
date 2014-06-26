package cz.tul.dic.data.roi;

import java.io.Serializable;

public class CircularROI extends ROI implements Serializable {

    private final double centerX, centerY, radius;

    public CircularROI(double centerX, double centerY, double radius) {
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
    public boolean isAreaInside(double x1, double y1, double x2, double y2) {
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
    public boolean isPointInside(double x, double y) {
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
