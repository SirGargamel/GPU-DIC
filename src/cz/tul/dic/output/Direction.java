package cz.tul.dic.output;

/**
 *
 * @author Petr Jecmen
 */
public enum Direction {

    Dx("mm"),
    Dy("mm"),
    Dabs("mm"),
    Exx("%"),
    Eyy("%"),
    Exy("%"),
    Eabs("%"),
    dDx("mm"),
    dDy("mm"),
    dDabs("mm"),
    ;

    private final String unit;

    private Direction(String unit) {
        this.unit = unit;
    }

    public String getDescription() {
        return toString().concat(" [").concat(unit).concat("]");
    }

}
