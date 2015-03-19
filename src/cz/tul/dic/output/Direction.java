/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.output;

/**
 *
 * @author Petr Jecmen
 */
public enum Direction {

    Dx("mm", true, true),
    Dy("mm", true, true),
    Dabs("mm", true, true),
    Exx("%", true, false),
    Eyy("%", true, false),
    Exy("%", true, false),
    Eabs("%", true, false),
    dDx("mm", false, true),
    dDy("mm", false, true),
    dDabs("mm", false, true),
    dExx("%", false, true),
    dEyy("%", false, true),
    dExy("%", false, false),
    dEabs("%", false, false),
    rDx("mm/ms", false, true),
    rDy("mm/ms", false, true),
    rDabs("mm/ms", false, true),
    ;

    private final String unit;
    private final boolean stretch, mm;

    private Direction(String unit, boolean stretch, boolean mm) {
        this.unit = unit;
        this.stretch = stretch;
        this.mm = mm;
    }

    public String getDescription() {
        return toString().concat(" [").concat(unit).concat("]");
    }

    public boolean isStretch() {
        return stretch;
    }

    public boolean isMm() {
        return mm;
    }

}
