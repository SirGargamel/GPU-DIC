package cz.tul.dic.data.roi;

/**
 *
 * @author Petr Jecmen
 */
public abstract class ROI {
    
    static final String SEPARATOR = ";";

    public static ROI generateROI(final String data) {
        final String[] split = data.split(SEPARATOR);
        if (split.length != 4) {
            throw new IllegalArgumentException("4 values required for ROI - " + data);
        }
        return new RectangleROI(Integer.valueOf(split[0]), Integer.valueOf(split[1]), Integer.valueOf(split[2]), Integer.valueOf(split[3]));
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

}
