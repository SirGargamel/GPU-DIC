package cz.tul.dic.data.task;

import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.splitter.TaskSplit;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.generators.facet.FacetGeneratorMode;
import cz.tul.dic.output.ExportTask;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Petr Jecmen
 */
public class TaskContainerUtils {

    private static final String CONFIG_INPUT = "INPUT";
    private static final String CONFIG_SEPARATOR = ";";
    private static final String CONFIG_SIZE = "SIZE";
    private static final String CONFIG_PARAMETERS = "PARAM_";
    private static final String CONFIG_ROIS = "ROI_";
    private static final String CONFIG_EXPORTS = "EXPORT_";

    public static int getRoundCount(final TaskContainer tc) {
        int counter = 0;
        for (Image img : tc.getImages()) {
            if (img.isEnabled()) {
                counter++;
            }
        }
        return Math.max(counter - 1, 0);
    }

    public static int getDeformationCount(final TaskContainer tc, final int round) {
        final int deformationArrayLength = getDeformationArrayLength(tc);
        final int result = tc.getDeformations(round).length / deformationArrayLength;

        return result;
    }

    public static int getDeformationArrayLength(final TaskContainer tc) {
        int result;

        final DeformationDegree dd = (DeformationDegree) tc.getParameter(TaskParameter.DEFORMATION_DEGREE);
        switch (dd) {
            case ZERO:
                result = 2;
                break;
            case FIRST:
                result = 6;
                break;
            case SECOND:
                result = 12;
                break;
            default:
                throw new IllegalArgumentException("Deformation parameters not set.");
        }

        return result;
    }

    public static double[] extractDeformation(final TaskContainer tc, final int index, final int round) {
        if (index < 0) {
            throw new IllegalArgumentException("Negative index not allowed.");
        }

        final int deformationArrayLength = getDeformationArrayLength(tc);
        final double[] result = new double[deformationArrayLength];
        System.arraycopy(tc.getDeformations(round), deformationArrayLength * index, result, 0, deformationArrayLength);

        return result;
    }

    public static Map<String, String> serializeTaskContainer(final TaskContainer tc) {
        final Map<String, String> result = new HashMap<>();
        final int roundCount = getRoundCount(tc);
        // input
        final Object input = tc.getInput();
        if (input instanceof File) {
            final File f = (File) input;
            result.put(CONFIG_INPUT, f.getAbsolutePath());
        } else if (input instanceof List) {
            final List<File> l = (List<File>) input;
            final StringBuilder sb = new StringBuilder();
            for (File f : l) {
                sb.append(f.getAbsolutePath());
                sb.append(CONFIG_SEPARATOR);
            }
            sb.setLength(sb.length() - CONFIG_SEPARATOR.length());
            result.put(CONFIG_INPUT, sb.toString());
        } else {
            throw new IllegalArgumentException("Unsupported type of input.");
        }
        // facet size
        result.put(CONFIG_SIZE, Integer.toString(tc.getFacetSize()));
        // rois        
        ROI roi, prevRoi = null;
        for (int round = 0; round < roundCount; round++) {
            roi = tc.getRoi(round);
            if (roi != prevRoi) {
                result.put(CONFIG_ROIS.concat(Integer.toString(round)), roi.toString());
                prevRoi = roi;
            }
        }
        // exports
        int i = 0;
        for (ExportTask et : tc.getExportTasks()) {
            result.put(CONFIG_EXPORTS.concat(Integer.toString(i)), et.toString());
            i++;
        }
        // parameters
        Object val;
        for (TaskParameter tp : TaskParameter.values()) {
            val = tc.getParameter(tp);
            if (val != null) {
                if (tp.equals(TaskParameter.DEFORMATION_BOUNDS)) {
                    result.put(CONFIG_PARAMETERS.concat(tp.name()), toString((double[]) val));
                } else {
                    result.put(CONFIG_PARAMETERS.concat(tp.name()), val.toString());
                }
            }
        }

        return result;
    }

    private static String toString(final double[] data) {
        final StringBuilder sb = new StringBuilder();

        for (double d : data) {
            sb.append(d);
            sb.append(CONFIG_SEPARATOR);
        }
        sb.setLength(sb.length() - CONFIG_SEPARATOR.length());

        return sb.toString();
    }

    public static TaskContainer deserializeTaskContainer(final Map<String, String> data) {
        final TaskContainer result;
        // input
        final String input = data.get(CONFIG_INPUT);
        if (input.contains(CONFIG_SEPARATOR)) {
            // list of images
            final String[] split = input.split(CONFIG_SEPARATOR);
            final List<File> l = new ArrayList<>(split.length);
            for (String s : split) {
                l.add(new File(s));
            }
            result = new TaskContainer(l);
        } else {
            // video file
            result = new TaskContainer(new File(input));
        }
        // facet size
        result.setFacetSize(Integer.valueOf(data.get(CONFIG_SIZE)));
        // rois, exports, parameters
        String key;
        TaskParameter tp;
        int index;
        for (Entry<String, String> e : data.entrySet()) {
            key = e.getKey();
            if (key.startsWith(CONFIG_ROIS)) {
                index = Integer.valueOf(key.replaceFirst(CONFIG_ROIS, ""));
                result.addRoi(ROI.generateROI(e.getValue()), index);
            } else if (key.startsWith(CONFIG_EXPORTS)) {
                result.addExportTask(ExportTask.generateExportTask(e.getValue()));
            } else if (key.startsWith(CONFIG_PARAMETERS)) {
                tp = TaskParameter.valueOf(key.replaceFirst(CONFIG_PARAMETERS, ""));
                switch (tp) {
                    case DIR:
                        result.addParameter(tp, new File(e.getValue()));
                        break;
                    case FACET_GENERATOR_MODE:
                        result.addParameter(tp, FacetGeneratorMode.valueOf(e.getValue()));
                        break;
                    case FACET_GENERATOR_SPACING:
                        result.addParameter(tp, Integer.valueOf(e.getValue()));
                        break;
                    case DEFORMATION_DEGREE:
                        result.addParameter(tp, DeformationDegree.valueOf(e.getValue()));
                        break;
                    case DEFORMATION_BOUNDS:
                        result.addParameter(tp, doubleArrayFromString(e.getValue()));
                        break;
                    case KERNEL:
                        result.addParameter(tp, KernelType.valueOf(e.getValue()));
                        break;
                    case TASK_SPLIT_VARIANT:
                        result.addParameter(tp, TaskSplit.valueOf(e.getValue()));
                        break;
                    case TASK_SPLIT_VALUE:
                        result.addParameter(tp, Integer.valueOf(e.getValue()));
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported task parameter - " + tp);
                }
            }
        }

        return result;
    }

    private static double[] doubleArrayFromString(final String data) {
        final String[] split = data.split(CONFIG_SEPARATOR);
        final double[] result = new double[split.length];
        for (int i = 0; i < split.length; i++) {
            result[i] = Double.valueOf(split[i]);
        }
        return result;
    }

}
