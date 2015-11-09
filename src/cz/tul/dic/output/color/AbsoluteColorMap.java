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
public class AbsoluteColorMap extends ColorMap {

    private final double max;

    public AbsoluteColorMap(final Type colorMap, final double max) {
        super(colorMap);

        this.max = max;
    }

    @Override
    protected double convertVal(double value) {
        if (value < 0 || value > max) {
            throw new IllegalArgumentException("Value must be between 0 and 1 (both inclusive)");
        }

        return ((value / max) / 2) + 0.5;
    }

}
