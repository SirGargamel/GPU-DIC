package cz.tul.dic;

import cz.tul.dic.gui.lang.Lang;

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

    public double getTime(final int imageNr) {
        return tickLength * imageNr;
    }

    public String getTickUnit() {
        return tickUnit;
    }

    public String buildTimeDescription() {
        return Lang.getString("Time").concat(" [".concat(getTickUnit()).concat("]"));
    }

}
