/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task.loaders;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Interpolation;
import cz.tul.dic.data.config.Config;
import cz.tul.dic.data.config.ConfigType;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.roi.AbstractROI;
import cz.tul.dic.data.subset.generator.SubsetGeneratorMethod;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.engine.displacement.DisplacementCalculation;
import cz.tul.dic.engine.opencl.kernels.KernelType;
import cz.tul.dic.engine.opencl.solvers.Solver;
import cz.tul.dic.engine.strain.StrainEstimationMethod;
import cz.tul.dic.output.ExportTask;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.pmw.tinylog.Logger;

public class ConfigLoader extends AbstractInputLoader {

    private static final String SUPPORTED_TYPES = "config";
    public static final String CONFIG_EMPTY = "NONE";
    public static final String CONFIG_EXPORTS = "EXPORT_";
    public static final String CONFIG_INPUT = "INPUT";
    public static final String CONFIG_SEPARATOR = ";;";
    public static final String CONFIG_SEPARATOR_ARRAY = ";";
    public static final String CONFIG_SEPARATOR_ROI = "--";
    public static final String CONFIG_PARAMETERS = "PARAM_";
    public static final String CONFIG_ROIS = "ROI_";

    @Override
    public TaskContainer loadTask(final Object in, final TaskContainer task) throws IOException, ComputationException {
        if (!(in instanceof File)) {
            throw new IllegalArgumentException("ImageLoader needs a list of files as input.");
        }

        Config config = new Config();
        try {
            config = config.load((File) in);
        } catch (IOException ex) {
            throw new ComputationException(ComputationExceptionCause.IO, ex);
        }
        if (config == null) {
            throw new IllegalArgumentException("Non-existent config.");
        }
        if (!config.getType().equals(ConfigType.TASK)) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_CONFIG, "Not a task config.");
        }

        // input
        task.setParameter(TaskParameter.IN, in);
        final String input = config.get(CONFIG_INPUT);
        if (input.contains(CONFIG_SEPARATOR_ARRAY)) {
            // list of images
            final String[] split = input.split(CONFIG_SEPARATOR_ARRAY);
            final List<File> l = new ArrayList<>(split.length);
            for (String s : split) {
                l.add(new File(s));
            }
            loadImages(task, l);
        } else {
            // video file
            final VideoLoader vl = new VideoLoader();
            vl.loadTask(new File(input), task);
        }
        // rois, exports, parameters        
        String value;
        TaskParameter tp;
        int index;
        String[] split;
        AbstractROI roi;
        for (String key : config.keySet()) {
            value = config.get(key);
            try {
                if (key.startsWith(CONFIG_ROIS)) {
                    index = Integer.parseInt(key.replaceFirst(CONFIG_ROIS, ""));
                    final String[] splitPairs = value.split(CONFIG_SEPARATOR);
                    for (String s : splitPairs) {
                        split = s.split(CONFIG_SEPARATOR_ROI);
                        if (split.length == 3) {
                            roi = AbstractROI.generateROI(split[0]);
                            task.addRoi(index, roi);
                            if (!split[1].trim().equals(CONFIG_EMPTY)) {
                                task.setDeformationLimits(index, roi, doubleArrayFromString(split[1]));
                            }
                            if (!split[2].trim().equals(CONFIG_EMPTY)) {
                                task.addSubsetSize(index, roi, Integer.decode(split[2]));
                            }
                        } else {
                            throw new IllegalArgumentException("Illegal roi-limits pair - " + Arrays.toString(split));
                        }
                    }
                } else if (key.startsWith(CONFIG_PARAMETERS)) {
                    tp = TaskParameter.valueOf(key.replaceFirst(CONFIG_PARAMETERS, ""));
                    switch (tp) {
                        case IN:
                            task.setParameter(tp, new File(value));
                            break;
                        case FACET_GENERATOR_METHOD:
                            task.setParameter(tp, SubsetGeneratorMethod.valueOf(value));
                            break;
                        case FACET_GENERATOR_PARAM:
                            task.setParameter(tp, Integer.valueOf(value));
                            break;
                        case DEFORMATION_LIMITS:
                            task.setParameter(tp, doubleArrayFromString(value));
                            break;
                        case DEFORMATION_ORDER:
                            task.setParameter(tp, DeformationDegree.valueOf(value));
                            break;
                        case DISPLACEMENT_CALCULATION_METHOD:
                            task.setParameter(tp, DisplacementCalculation.valueOf(value));
                            break;
                        case DISPLACEMENT_CALCULATION_PARAM:
                            task.setParameter(tp, Integer.valueOf(value));
                            break;
                        case FACET_SIZE:
                            task.setParameter(tp, Integer.valueOf(value));
                            break;
                        case FPS:
                            task.setParameter(tp, Integer.valueOf(value));
                            break;
                        case INTERPOLATION:
                            task.setParameter(tp, Interpolation.valueOf(value));
                            break;
                        case KERNEL:
                            task.setParameter(tp, KernelType.valueOf(value));
                            break;
                        case MM_TO_PX_RATIO:
                            task.setParameter(tp, Double.valueOf(value));
                            break;
                        case TASK_SPLIT_METHOD:
                            task.setParameter(tp, TaskSplitMethod.valueOf(value));
                            break;
                        case TASK_SPLIT_PARAM:
                            task.setParameter(tp, Integer.valueOf(value));
                            break;
                        case RESULT_QUALITY:
                            task.setParameter(tp, Double.valueOf(value));
                            break;
                        case ROUND_LIMITS:
                            task.setParameter(tp, intArrayFromString(value));
                            break;
                        case SOLVER:
                            task.setParameter(tp, Solver.valueOf(value));
                            break;
                        case STRAIN_ESTIMATION_METHOD:
                            task.setParameter(tp, StrainEstimationMethod.valueOf(value));
                            break;
                        case STRAIN_ESTIMATION_PARAM:
                            task.setParameter(tp, Double.valueOf(value));
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported task parameter - " + tp);
                    }
                } else if (key.startsWith(CONFIG_EXPORTS)) {
                    task.addExport(ExportTask.generateExportTask(value));
                }
            } catch (IllegalArgumentException ex) {
                Logger.error("Error parsing config file - {0} : {1}", key, value);
                Logger.debug(ex);
            }
        }

        return task;
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

    @Override
    public boolean canLoad(Object in) {
        boolean result = false;
        if (in instanceof File) {
            final File input = (File) in;
            final String ext = input.getName().substring(input.getName().lastIndexOf('.') + 1).toLowerCase(Locale.getDefault());
            result = SUPPORTED_TYPES.contains(ext);
        }
        return result;
    }

}
