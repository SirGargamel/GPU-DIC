package cz.tul.dic.data.roi;

/**
 *
 * @author Petr Jecmen
 */
public class ROI {

    private static final String SEPARATOR = ";";
    private final int x1, y1, x2, y2;

    public static ROI generateROI(final String data) {
        final String[] split = data.split(SEPARATOR);
        if (split.length != 4) {
            throw  new IllegalArgumentException("4 values required for ROI - " + data);
        }
        return new ROI(Integer.valueOf(split[0]), Integer.valueOf(split[1]), Integer.valueOf(split[2]), Integer.valueOf(split[3]));
    }
    
    public ROI(int x1, int y1, int x2, int y2) {
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
    }        

    public int getX1() {
        return x1;
    }

    public int getY1() {
        return y1;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }

    public int getWidth() {
        return x2 - x1;
    }

    public int getHeight() {
        return y2 - y1;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(x1);
        sb.append(SEPARATOR);
        sb.append(y1);
        sb.append(SEPARATOR);
        sb.append(x2);
        sb.append(SEPARATOR);
        sb.append(y2);
        return sb.toString();
    }

}
