package cz.tul.dic;

import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.task.splitter.TaskSplit;
import cz.tul.dic.engine.EngineUtils;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import cz.tul.dic.generators.facet.FacetGeneratorMode;
import cz.tul.dic.input.InputLoader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Petr Jecmen
 */
public class EngineTest {

    private static final int ROUND = 0;
    private static final int BACKGROUND = -16777216;
    private static final String[] DEF_ZERO_FILES = new String[]{
        "out_0_0", "out_5_0", "out_0_-5", "out_-5_5"};
    private static final double[] DEF_ZERO = new double[]{
        -10, 10, 1, -10, 10, 1};
    private static final double[] DEF_ZERO_F = new double[]{
        -10, 10, 1, -10, 10, 1,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final String[] DEF_FIRST_FILES = new String[]{
        "out_0_0_1_0_0_0", "out_0_0_0_0_0_1", "out_0_0_1_0_0_1"};
    private static final double[] DEF_FIRST = new double[]{
        0, 0, 0, 0, 0, 0,
        -2.0, 2.0, 0.5, -2.0, 2.0, 0.5, -2.0, 2.0, 0.5, -2.0, 2.0, 0.5};
    private static final double[] DEF_FIRST_F = new double[]{
        -0, 3, 1, -0, 3, 1,
        -2.0, 2.0, 0.5, -2.0, 2.0, 0.5, -2.0, 2.0, 0.5, -2.0, 2.0, 0.5};

    @Test
    public void testEngineAll() throws IOException, URISyntaxException, ComputationException {
        TaskContainer tc;
        Set<String> errors = new HashSet<>();
        for (KernelType kt : KernelType.values()) {
            for (Interpolation i : Interpolation.values()) {
                for (TaskSplit ts : TaskSplit.values()) {
                    for (FacetGeneratorMode fgm : FacetGeneratorMode.values()) {
                        for (String s : DEF_ZERO_FILES) {
                            tc = generateTask(s, DEF_ZERO, kt, i, ts, fgm);
                            errors.add(checkResultsBack(tc));
                            tc = generateTask(s, DEF_ZERO_F, kt, i, ts, fgm);
                            errors.add(checkResultsBack(tc));
                        }

                        for (String s : DEF_FIRST_FILES) {
                            tc = generateTask(s, DEF_FIRST, kt, i, ts, fgm);
                            errors.add(checkResultsBack(tc));
                            tc = generateTask(s, DEF_FIRST_F, kt, i, ts, fgm);
                            errors.add(checkResultsBack(tc));
                        }
                    }
                }
            }
        }

        errors.remove(null);
        Assert.assertEquals(errors.toString(), 0, errors.size());
    }

    private TaskContainer generateTask(
            final String outFilename, final double[] deformations,
            final KernelType kernel, final Interpolation interpolation,
            final TaskSplit taskSplit, final FacetGeneratorMode fgm) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/" + outFilename + ".bmp").toURI()).toFile());

        TaskContainer tc = new TaskContainer(input);
        InputLoader.loadInput(tc);

        ROI roi = new RectangleROI(10, 10, 20, 20);

        tc.addRoi(ROUND, roi);
        tc.setDeformationLimits(ROUND, roi, deformations);

        tc.setParameter(TaskParameter.FACET_SIZE, 11);
        tc.setParameter(TaskParameter.FACET_GENERATOR_MODE, FacetGeneratorMode.CLASSIC);
        tc.setParameter(TaskParameter.FACET_GENERATOR_SPACING, 1);
        tc.setParameter(TaskParameter.KERNEL, kernel);
        tc.setParameter(TaskParameter.INTERPOLATION, interpolation);
        tc.setParameter(TaskParameter.TASK_SPLIT_VARIANT, taskSplit);
        tc.setParameter(TaskParameter.FACET_GENERATOR_MODE, fgm);

        EngineUtils.getInstance().computeTask(tc);

        return tc;
    }

    @Test
    public void testEngineDefault() throws IOException, URISyntaxException, ComputationException {
        TaskContainer tc;
        Set<String> errors = new HashSet<>();

        for (String s : DEF_ZERO_FILES) {
            tc = generateTask(s, DEF_ZERO);
            errors.add(checkResultsBack(tc));
            tc = generateTask(s, DEF_ZERO_F);
            errors.add(checkResultsBack(tc));
        }
        for (String s : DEF_FIRST_FILES) {
            tc = generateTask(s, DEF_FIRST);
            errors.add(checkResultsBack(tc));
            tc = generateTask(s, DEF_FIRST_F);
            errors.add(checkResultsBack(tc));
        }

        errors.remove(null);
        Assert.assertEquals(errors.toString(), 0, errors.size());
    }

    private TaskContainer generateTask(final String outFilename, final double[] deformations) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/" + outFilename + ".bmp").toURI()).toFile());

        TaskContainer tc = new TaskContainer(input);
        InputLoader.loadInput(tc);

        ROI roi = new RectangleROI(10, 10, 20, 20);

        tc.addRoi(ROUND, roi);
        tc.setDeformationLimits(ROUND, roi, deformations);

        tc.setParameter(TaskParameter.FACET_SIZE, 11);

        EngineUtils.getInstance().computeTask(tc);

        return tc;
    }

    private String checkResultsBack(final TaskContainer tc) {
        final Image img1 = tc.getImage(ROUND);
        final Image img2 = tc.getImage(ROUND + 1);
        double[][][] results = tc.getDisplacement(ROUND);

        // displacement map
        final Map<Integer, Map<Integer, List<Integer>>> defMap = new HashMap<>();
        final double[] stub = new double[]{0, 0};
        double[] def;
        Map<Integer, List<Integer>> m;
        List<Integer> l;
        int newX, newY;
        int errorCount = 0;
        for (int x = 0; x < results.length; x++) {
            for (int y = 0; y < results[x].length; y++) {
                def = results[x][y];
                if (def != null) {
                    assert (def.length == 2);
                } else {
                    def = stub;
                }
                newX = (int) (x + def[0]);
                m = defMap.get(newX);
                if (m == null) {
                    m = new HashMap<>();
                    defMap.put(newX, m);
                }

                newY = (int) (y + def[1]);
                l = m.get(newY);
                if (l == null) {
                    l = new LinkedList<>();
                    m.put(newY, l);
                }

                l.add(img1.getRGB(x, y));
            }
        }

        // check data
        int color;
        for (int x = 0; x < results.length; x++) {
            for (int y = 0; y < results[x].length; y++) {
                color = img2.getRGB(x, y);

                m = defMap.get(x);
                if (m != null && color != BACKGROUND) {
                    l = m.get(y);
                    if (l != null) {
                        if (!l.contains(color)) {
                            errorCount++;
//                            System.out.println(x + "; " + y);
                        }
                    }
                }
            }
        }

        if (errorCount > 0) {
            final StringBuilder sb = new StringBuilder();
            sb.append("\n");
            sb.append(tc.getParameter(TaskParameter.KERNEL));
            sb.append("; ");
            sb.append(tc.getParameter(TaskParameter.INTERPOLATION));
            sb.append("; ");
            sb.append(Arrays.toString(tc.getDeformationLimits(0).values().iterator().next()));
            return sb.toString();
        } else {
            return null;
        }
    }
}
