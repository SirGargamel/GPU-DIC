/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.output.color;

/**
 *
 * @author Petr Ječmen
 */
public class GeneralColorMap extends ColorMap {

    private final double min, max, dif;

    public GeneralColorMap(final Type colorMap, final double min, final double max) {
        super(colorMap);

        this.max = max;
        this.min = min;
        dif = max - min;
    }

    @Override
    protected double convertVal(double value) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("Value must be between 0 and 1 (both inclusive)");
        }

        return (value - min) / dif;
    }

}
