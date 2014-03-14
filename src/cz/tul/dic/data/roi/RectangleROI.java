package cz.tul.dic.data.roi;

public class RectangleROI extends ROI {

    private final int x1, y1, x2, y2;

    public RectangleROI(int x1, int y1, int x2, int y2) {
        super();

        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
    }

    @Override
    public int getX1() {
        return x1;
    }

    @Override
    public int getY1() {
        return y1;
    }

    @Override
    public int getX2() {
        return x2;
    }

    @Override
    public int getY2() {
        return y2;
    }

    @Override
    public int getWidth() {
        return x2 - x1;
    }

    @Override
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

    @Override
    public boolean isAreaInside(int x1, int y1, int x2, int y2) {
        return (Math.min(x1, x2) >= this.x1)
                && (Math.min(y1, y2) >= this.y1)
                && (Math.max(x1, x2) <= this.x2)
                && (Math.max(y1, y2) <= this.y2);
    }

}
