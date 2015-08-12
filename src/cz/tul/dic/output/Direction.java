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

    DX("mm", true, true),
    DY("mm", true, true),
    DABS("mm", true, true),    
    EXX("%", true, false),
    EYY("%", true, false),
    EXY("%", true, false),
    EABS("%", true, false),    
    D_DX("mm", false, true),
    D_DY("mm", false, true),
    D_DABS("mm", false, true),    
    D_EXX("%", false, true),
    D_EYY("%", false, true),
    D_EXY("%", false, false),
    D_EABS("%", false, false),
    Q_D("", true, false),
    Q_EX("%", true, false),
    Q_EY("%", true, false),
    Q_D_D("", false, false),
    Q_D_EX("", false, false),
    Q_D_EY("", false, false),
    R_DX("mm/ms", false, true),
    R_DY("mm/ms", false, true),
    R_DABS("mm/ms", false, true),
    ;

    private final String unit;
    private final boolean stretch, mm;

    private Direction(final String unit, final boolean stretch, final boolean mm) {
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
