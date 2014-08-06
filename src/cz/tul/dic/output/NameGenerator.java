package cz.tul.dic.output;

/**
 *
 * @author Petr Ječmen
 */
public class NameGenerator {

    private static final String DELIMITER = "_";
    public static final String EXT_CONFIG = ".config";
    public static final String EXT_CSV = ".csv";
    public static final String EXT_MAP = ".bmp";
    public static final String EXT_SEQUENCE = ".avi";
    public static final String EXT_BINARY = ".task";

    public static String generateBinary(final String name) {
        return name.concat(EXT_BINARY);
    }

    public static String generateConfig(final String name) {
        return name.concat(EXT_CONFIG);
    }
    
    public static String generateCsvShifts(final String name) {
        return name.concat(EXT_CSV);
    }

    public static String generateMap(final String name, final int round, final Direction dir) {
        return name.concat(DELIMITER).concat(Integer.toString(round)).concat(DELIMITER).concat(dir.toString()).concat(EXT_MAP);
    }

    public static String generateSequence(final String name, final Direction dir) {
        return name.concat(DELIMITER).concat(dir.toString()).concat(EXT_SEQUENCE);
    }

    public static String generateCsvPoint(final String name, final int x, final int y) {
        return name.concat(DELIMITER).concat(Integer.toString(x)).concat(DELIMITER).concat(Integer.toString(y)).concat(EXT_CSV);
    }

    public static String generateCsvMap(final String name, final int round, final Direction dir) {
        return generateMap(name, round, dir).replace(EXT_MAP, EXT_CSV);
    }

}
