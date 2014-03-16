package cz.tul.dic.data.roi;

public class CircularROI extends ROI {

    private final int centerX, centerY, radius;

    public CircularROI(int centerX, int centerY, int radius) {
        super();

        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
    }

    @Override
    public int getX1() {
        return centerX - radius;
    }

    @Override
    public int getY1() {
        return centerY - radius;
    }

    @Override
    public int getX2() {
        return centerX + radius;
    }

    @Override
    public int getY2() {
        return centerY + radius;
    }

    @Override
    public int getWidth() {
        return 2 * radius;
    }

    @Override
    public int getHeight() {
        return 2 * radius;
    }

    @Override
    public boolean isAreaInside(int x1, int y1, int x2, int y2) {
        final int maxDist2 = radius * radius;
        
        return dist2(x1, y1, centerX, centerY) <= maxDist2
                && dist2(x1, y2, centerX, centerY) <= maxDist2
                && dist2(x2, y1, centerX, centerY) <= maxDist2
                && dist2(x2, y2, centerX, centerY) <= maxDist2;
    }

    private static double dist2(final int x1, final int y1, final int x2, final int y2) {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
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

}
