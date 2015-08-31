/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.output;

import cz.tul.dic.FpsManager;
import cz.tul.dic.Utils;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.opencl.solvers.Solver;
import java.io.File;

/**
 *
 * @author Petr Ječmen
 */
public final class NameGenerator {

    private static final String DELIMITER = "_";
    public static final String EXT_CONFIG = ".config";
    public static final String EXT_CSV = ".csv";
    public static final String EXT_TXT = ".txt";
    public static final String EXT_MAP = ".bmp";
    public static final String EXT_SEQUENCE = ".avi";
    public static final String EXT_BINARY = ".task";
    private static final String TEXT_GPU_RESULTS = "gpuResults";
    private static final String TEXT_QUALITY_DEFORMATION = "deformationQuality";
    private static final String TEXT_QUALITY_DEFORMATION_BOOL = "deformationQualityBool";
    private static final String TEXT_QUALITY_SUBSET = "correlationQualitySubset";
    private static final String TEXT_QUALITY_POINT = "correlationQualityPoint";
    private static final String TEXT_QUALITY_REGRESSION = "regressionError";
    private static final String TEXT_HISTOGRAM = "histogram";
    private static boolean debugMode;

    static {
        debugMode = false;
    }

    private NameGenerator() {
    }

    public static void enableDebugMode() {
        NameGenerator.debugMode = true;
    }

    public static String generateBinary(final TaskContainer tc) {
        return new Generator(tc).name().finalizeName(EXT_BINARY);
    }

    public static String generateConfig(final TaskContainer tc) {
        return new Generator(tc).name().finalizeName(EXT_CONFIG);
    }

    public static String generateCsvShifts(final TaskContainer tc) {
        return new Generator(tc).name().finalizeName(EXT_CSV);
    }

    public static String generateMap(final TaskContainer tc, final int round, final Direction dir) {
        return new Generator(tc).name().time(round).direction(dir).finalizeName(EXT_MAP);
    }

    public static String generateSequence(final TaskContainer tc, final Direction dir) {
        return new Generator(tc).name().direction(dir).finalizeName(EXT_SEQUENCE);
    }

    public static String generateCsvPoint(final TaskContainer tc, final int x, final int y) {
        return new Generator(tc).name().intVal(x).intVal(y).finalizeName(EXT_CSV);
    }

    public static String generateCsvDoublePoint(final TaskContainer tc, final int x1, final int y1, final int x2, final int y2) {
        return new Generator(tc).name().intVal(x1).intVal(y1).delimiter().intVal(x2).intVal(y2).finalizeName(EXT_CSV);
    }

    public static String generateCsvMap(final TaskContainer tc, final int round, final Direction dir) {
        return generateMap(tc, round, dir).replace(EXT_MAP, EXT_CSV);
    }

    public static String generateQualityMapSubset(final TaskContainer tc, final int round) {
        return new Generator(tc).name(TEXT_QUALITY_SUBSET).time(round).text(TEXT_QUALITY_SUBSET).finalizeName(EXT_MAP);
    }

    public static String generateQualityMapPoint(final TaskContainer tc, final int round) {
        return new Generator(tc).name(TEXT_QUALITY_POINT).time(round).text(TEXT_QUALITY_POINT).finalizeName(EXT_MAP);
    }

    public static String generateDeformationQualityDump(final TaskContainer tc, final int round) {
        return new Generator(tc).name(TEXT_QUALITY_DEFORMATION).time(round).text(TEXT_QUALITY_DEFORMATION).finalizeName(EXT_TXT);
    }

    public static String generateDeformationQualityUsageDump(final TaskContainer tc, final int round) {
        return new Generator(tc).name(TEXT_QUALITY_DEFORMATION).time(round).text(TEXT_QUALITY_DEFORMATION_BOOL).finalizeName(EXT_TXT);
    }

    public static String generateRegressionQualityMap(final TaskContainer tc, final int round, final Direction dir) {
        return new Generator(tc).name(TEXT_QUALITY_REGRESSION).time(round).text(TEXT_QUALITY_REGRESSION).direction(dir).finalizeName(EXT_MAP);
    }

    public static String generate2DValueHistogram(final TaskContainer tc, final int round, final int x, final int y) {
        return new Generator(tc).name(TEXT_HISTOGRAM).time(round).text(TEXT_HISTOGRAM).intVal(x).intVal(y).finalizeName(EXT_CSV);
    }

    public static String generateGpuResultsDump(final TaskContainer tc, final int batch) {
        return new Generator(tc).name(TEXT_GPU_RESULTS).text(TEXT_GPU_RESULTS).intVal(batch).finalizeName(EXT_TXT);
    }

    private static class Generator {

        private final TaskContainer task;
        private final StringBuilder sb;
        private final FpsManager fpsM;

        public Generator(TaskContainer task) {
            this.task = task;
            sb = new StringBuilder();
            fpsM = new FpsManager(task);
        }

        public Generator name() {
            sb.append(task.getParameter(TaskParameter.IN).toString());
            return this;
        }

        public Generator name(final String dirName) {
            final File in = (File) task.getParameter(TaskParameter.IN);
            sb.append(in.getParent());
            sb.append(File.separator);
            sb.append(dirName);
            sb.append(File.separator);
            sb.append(in.getName());
            return this;
        }

        public Generator intVal(final int round) {
            sb.append(DELIMITER);
            sb.append(Utils.format(round));
            return this;
        }

        public Generator time(final int imageNr) {
            sb.append(DELIMITER);
            sb.append(Utils.format(fpsM.getTime(imageNr)));
            sb.append(fpsM.getTickUnit());
            return this;
        }

        public Generator direction(final Direction dir) {
            sb.append(DELIMITER);
            sb.append(dir.toString());
            return this;
        }

        public Generator delimiter() {
            sb.append(DELIMITER);
            return this;
        }

        public Generator text(final String text) {
            sb.append(DELIMITER);
            sb.append(text);
            return this;
        }

        public String finalizeName(final String extension) {
            sb.append(DELIMITER);
            sb.append(DELIMITER);
            sb.append(Utils.format((int) task.getParameter(TaskParameter.SUBSET_SIZE)));
            if (debugMode) {
                sb.append(DELIMITER);
                sb.append(Utils.format((int) task.getParameter(TaskParameter.SUBSET_GENERATOR_PARAM)));
                sb.append(DELIMITER);
                sb.append(Utils.format((double) task.getParameter(TaskParameter.STRAIN_ESTIMATION_PARAM)));
                sb.append(DELIMITER);
                final Solver solver  = (Solver) task.getParameter(TaskParameter.SOLVER);
                sb.append(solver.getAbbreviation());
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
