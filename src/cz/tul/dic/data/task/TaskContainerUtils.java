package cz.tul.dic.data.task;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Config;
import cz.tul.dic.data.ConfigType;
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.engine.ResultCompilation;
import cz.tul.dic.engine.opencl.kernels.KernelType;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import cz.tul.dic.engine.opencl.solvers.Solver;
import cz.tul.dic.generators.facet.FacetGeneratorMethod;
import cz.tul.dic.output.ExportTask;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class TaskContainerUtils {

    private static final String CONFIG_EMPTY = "NONE";
    private static final String CONFIG_EXPORTS = "EXPORT_";
    private static final String CONFIG_INPUT = "INPUT";
    private static final String CONFIG_SEPARATOR = ";;";
    private static final String CONFIG_SEPARATOR_ARRAY = ";";
    private static final String CONFIG_SEPARATOR_ROI = "--";
    private static final String CONFIG_PARAMETERS = "PARAM_";
    private static final String CONFIG_ROIS = "ROI_";

    public static Map<Integer, Integer> getRounds(final TaskContainer tc) {
        final Map<Integer, Integer> result = new TreeMap<>();
        if (tc != null) {
            final Object roundData = tc.getParameter(TaskParameter.ROUND_LIMITS);
            if (roundData == null) {
                for (int r = 0; r < tc.getImages().size() - 1; r++) {
                    result.put(r, r + 1);
                }
            } else {
                final int[] rounds = (int[]) roundData;
                for (int round = rounds[0]; round < rounds[1]; round++) {
                    result.put(round, round + 1);
                }
            }
        }
        return result;
    }

    public static int getFirstRound(final TaskContainer tc) {
        return getRounds(tc).keySet().iterator().next();
    }

    public static int getMaxRoundCount(final TaskContainer tc) {
        return tc.getImages().size() - 1;
    }

    public static int getDeformationArrayLength(final TaskContainer tc, final int round, final ROI roi) throws ComputationException {
        int result;

        final double[] limits = tc.getDeformationLimits(round, roi);
        switch (limits.length) {
            case 6:
                result = 2;
                break;
            case 18:
                result = 6;
                break;
            case 36:
                result = 12;
                break;
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Illegal deformation parameters set.");
        }

        return result;
    }

    public static double[] extractDeformation(final TaskContainer tc, final int index, final int round, final ROI roi, final double[] deformations) throws ComputationException {
        if (index < 0) {
            throw new IllegalArgumentException("Negative index not allowed.");
        }

        final int deformationArrayLength = getDeformationArrayLength(tc, round, roi);
        final double[] result = new double[deformationArrayLength];
        System.arraycopy(deformations, deformationArrayLength * index, result, 0, deformationArrayLength);

        return result;
    }

    public static DisplacementResult getDisplacement(final TaskContainer tc, final int startImageIndex, final int endImageIndex) throws ComputationException {
        DisplacementResult result = tc.getDisplacement(startImageIndex, endImageIndex);

        if (result == null) {
            final Image img = tc.getImage(startImageIndex);
            final int width = img.getWidth();
            final int height = img.getHeight();
            final double[][][] resultData = new double[width][height][];

            double posX, posY;
            int iX, iY;
            double[][][] data;
            double[] val;
            int indexFrom, indexTo;
            boolean notNull, inited;
            DisplacementResult dr;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    notNull = false;
                    inited = false;
                    indexFrom = startImageIndex;
                    indexTo = endImageIndex;
                    posX = x;
                    posY = y;
                    iX = x;
                    iY = y;

                    while (indexFrom != endImageIndex) {
                        do {
                            dr = tc.getDisplacement(indexFrom, indexTo);
                            if (dr != null) {
                                data = dr.getDisplacement();
                            } else {
                                data = null;
                            }
                            if (data == null) {
                                indexTo--;
                            }
                        } while (data == null && indexTo >= 0);
                        if (data == null) {
                            break;
                        }

                        val = data[iX][iY];
                        if (val != null) {
                            notNull = true;
                            posX += val[Coordinates.X];
                            posY += val[Coordinates.Y];
                        } else if (!inited) {
                            break;
                        }
                        inited = true;

                        indexFrom = indexTo;
                        indexTo = endImageIndex;

                        iX = (int) Math.round(posX);
                        iY = (int) Math.round(posY);
                        if (posX < 0 || posY < 0 || iX >= data.length || iY >= data[x].length) {
                            break;
                        }
                    }

                    if (notNull) {
                        resultData[x][y] = new double[]{posX - x, posY - y};
                    }
                }
            }

            result = new DisplacementResult(resultData, null);
            tc.setDisplacement(startImageIndex, endImageIndex, result);
        }

        return result;
    }

    public static double getStretchFactor(final TaskContainer tc, final int endImageIndex) {
        final int startImageIndex = getFirstRound(tc);
        double result = 1.0;
        final DisplacementResult resultsC = tc.getDisplacement(startImageIndex, endImageIndex);
        final DisplacementResult dResultsC = tc.getDisplacement(endImageIndex - 1, endImageIndex);
        if (resultsC != null & dResultsC != null) {
            final double[][][] results = resultsC.getDisplacement();
            final double[][][] dResults = dResultsC.getDisplacement();
            if (dResults != null) {
                final int width = dResults.length;
                final int height = dResults[0].length;

                int y2 = 1;
                outerloop:
                for (int y = height - 1; y >= 0; y--) {
                    for (int x = 0; x < width; x++) {
                        if (dResults[x][y] != null) {
                            y2 = y;
                            break outerloop;
                        }
                    }
                }
                int y1 = 1;
                outerloop:
                for (int y = height - 1; y >= 0; y--) {
                    for (int x = 0; x < width; x++) {
                        if (results[x][y] != null) {
                            y1 = y;
                            break outerloop;
                        }
                    }
                }
                result = y2 / (double) y1;
            }
        }

        return result;
    }

    public static void serializeTaskToConfig(final TaskContainer tc, final File out) throws IOException {
        final Config config = new Config();
        final int roundCount = getMaxRoundCount(tc);
        // input
        final Object input = tc.getInput();
        if (input instanceof File) {
            final File f = (File) input;
            config.put(CONFIG_INPUT, f.getAbsolutePath());
        } else if (input instanceof List) {
            @SuppressWarnings("unchecked")
            final List<File> l = (List<File>) input;
            final StringBuilder sb = new StringBuilder();
            for (File f : l) {
                sb.append(f.getAbsolutePath());
                sb.append(CONFIG_SEPARATOR_ARRAY);
            }
            sb.setLength(sb.length() - CONFIG_SEPARATOR_ARRAY.length());
            config.put(CONFIG_INPUT, sb.toString());
        } else {
            throw new IllegalArgumentException("Unsupported type of input.");
        }
        // rois, deformation limits, facetSizes        
        Set<ROI> rois, prevRoi = null;
        Map<ROI, Integer> fs, prevFs = null;
        Map<ROI, double[]> limits, prevLimits = null;
        final StringBuilder sb = new StringBuilder();
        for (int round = 0; round < roundCount; round++) {
            rois = tc.getRois(round);
            fs = tc.getFacetSizes(round);
            limits = tc.getDeformationLimits(round);
            if (rois != prevRoi || fs != prevFs || limits != prevLimits) {
                sb.setLength(0);
                for (ROI roi : rois) {
                    sb.append(roi.toString());
                    sb.append(CONFIG_SEPARATOR_ROI);
                    sb.append(toString(tc.getDeformationLimits(round, roi)));
                    sb.append(CONFIG_SEPARATOR_ROI);
                    sb.append(Integer.toString(tc.getFacetSize(round, roi)));
                    sb.append(CONFIG_SEPARATOR);
                }
                if (sb.length() > CONFIG_SEPARATOR.length()) {
                    sb.setLength(sb.length() - CONFIG_SEPARATOR.length());
                }

                config.put(CONFIG_ROIS.concat(Integer.toString(round)), sb.toString());
                prevRoi = rois;
            }
        }
        // parameters
        Object val;
        for (TaskParameter tp : TaskParameter.values()) {
            val = tc.getParameter(tp);
            if (val instanceof int[]) {
                config.put(CONFIG_PARAMETERS.concat(tp.name()), toString((int[]) val));
            } else if (val instanceof double[]) {
                config.put(CONFIG_PARAMETERS.concat(tp.name()), toString((double[]) val));
            } else if (val != null) {
                config.put(CONFIG_PARAMETERS.concat(tp.name()), val.toString());
            }
        }
        // exports
        int i = 0;
        for (ExportTask et : tc.getExports()) {
            config.put(CONFIG_EXPORTS.concat(Integer.toString(i)), et.toString());
            i++;
        }

        Config.saveConfig(config, ConfigType.TASK, out);

    }

    private static String toString(final double[] data) {
        final StringBuilder sb = new StringBuilder();

        if (data != null) {
            for (double d : data) {
                sb.append(" ");
                sb.append(d);
                sb.append(CONFIG_SEPARATOR_ARRAY);
            }
            sb.setLength(sb.length() - CONFIG_SEPARATOR_ARRAY.length());
        } else {
            sb.append(CONFIG_EMPTY);
        }

        return sb.toString();
    }

    private static String toString(final int[] data) {
        final StringBuilder sb = new StringBuilder();

        if (data != null) {
            for (int d : data) {
                sb.append(" ");
                sb.append(d);
                sb.append(CONFIG_SEPARATOR_ARRAY);
            }
            sb.setLength(sb.length() - CONFIG_SEPARATOR_ARRAY.length());
        } else {
            sb.append(CONFIG_EMPTY);
        }

        return sb.toString();
    }

    public static TaskContainer deserializeTaskFromConfig(final File in) throws IOException, ComputationException {
        final Config config = Config.loadConfig(in);
        if (!Config.determineType(config).equals(ConfigType.TASK)) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_CONFIG, "Not a task config.");
        }

        final TaskContainer result;
        // input
        final String input = config.get(CONFIG_INPUT);
        if (input.contains(CONFIG_SEPARATOR_ARRAY)) {
            // list of images
            final String[] split = input.split(CONFIG_SEPARATOR_ARRAY);
            final List<File> l = new ArrayList<>(split.length);
            for (String s : split) {
                l.add(new File(s));
            }
            result = new TaskContainer(l);
        } else {
            // video file
            result = new TaskContainer(new File(input));
        }
        // rois, exports, parameters
        String key;
        TaskParameter tp;
        int index;
        String[] split;
        ROI roi;
        for (Entry<String, String> e : config.entrySet()) {
            key = e.getKey();
            if (key.startsWith(CONFIG_ROIS)) {
                index = Integer.parseInt(key.replaceFirst(CONFIG_ROIS, ""));
                final String[] splitPairs = e.getValue().split(CONFIG_SEPARATOR);
                for (String s : splitPairs) {
                    split = s.split(CONFIG_SEPARATOR_ROI);
                    if (split.length == 3) {
                        roi = ROI.generateROI(split[0]);
                        result.addRoi(index, roi);
                        if (!split[1].trim().equals(CONFIG_EMPTY)) {
                            result.setDeformationLimits(index, roi, doubleArrayFromString(split[1]));
                        }
                        if (!split[2].trim().equals(CONFIG_EMPTY)) {
                            result.addFacetSize(index, roi, Integer.decode(split[2]));
                        }
                    } else {
                        throw new IllegalArgumentException("Illegal roi-limits pair - " + Arrays.toString(split));
                    }
                }
            } else if (key.startsWith(CONFIG_PARAMETERS)) {
                tp = TaskParameter.valueOf(key.replaceFirst(CONFIG_PARAMETERS, ""));
                switch (tp) {
                    case IN:
                        result.setParameter(tp, new File(e.getValue()));
                        break;
                    case FACET_GENERATOR_METHOD:
                        result.setParameter(tp, FacetGeneratorMethod.valueOf(e.getValue()));
                        break;
                    case FACET_GENERATOR_PARAM:
                        result.setParameter(tp, Integer.valueOf(e.getValue()));
                        break;
                    case DEFORMATION_LIMITS:
                        result.setParameter(tp, doubleArrayFromString(e.getValue()));
                        break;
                    case FACET_SIZE:
                        result.setParameter(tp, Integer.valueOf(e.getValue()));
                        break;
                    case FPS:
                        result.setParameter(tp, Integer.valueOf(e.getValue()));
                        break;
                    case INTERPOLATION:
                        result.setParameter(tp, Interpolation.valueOf(e.getValue()));
                        break;
                    case KERNEL:
                        result.setParameter(tp, KernelType.valueOf(e.getValue()));
                        break;
                    case RESULT_COMPILATION:
                        result.setParameter(tp, ResultCompilation.valueOf(e.getValue()));
                        break;
                    case TASK_SPLIT_METHOD:
                        result.setParameter(tp, TaskSplitMethod.valueOf(e.getValue()));
                        break;
                    case TASK_SPLIT_PARAM:
                        result.setParameter(tp, Integer.valueOf(e.getValue()));
                        break;
                    case ROUND_LIMITS:
                        result.setParameter(tp, intArrayFromString(e.getValue()));
                        break;
                    case SOLVER:
                        result.setParameter(tp, Solver.valueOf(e.getValue()));
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported task parameter - " + tp);
                }
            } else if (key.startsWith(CONFIG_EXPORTS)) {
                result.addExport(ExportTask.generateExportTask(e.getValue()));
            }
        }

        return result;
    }

    private static double[] doubleArrayFromString(final String data) {
        final double[] result;
        if (data.equals(CONFIG_EMPTY)) {
            result = null;
        } else {
            final String[] split = data.split(CONFIG_SEPARATOR_ARRAY);
            result = new double[split.length];
            for (int i = 0; i < split.length; i++) {
                result[i] = Double.valueOf(split[i].trim());
            }
        }
        return result;
    }

    private static int[] intArrayFromString(final String data) {
        final int[] result;
        if (data.equals(CONFIG_EMPTY)) {
            result = null;
        } else {
            final String[] split = data.split(CONFIG_SEPARATOR_ARRAY);
            result = new int[split.length];
            for (int i = 0; i < split.length; i++) {
                result[i] = Integer.parseInt(split[i].trim());
            }
        }
        return result;
    }

    public static Set<Facet> getAllFacets(final Map<ROI, List<Facet>> facets) {
        final Set<Facet> result = new HashSet<>();

        if (facets != null) {
            for (List<Facet> l : facets.values()) {
                if (l != null) {
                    result.addAll(l);
                }
            }
        }

        return result;
    }

    public static void setUniformFacetSize(final TaskContainer tc, final int round, final int facetSize) {
        for (ROI roi : tc.getRois(round)) {
            tc.addFacetSize(round, roi, facetSize);
        }
    }

    public static void serializeTaskToBinary(final TaskContainer tc, final File target) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(target))) {
            out.writeObject(tc);
            out.flush();
            out.reset();
        }
    }

    public static TaskContainer deserializeTaskFromBinary(final File source) throws IOException, ClassNotFoundException {
        TaskContainer result;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(source))) {
            result = (TaskContainer) in.readObject();
        }
        return result;
    }

    public static void checkTaskValidity(final TaskContainer tc) throws ComputationException {
        final Object in = tc.getParameter(TaskParameter.IN);
        if (in == null) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "No input.");
        }
        final Object roundData = tc.getParameter(TaskParameter.ROUND_LIMITS);
        if (roundData == null) {
            tc.setParameter(TaskParameter.ROUND_LIMITS, new int[]{0, tc.getImages().size() - 1});
        } else {
            final int[] limit = (int[]) roundData;
            if ((limit.length != 2) || (limit[0] > limit[1])) {
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Illegal limits for enabled rounds.");
            }
        }
        final int roundCount = TaskContainerUtils.getRounds(tc).size();
        if (roundCount < 1) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "No rounds for computation.");
        }
        final Object fs = tc.getParameter(TaskParameter.FACET_SIZE);
        if (fs == null) {
            Logger.warn("Adding default facetSize.");
            tc.setParameter(TaskParameter.FACET_SIZE, TaskDefaultValues.DEFAULT_FACET_SIZE);
        }
        final Object dl = tc.getParameter(TaskParameter.DEFORMATION_LIMITS);
        if (dl == null) {
            Logger.warn("Adding default deformation limits.");
            tc.setParameter(TaskParameter.DEFORMATION_LIMITS, TaskDefaultValues.DEFAULT_DEFORMATION_LIMITS_FIRST);
        } else {
            final double[] limits = (double[]) dl;
            final double[] newLimits;
            if (limits.length < 6) {
                newLimits = new double[6];
            } else if (limits.length > 6 && limits.length < 18) {
                newLimits = new double[18];
            } else if (limits.length > 18) {
                newLimits = new double[36];
            } else {
                newLimits = new double[limits.length];
            }
            System.arraycopy(limits, 0, newLimits, 0, limits.length);
            tc.setParameter(TaskParameter.DEFORMATION_LIMITS, newLimits);
        }
        Image img;
        Set<ROI> rois;
        for (int round : TaskContainerUtils.getRounds(tc).keySet()) {
            img = tc.getImage(round);
            if (img == null) {
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "NULL image found.");
            }
            rois = tc.getRois(round);
            if (rois == null || rois.isEmpty()) {
                Logger.warn("Adding default ROI.");
                tc.addRoi(round, new RectangleROI(0, 0, img.getWidth() - 1, img.getHeight() - 1));
            }
        }
        final Object ts = tc.getParameter(TaskParameter.TASK_SPLIT_METHOD);
        if (ts == null) {
            Logger.warn("Adding default TaskSplit.");
            tc.setParameter(TaskParameter.TASK_SPLIT_METHOD, TaskDefaultValues.DEFAULT_TASK_SPLIT_METHOD);
        }
        final Object tsp = tc.getParameter(TaskParameter.TASK_SPLIT_PARAM);
        if (tsp == null) {
            Logger.warn("Adding default TaskSplit param.");
            tc.setParameter(TaskParameter.TASK_SPLIT_PARAM, TaskDefaultValues.DEFAULT_TASK_SPLIT_PARAMETER);
        }
        final Object kernel = tc.getParameter(TaskParameter.KERNEL);
        if (kernel == null) {
            Logger.warn("Adding default kernel.");
            tc.setParameter(TaskParameter.KERNEL, TaskDefaultValues.DEFAULT_KERNEL);
        }
        final Object facetGenMode = tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD);
        if (facetGenMode == null) {
            Logger.warn("Adding default facet generator.");
            tc.setParameter(TaskParameter.FACET_GENERATOR_METHOD, TaskDefaultValues.DEFAULT_FACET_GENERATOR);
        }
        final Object facetGenModeParam = tc.getParameter(TaskParameter.FACET_GENERATOR_PARAM);
        if (facetGenModeParam == null) {
            Logger.warn("Adding default facet generator.");
            tc.setParameter(TaskParameter.FACET_GENERATOR_PARAM, TaskDefaultValues.DEFAULT_FACET_SPACING);
        }
        final Object interpolation = tc.getParameter(TaskParameter.INTERPOLATION);
        if (interpolation == null) {
            Logger.warn("Adding default interpolation.");
            tc.setParameter(TaskParameter.INTERPOLATION, TaskDefaultValues.DEFAULT_INTERPOLATION);
        }
        final Object resultCompilation = tc.getParameter(TaskParameter.RESULT_COMPILATION);
        if (resultCompilation == null) {
            Logger.warn("Adding default result compilator.");
            tc.setParameter(TaskParameter.RESULT_COMPILATION, TaskDefaultValues.DEFAULT_RESULT_COMPILATION);
        }
        final Object strainEstimation = tc.getParameter(TaskParameter.STRAIN_ESTIMATION_METHOD);
        if (strainEstimation == null) {
            Logger.warn("Adding default strain estimator.");
            tc.setParameter(TaskParameter.STRAIN_ESTIMATION_METHOD, TaskDefaultValues.DEFAULT_STRAIN_ESTIMATION_METHOD);
        }
        final Object strainEstimationParam = tc.getParameter(TaskParameter.STRAIN_ESTIMATION_PARAM);
        if (strainEstimationParam == null) {
            Logger.warn("Adding default strain estimator.");
            tc.setParameter(TaskParameter.STRAIN_ESTIMATION_PARAM, TaskDefaultValues.DEFAULT_STRAIN_ESTIMATION_PARAMETER);
        }
        final Object displacementCalculator = tc.getParameter(TaskParameter.DISPLACEMENT_CALCULATION_METHOD);
        if (displacementCalculator == null) {
            Logger.warn("Adding default displacement calculator.");
            tc.setParameter(TaskParameter.DISPLACEMENT_CALCULATION_METHOD, TaskDefaultValues.DEFAULT_DISPLACEMENT_CALCULATION_METHOD);
            tc.setParameter(TaskParameter.DISPLACEMENT_CALCULATION_PARAM, TaskDefaultValues.DEFAULT_DISPLACEMENT_CALCULATION_PARAM);
        }
        final Object ratio = tc.getParameter(TaskParameter.MM_TO_PX_RATIO);
        if (ratio == null) {
            Logger.warn("Adding default mmToPx ratio.");
            tc.setParameter(TaskParameter.MM_TO_PX_RATIO, TaskDefaultValues.DEFAULT_MM_TO_PX_RATIO);
        }
        final Object quality = tc.getParameter(TaskParameter.RESULT_QUALITY);
        if (quality == null) {
            Logger.warn("Adding default result quality.");
            tc.setParameter(TaskParameter.RESULT_QUALITY, TaskDefaultValues.DEFAULT_RESULT_QUALITY);
        }
        final Object correlation = tc.getParameter(TaskParameter.SOLVER);
        if (correlation == null) {
            Logger.warn("Adding default solver.");
            tc.setParameter(TaskParameter.SOLVER, TaskDefaultValues.DEFAULT_SOLVER);
        }
    }

}
