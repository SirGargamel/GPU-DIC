package cz.tul.dic.output;

/**
 *
 * @author Petr Jecmen
 */
public enum Direction {

    cDx("mm"),
    cDy("mm"),
    cDabs("mm"),
    cExx("%"),
    cEyy("%"),
    cExy("%"),
    cEabs("%"),
    Dx("mm"),
    Dy("mm"),
    Dabs("mm"),
    Exx("%"),
    Eyy("%"),
    Exy("%"),
    Eabs("%");

    private final String unit;

    private Direction(String unit) {
        this.unit = unit;
    }

    public String getDescription() {
        return toString().concat(" [").concat(unit).concat("]");
    }

}
