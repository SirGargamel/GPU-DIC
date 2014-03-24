package cz.tul.dic.data.roi;

/**
 *
 * @author Petr Jecmen
 */
public abstract class ROI {

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

    public abstract boolean isAreaInside(int x1, int y1, int x2, int y2);
    
    public abstract boolean isPointInside(int x, int y);
    
    @Override
    public abstract String toString();

}
