/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task;

import cz.tul.dic.data.result.DisplacementResult;
import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.config.Config;
import cz.tul.dic.data.config.ConfigType;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.roi.AbstractROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.engine.opencl.WorkSizeManager;
import cz.tul.dic.engine.opencl.kernels.KernelType;
import cz.tul.dic.data.Interpolation;
import cz.tul.dic.engine.opencl.solvers.Solver;
import cz.tul.dic.data.subset.generator.FacetGeneratorMethod;
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
import java.util.Set;
import java.util.TreeMap;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public final class TaskContainerUtils {

    private static final String CONFIG_EMPTY = "NONE";
    private static final String CONFIG_EXPORTS = "EXPORT_";
    private static final String CONFIG_INPUT = "INPUT";
    private static final String CONFIG_SEPARATOR = ";;";
    private static final String CONFIG_SEPARATOR_ARRAY = ";";
    private static final String CONFIG_SEPARATOR_ROI = "--";
    private static final String CONFIG_PARAMETERS = "PARAM_";
    private static final String CONFIG_ROIS = "ROI_";
    
    private TaskContainerUtils() {
    }

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

    public static int getDeformationArrayLength(final TaskContainer tc, final int round, final AbstractROI roi) throws ComputationException {
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

    public static double[] extractDeformation(final TaskContainer tc, final int index, final int round, final AbstractROI roi, final double[] deformations) throws ComputationException {
        if (index < 0) {
            throw new IllegalArgumentException("Negative index not allowed.");
        }

        final int deformationArrayLength = getDeformationArrayLength(tc, round, roi);
        final double[] result = new double[deformationArrayLength];
        System.arraycopy(deformations, deformationArrayLength * index, result, 0, deformationArrayLength);

        return result;
    }

    public static double getStretchFactor(final TaskContainer tc, final int endImageIndex) throws ComputationException {
        final int startImageIndex = getFirstRound(tc);
        double result = 1.0;
        final DisplacementResult resultsC = tc.getResult(startImageIndex, endImageIndex).getDisplacementResult();
        final DisplacementResult dResultsC = tc.getResult(endImageIndex - 1, endImageIndex).getDisplacementResult();
        if (resultsC != null && dResultsC != null) {
            final double[][][] results = resultsC.getDisplacement();
            final double[][][] dResults = dResultsC.getDisplacement();
            if (dResults != null) {
                final int y2 = finalBottomLine(dResults);
                final int y1 = finalBottomLine(results);                
                result = y2 / (double) y1;
            }
        }

        return result;
    }

    private static int finalBottomLine(final double[][][] data) {
        for (int y = data[0].length - 1; y >= 0; y--) {
            for (int x = 0; x < data.length; x++) {
                if (data[x][y] != null) {                    
                    return y;
                }
            }
        }
        return 1;
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
        // rois, deformation limits, subsetSizes        
        Set<AbstractROI> rois, prevRoi = null;
        Map<AbstractROI, Integer> fs, prevFs = null;
        Map<AbstractROI, double[]> limits, prevLimits = null;
        final StringBuilder sb = new StringBuilder();
        for (int round = 0; round < roundCount; round++) {
            rois = tc.getRois(round);
            fs = tc.getSubsetSizes(round);
            limits = tc.getDeformationLimits(round);
            if (rois != prevRoi || fs != prevFs || limits != prevLimits) {
                sb.setLength(0);
                for (AbstractROI roi : rois) {
                    sb.append(roi.toString());
                    sb.append(CONFIG_SEPARATOR_ROI);
                    sb.append(toString(tc.getDeformationLimits(round, roi)));
                    sb.append(CONFIG_SEPARATOR_ROI);
                    sb.append(Integer.toString(tc.getSubsetSize(round, roi)));
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

    public static TaskContainer deserializeTaskFromConfig(final File in) throws ComputationException {
        final Config config;
        try {
            config = Config.loadConfig(in);
        } catch (IOException ex) {
            throw new ComputationException(ComputationExceptionCause.IO, ex);
        }
        if (config == null) {
            throw new IllegalArgumentException("Non-existent config.");
        }
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
        String value;
        TaskParameter tp;
        int index;
        String[] split;
        AbstractROI roi;
        for (String key : config.keySet()) {
            value = config.get(key);
            if (key.startsWith(CONFIG_ROIS)) {
                index = Integer.parseInt(key.replaceFirst(CONFIG_ROIS, ""));
                final String[] splitPairs = value.split(CONFIG_SEPARATOR);
                for (String s : splitPairs) {
                    split = s.split(CONFIG_SEPARATOR_ROI);
                    if (split.length == 3) {
                        roi = AbstractROI.generateROI(split[0]);
                        result.addRoi(index, roi);
                        if (!split[1].trim().equals(CONFIG_EMPTY)) {
                            result.setDeformationLimits(index, roi, doubleArrayFromString(split[1]));
                        }
                        if (!split[2].trim().equals(CONFIG_EMPTY)) {
                            result.addSubsetSize(index, roi, Integer.decode(split[2]));
                        }
                    } else {
                        throw new IllegalArgumentException("Illegal roi-limits pair - " + Arrays.toString(split));
                    }
                }
            } else if (key.startsWith(CONFIG_PARAMETERS)) {
                tp = TaskParameter.valueOf(key.replaceFirst(CONFIG_PARAMETERS, ""));
                switch (tp) {
                    case IN:
                        result.setParameter(tp, new File(value));
                        break;
                    case FACET_GENERATOR_METHOD:
                        result.setParameter(tp, FacetGeneratorMethod.valueOf(value));
                        break;
                    case FACET_GENERATOR_PARAM:
                        result.setParameter(tp, Integer.valueOf(value));
                        break;
                    case DEFORMATION_LIMITS:
                        result.setParameter(tp, doubleArrayFromString(value));
                        break;
                    case DEFORMATION_ORDER:
                        result.setParameter(tp, DeformationDegree.valueOf(value));
                        break;
                    case FACET_SIZE:
                        result.setParameter(tp, Integer.valueOf(value));
                        break;
                    case FPS:
                        result.setParameter(tp, Integer.valueOf(value));
                        break;
                    case INTERPOLATION:
                        result.setParameter(tp, Interpolation.valueOf(value));
                        break;
                    case KERNEL:
                        result.setParameter(tp, KernelType.valueOf(value));
                        break;
                    case TASK_SPLIT_METHOD:
                        result.setParameter(tp, TaskSplitMethod.valueOf(value));
                        break;
                    case TASK_SPLIT_PARAM:
                        result.setParameter(tp, Integer.valueOf(value));
                        break;
                    case ROUND_LIMITS:
                        result.setParameter(tp, intArrayFromString(value));
                        break;
                    case SOLVER:
                        result.setParameter(tp, Solver.valueOf(value));
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported task parameter - " + tp);
                }
            } else if (key.startsWith(CONFIG_EXPORTS)) {
                result.addExport(ExportTask.generateExportTask(value));
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
                result[i] = Double.valueOf(split[i].trim().replace(',', '.'));
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

    public static Set<AbstractSubset> getAllFacets(final Map<AbstractROI, List<AbstractSubset>> subsets) {
        final Set<AbstractSubset> result = new HashSet<>();

        if (subsets != null) {
            for (List<AbstractSubset> l : subsets.values()) {
                if (l != null) {
                    result.addAll(l);
                }
            }
        }

        return result;
    }

    public static void setUniformFacetSize(final TaskContainer tc, final int round, final int subsetSize) {
        for (AbstractROI roi : tc.getRois(round)) {
            tc.addSubsetSize(round, roi, subsetSize);
        }
    }

    public static void serializeTaskToBinary(final TaskContainer tc, final File target) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(target))) {
            out.writeObject(tc);
            out.flush();
            out.reset();
        }
    }

    public static TaskContainer deserializeTaskFromBinary(final File source) throws ComputationException {
        TaskContainer result;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(source))) {
            result = (TaskContainer) in.readObject();        
        } catch (IOException | ClassNotFoundException ex) {
            throw new ComputationException(ComputationExceptionCause.IO, ex);
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
            Logger.warn("Adding default subset size.");
            tc.setParameter(TaskParameter.FACET_SIZE, TaskDefaultValues.DEFAULT_FACET_SIZE);
        }
        final Object dorder = tc.getParameter(TaskParameter.DEFORMATION_ORDER);
        if (dorder == null) {
            Logger.warn("Adding default deformation order.");
            tc.setParameter(TaskParameter.DEFORMATION_ORDER, TaskDefaultValues.DEFAULT_DEFORMATION_DEGREE);
        }
        final Object dl = tc.getParameter(TaskParameter.DEFORMATION_LIMITS);
        if (dl == null) {
            Logger.warn("Adding default deformation limits.");
            if (tc.getParameter(TaskParameter.DEFORMATION_ORDER) == DeformationDegree.ZERO) {
                tc.setParameter(TaskParameter.DEFORMATION_LIMITS, TaskDefaultValues.DEFAULT_DEFORMATION_LIMITS_ZERO);
            } else {
                tc.setParameter(TaskParameter.DEFORMATION_LIMITS, TaskDefaultValues.DEFAULT_DEFORMATION_LIMITS_FIRST);
            }
        } else {
            final DeformationDegree deg = (DeformationDegree) tc.getParameter(TaskParameter.DEFORMATION_ORDER);
            final double[] limits = (double[]) dl;
            final double[] newLimits = new double[DeformationUtils.getDeformationLimitsArrayLength(deg)];
            System.arraycopy(limits, 0, newLimits, 0, Math.min(limits.length, newLimits.length));
            tc.setParameter(TaskParameter.DEFORMATION_LIMITS, newLimits);
        }
        Image img;
        Set<AbstractROI> rois;
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
            tc.setParameter(TaskParameter.KERNEL, WorkSizeManager.getBestKernel());
        }
        final Object subsetGenMode = tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD);
        if (subsetGenMode == null) {
            Logger.warn("Adding default subset generator.");
            tc.setParameter(TaskParameter.FACET_GENERATOR_METHOD, TaskDefaultValues.DEFAULT_FACET_GENERATOR);
        }
        final Object subsetGenModeParam = tc.getParameter(TaskParameter.FACET_GENERATOR_PARAM);
        if (subsetGenModeParam == null) {
            Logger.warn("Adding default subset generator.");
            tc.setParameter(TaskParameter.FACET_GENERATOR_PARAM, TaskDefaultValues.DEFAULT_FACET_SPACING);
        }
        final Object interpolation = tc.getParameter(TaskParameter.INTERPOLATION);
        if (interpolation == null) {
            Logger.warn("Adding default interpolation.");
            tc.setParameter(TaskParameter.INTERPOLATION, TaskDefaultValues.DEFAULT_INTERPOLATION);
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
