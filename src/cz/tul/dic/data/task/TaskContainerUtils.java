package cz.tul.dic.data.task;

import cz.tul.dic.data.Config;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.splitter.TaskSplit;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.generators.facet.FacetGeneratorMode;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
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

/**
 *
 * @author Petr Jecmen
 */
public class TaskContainerUtils {

    private static final String CONFIG_INPUT = "INPUT";
    private static final String CONFIG_SEPARATOR_DATA = ",,";
    private static final String CONFIG_SEPARATOR_FULL = ";;";
    private static final String CONFIG_SEPARATOR_PAIRS = "--";
    private static final String CONFIG_PARAMETERS = "PARAM_";
    private static final String CONFIG_ROIS = "ROI_";

    public static int getRoundCount(final TaskContainer tc) {
        int counter = 0;
        for (Image img : tc.getImages()) {
            if (img.isEnabled()) {
                counter++;
            }
        }
        return Math.max(counter - 1, 0);
    }

    public static int getDeformationCount(final TaskContainer tc, final int round, final ROI roi) {
        final int deformationArrayLength = getDeformationArrayLength(tc, round, roi);
        final int result = tc.getDeformations(round, roi).length / deformationArrayLength;

        return result;
    }

    public static int getDeformationArrayLength(final TaskContainer tc, final int round, final ROI roi) {
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
                throw new IllegalArgumentException("Illegal deformation parameters set.");
        }

        return result;
    }

    public static double[] extractDeformation(final TaskContainer tc, final int index, final int round, final ROI roi) {
        if (index < 0) {
            throw new IllegalArgumentException("Negative index not allowed.");
        }

        final int deformationArrayLength = getDeformationArrayLength(tc, round, roi);
        final double[] result = new double[deformationArrayLength];
        System.arraycopy(tc.getDeformations(round, roi), deformationArrayLength * index, result, 0, deformationArrayLength);

        return result;
    }

    public static Config serializeTaskContainer(final TaskContainer tc) {
        final Config result = new Config();
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
                sb.append(CONFIG_SEPARATOR_FULL);
            }
            sb.setLength(sb.length() - CONFIG_SEPARATOR_FULL.length());
            result.put(CONFIG_INPUT, sb.toString());
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
                    sb.append(CONFIG_SEPARATOR_PAIRS);
                    sb.append(toString(tc.getDeformationLimits(round, roi)));
                    sb.append(CONFIG_SEPARATOR_PAIRS);
                    sb.append(Integer.toString(tc.getFacetSize(round, roi)));
                    sb.append(CONFIG_SEPARATOR_FULL);
                }
                sb.setLength(sb.length() - CONFIG_SEPARATOR_FULL.length());

                result.put(CONFIG_ROIS.concat(Integer.toString(round)), sb.toString());
                prevRoi = rois;
            }
        }
        // parameters
        Object val;
        for (TaskParameter tp : TaskParameter.values()) {
            val = tc.getParameter(tp);
            if (val != null) {
                result.put(CONFIG_PARAMETERS.concat(tp.name()), val.toString());
            }
        }

        return result;
    }

    private static String toString(final double[] data) {
        final StringBuilder sb = new StringBuilder();

        for (double d : data) {
            sb.append(d);
            sb.append(CONFIG_SEPARATOR_DATA);
        }
        sb.setLength(sb.length() - CONFIG_SEPARATOR_DATA.length());

        return sb.toString();
    }

    public static TaskContainer deserializeTaskContainer(final Config config) {
        final TaskContainer result;
        // input
        final String input = config.get(CONFIG_INPUT);
        if (input.contains(CONFIG_SEPARATOR_FULL)) {
            // list of images
            final String[] split = input.split(CONFIG_SEPARATOR_FULL);
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
                index = Integer.valueOf(key.replaceFirst(CONFIG_ROIS, ""));
                final String[] splitPairs = e.getValue().split(CONFIG_SEPARATOR_FULL);
                for (String s : splitPairs) {
                    split = s.split(CONFIG_SEPARATOR_PAIRS);
                    if (split.length == 3) {
                        roi = ROI.generateROI(split[0]);
                        result.addRoi(roi, index);
                        result.setDeformationLimits(doubleArrayFromString(split[1]), index, roi);
                        result.addFacetSize(index, roi, Integer.decode(split[2]));
                    } else {
                        throw new IllegalArgumentException("Illegal roi-limits pair - " + Arrays.toString(split));
                    }
                }
            } else if (key.startsWith(CONFIG_PARAMETERS)) {
                tp = TaskParameter.valueOf(key.replaceFirst(CONFIG_PARAMETERS, ""));
                switch (tp) {
                    case DIR:
                        result.setParameter(tp, new File(e.getValue()));
                        break;
                    case FACET_GENERATOR_MODE:
                        result.setParameter(tp, FacetGeneratorMode.valueOf(e.getValue()));
                        break;
                    case FACET_GENERATOR_SPACING:
                        result.setParameter(tp, Integer.valueOf(e.getValue()));
                        break;
                    case KERNEL:
                        result.setParameter(tp, KernelType.valueOf(e.getValue()));
                        break;
                    case TASK_SPLIT_VARIANT:
                        result.setParameter(tp, TaskSplit.valueOf(e.getValue()));
                        break;
                    case TASK_SPLIT_VALUE:
                        result.setParameter(tp, Integer.valueOf(e.getValue()));
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported task parameter - " + tp);
                }
            }
        }

        return result;
    }

    private static double[] doubleArrayFromString(final String data) {
        final String[] split = data.split(CONFIG_SEPARATOR_DATA);
        final double[] result = new double[split.length];
        for (int i = 0; i < split.length; i++) {
            result[i] = Double.valueOf(split[i]);
        }
        return result;
    }

    public static Set<Facet> getAllFacets(final TaskContainer tc, final int round) {
        final Set<Facet> result = new HashSet<>();

        List<Facet> tmp;
        for (ROI roi : tc.getRois(round)) {
            tmp = tc.getFacets(round, roi);
            if (tmp != null) {
                result.addAll(tmp);
            }
        }

        return result;
    }

    public static void setUniformFacetSize(final TaskContainer tc, final int round, final int facetSize) {
        for (ROI roi : tc.getRois(round)) {
            tc.addFacetSize(round, roi, facetSize);
        }
    }

    public static TaskContainer readTaskFromFile(final File in) throws IOException, ClassNotFoundException {
        TaskContainer result;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(in))) {
            result = (TaskContainer) ois.readObject();
        }
        return result;
    }
    public static void dumpTaskToFile(final File target, final TaskContainer data) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(target))) {
            out.writeObject(data);
        }
    }

}
