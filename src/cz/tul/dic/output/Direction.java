package cz.tul.dic.output;

/**
 *
 * @author Petr Jecmen
 */
public enum Direction {

    Dx("mm", true),
    Dy("mm", true),
    Dabs("mm", true),
    Exx("%", true),
    Eyy("%", true),
    Exy("%", true),
    Eabs("%", true),
    dDx("mm", false),
    dDy("mm", false),
    dDabs("mm", false),
    dExx("%", false),
    dEyy("%", false),
    dExy("%", false),
    dEabs("%", false),
    rDx("mm/ms", false),
    rDy("mm/ms", false),
    rDabs("mm/ms", false),
    ;

    private final String unit;
    private final boolean stretch;

    private Direction(String unit, boolean stretch) {
        this.unit = unit;
        this.stretch = stretch;
    }

    public String getDescription() {
        return toString().concat(" [").concat(unit).concat("]");
    }

    public boolean isStretch() {
        return stretch;
    }

}
