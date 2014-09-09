package cz.tul.dic.output;

import cz.tul.dic.FpsManager;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;

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
    private static boolean debugMode;

    static {
        debugMode = false;
    }

    public static void enableDebugMode() {
        NameGenerator.debugMode = true;
    }

    public static String generateBinary(final TaskContainer tc) {
        return new Generator(tc).name().finalize(EXT_BINARY);
    }

    public static String generateConfig(final TaskContainer tc) {
        return new Generator(tc).name().finalize(EXT_CONFIG);
    }

    public static String generateCsvShifts(final TaskContainer tc) {
        return new Generator(tc).name().finalize(EXT_CSV);
    }

    public static String generateMap(final TaskContainer tc, final int round, final Direction dir) {
        return new Generator(tc).name().time(round).direction(dir).finalize(EXT_MAP);
    }

    public static String generateSequence(final TaskContainer tc, final Direction dir) {
        return new Generator(tc).name().direction(dir).finalize(EXT_SEQUENCE);
    }

    public static String generateCsvPoint(final TaskContainer tc, final int x, final int y) {
        return new Generator(tc).name().intVal(x).intVal(y).finalize(EXT_CSV);
    }

    public static String generateCsvMap(final TaskContainer tc, final int round, final Direction dir) {
        return generateMap(tc, round, dir).replace(EXT_MAP, EXT_CSV);
    }

    private static class Generator {

        private final TaskContainer tc;
        private final StringBuilder sb;
        private final FpsManager fpsM;

        public Generator(TaskContainer tc) {
            this.tc = tc;
            sb = new StringBuilder();
            fpsM = new FpsManager((int) tc.getParameter(TaskParameter.FPS));
        }

        public Generator name() {
            final String inName = tc.getParameter(TaskParameter.IN).toString();
            sb.append(inName.substring(0, inName.lastIndexOf('.')));
            return this;
        }

        public Generator intVal(int round) {
            sb.append(DELIMITER);
            sb.append(round);
            return this;
        }
        
        public Generator time(int imageNr) {
            sb.append(DELIMITER);
            sb.append(fpsM.getTime(imageNr));
            sb.append(fpsM.getTickUnit());
            return this;
        }

        public Generator direction(Direction dir) {
            sb.append(DELIMITER);
            sb.append(dir.toString());
            return this;
        }

        public String finalize(String extension) {
            sb.append(DELIMITER);
            sb.append(tc.getParameter(TaskParameter.FACET_SIZE).toString());
            if (debugMode) {
                sb.append(DELIMITER);
                sb.append(tc.getParameter(TaskParameter.FACET_GENERATOR_PARAM).toString());
                sb.append(DELIMITER);
                sb.append(tc.getParameter(TaskParameter.STRAIN_ESTIMATION_PARAM).toString());
            }
            sb.append(extension);
            return sb.toString();
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }

}
