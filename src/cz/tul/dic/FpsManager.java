package cz.tul.dic;

/**
 *
 * @author Petr Ječmen
 */
public class FpsManager {

    private final double tickLength;
    private final String tickUnit;

    public FpsManager(int fps) {
        double length = 1 / (double) fps;
        if (fps > 999) {
            length *= 1000;
            tickUnit = "us";
        } else {
            tickUnit = "ms";
        }
        tickLength = length;

    }

    public double getTickLength() {
        return tickLength;
    }

    public String getTickUnit() {
        return tickUnit;
    }

}
