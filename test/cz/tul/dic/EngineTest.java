package cz.tul.dic;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.engine.CorrelationCalculator;
import cz.tul.dic.engine.CumulativeResultsCounter;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.engine.displacement.DisplacementCalculator;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import cz.tul.dic.generators.facet.FacetGenerator;
import cz.tul.dic.generators.facet.FacetGeneratorMethod;
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
        "out_0_0_1_0_0_0", "out_0_0_0_1_0_0", "out_0_0_0_0_1_0",
        "out_0_0_0_0_0_1", "out_0_0_1_0_0_1", "out_0_0_1_1_0_0",
        "out_0_0_0_0_1_1"};
    private static final double[] DEF_FIRST = new double[]{
        0, 0, 0, 0, 0, 0,
        -2.0, 2.0, 0.5, -2.0, 2.0, 0.5, -2.0, 2.0, 0.5, -2.0, 2.0, 0.5};
    private static final double[] DEF_FIRST_F = new double[]{
        -2, 2, 1, -2, 2, 1,
        -2.0, 2.0, 0.5, -2.0, 2.0, 0.5, -2.0, 2.0, 0.5, -2.0, 2.0, 0.5};
    private static final String[] DEF_SECOND_FILES = new String[]{
        "out_0_0_0_0_0_0_0_0.5_0_0_0_0", "out_0_0_0_0_0_0_0_0_1_0_0_0",
        "out_0_0_0_0_0_0_0_0_0_0.5_0_0", "out_0_0_0_0_0_0_0_0_0_0_0_1",
        "out_0_0_0_0_0_0_0_0_1_0_0_1", "out_0_0_0_0_0_0_0_0.5_0_0.5_0_0"
    };
    private static final double[] DEF_SECOND = new double[]{
        0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        -0.0, 0.0, 0.0, -1.0, 1.0, 0.5, -1.0, 1.0, 0.5, -1.0, 1.0, 0.5, -0.0, 0.0, 0.0, -1.0, 1.0, 0.5};
    private static final double[] DEF_SECOND_F = new double[]{
        -1, 2, 1, -1, 1, 1,
        -1.0, 1.0, 1.0, -1.0, 1.0, 1.0, -1.0, 1.0, 1.0, -1.0, 1.0, 1.0,
        -0.0, 0.0, 0.0, -1.0, 1.0, 0.5, -1.0, 1.0, 0.5, -1.0, 1.0, 0.5, -0.0, 0.0, 0.0, -1.0, 1.0, 0.5};
    private static final String[] DEF_ZERO_FIRST_FILES = new String[]{
        "out_2_0_1_0_0_0", "out_1_-2_0_0_0_1", "out_-2_-1_1_0_0_1"};
    private static final String[] DEF_ZERO_FIRST_SECOND_FILES = new String[]{
        "out_2_0_1_1_0_0_1_0_0_0_0_1", "out_-1_-1_0_0_1_0_0_0.5_1_0_0_0"};
    private static final double[] DEF_LARGE = new double[]{
        -3, 3, 0.25, -3, 3, 0.25,
        -1.0, 1.0, 1.0, -1.0, 1.0, 1.0, -1.0, 1.0, 1.0, -1.0, 1.0, 1.0,
        -1.0, 1.0, 1.0, -1.0, 1.0, 0.5, -1.0, 1.0, 0.5, -1.0, 1.0, 0.5, -1.0, 1.0, 1.0, -1.0, 1.0, 0.5};
    private static final double[] DEF_LARGE_EXTRA = new double[]{
        -3, 3, 0.25, -1, 2, 0.25,
        -1.0, 1.0, 0.5, -1.0, 1.0, 0.5, -1.0, 1.0, 0.5, -1.0, 1.0, 0.5,
        -1.0, 1.0, 0.5, -1.0, 1.0, 0.5, -1.0, 1.0, 0.5, -1.0, 1.0, 0.5, -1.0, 1.0, 1.0, -1.0, 1.0, 0.5};
    private static final double[][][] CUMULATIVE_ZERO = new double[][][]{
        {{0, 0}, {0, 0}, {0, 0}},
        {{0, 0}, {0, 0}, {0, 0}},
        {{0, 0}, {0, 0}, {0, 0}}};
    private static final double[][][] CUMULATIVE_ONE = new double[][][]{
        {{1, 1}, {1, 1}, {1, 1}},
        {{1, 1}, {1, 1}, {1, 1}},
        {{1, 1}, {1, 1}, {1, 1}}};
    private static final double[][][] CUMULATIVE_TWO = new double[][][]{
        {{2, 2}, {2, 2}, {2, 2}},
        {{2, 2}, {2, 2}, {2, 2}},
        {{2, 2}, {2, 2}, {2, 2}}};

    @Test
    public void testEngineAll() throws IOException, URISyntaxException, ComputationException {
        TaskContainer tc;
        Set<String> errors = new HashSet<>();
        for (KernelType kt : KernelType.values()) {
            for (Interpolation i : Interpolation.values()) {
                for (TaskSplitMethod ts : TaskSplitMethod.values()) {
                    for (FacetGeneratorMethod fgm : FacetGeneratorMethod.values()) {
                        for (String s : DEF_ZERO_FILES) {
                            tc = generateTask(s, DEF_ZERO, kt, i, ts, fgm);
                            errors.add(checkResultsBack(tc, s));
                            tc = generateTask(s, DEF_ZERO_F, kt, i, ts, fgm);
                            errors.add(checkResultsBack(tc, s));
                        }

                        for (String s : DEF_FIRST_FILES) {
                            tc = generateTask(s, DEF_FIRST, kt, i, ts, fgm);
                            errors.add(checkResultsBack(tc, s));
                            tc = generateTask(s, DEF_FIRST_F, kt, i, ts, fgm);
                            errors.add(checkResultsBack(tc, s));
                        }

                        for (String s : DEF_ZERO_FIRST_FILES) {
                            tc = generateTask(s, DEF_FIRST_F, kt, i, ts, fgm);
                            errors.add(checkResultsBack(tc, s));
                        }

                        for (String s : DEF_SECOND_FILES) {
                            tc = generateTask(s, DEF_SECOND, kt, i, ts, fgm);
                            errors.add(checkResultsBack(tc, s));
                            tc = generateTask(s, DEF_SECOND_F, kt, i, ts, fgm);
                            errors.add(checkResultsBack(tc, s));
                        }

                        for (String s : DEF_ZERO_FIRST_SECOND_FILES) {
                            tc = generateTask(s, DEF_SECOND_F, kt, i, ts, fgm);
                            errors.add(checkResultsBack(tc, s));
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
            final TaskSplitMethod taskSplit, final FacetGeneratorMethod fgm) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/" + outFilename + ".bmp").toURI()).toFile());

        final TaskContainer tc = new TaskContainer(input);
        InputLoader.loadInput(tc);

        final ROI roi = new RectangleROI(85, 85, 95, 95);

        tc.addRoi(ROUND, roi);
        tc.setDeformationLimits(ROUND, roi, deformations);

        tc.setParameter(TaskParameter.FACET_SIZE, 11);
        tc.setParameter(TaskParameter.FACET_GENERATOR_METHOD, FacetGeneratorMethod.CLASSIC);
        tc.setParameter(TaskParameter.FACET_GENERATOR_PARAM, 1);
        tc.setParameter(TaskParameter.KERNEL, kernel);
        tc.setParameter(TaskParameter.INTERPOLATION, interpolation);
        tc.setParameter(TaskParameter.TASK_SPLIT_METHOD, taskSplit);
        tc.setParameter(TaskParameter.FACET_GENERATOR_METHOD, fgm);

        Engine.getInstance().computeTask(tc);

        return tc;
    }

    @Test
    public void testEngineDefault() throws IOException, URISyntaxException, ComputationException {
        TaskContainer tc;
        Set<String> errors = new HashSet<>();

        for (String s : DEF_ZERO_FILES) {
            tc = generateTask(s, DEF_ZERO);
            errors.add(checkResultsBack(tc, s));
            tc = generateTask(s, DEF_ZERO_F);
            errors.add(checkResultsBack(tc, s));
        }
        for (String s : DEF_FIRST_FILES) {
            tc = generateTask(s, DEF_FIRST);
            errors.add(checkResultsBack(tc, s));
            tc = generateTask(s, DEF_FIRST_F);
            errors.add(checkResultsBack(tc, s));
        }

        errors.remove(null);
        Assert.assertEquals(errors.toString(), 0, errors.size());
    }

    @Test
    public void testEngineLarge() throws IOException, URISyntaxException, ComputationException {
        TaskContainer tc;
        Set<String> errors = new HashSet<>();

        for (String s : DEF_ZERO_FIRST_SECOND_FILES) {
            tc = generateTask(s, DEF_LARGE);
            errors.add(checkResultsBack(tc, s));
        }

        errors.remove(null);
        Assert.assertEquals(errors.toString(), 0, errors.size());
    }

    @Test
    public void testEngineExtraLarge() throws IOException, URISyntaxException, ComputationException {
        final String s = DEF_ZERO_FIRST_SECOND_FILES[0];
        Assert.assertNull(checkResultsBack(generateTask(s, DEF_LARGE_EXTRA), s));
    }

    private TaskContainer generateTask(final String outFilename, final double[] deformations) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/" + outFilename + ".bmp").toURI()).toFile());

        final TaskContainer tc = new TaskContainer(input);
        InputLoader.loadInput(tc);

        final ROI roi = new RectangleROI(85, 85, 95, 95);

        tc.addRoi(ROUND, roi);
        tc.setDeformationLimits(ROUND, roi, deformations);

        tc.setParameter(TaskParameter.FACET_SIZE, 11);

        Engine.getInstance().computeTask(tc);

        return tc;
    }

    private String checkResultsBack(final TaskContainer tc, final String fileName) {
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
            sb.append(fileName);
            sb.append("; ");
            sb.append(tc.getParameter(TaskParameter.KERNEL));
            sb.append("; ");
            sb.append(tc.getParameter(TaskParameter.INTERPOLATION));
            sb.append("; ");
            final Map<ROI, double[]> limits = tc.getDeformationLimits(0);
            if (limits != null && limits.values().iterator().hasNext()) {
                sb.append(Arrays.toString(limits.values().iterator().next()));
            } else {
                sb.append("No limits !!!");
            }
            sb.append(" - ");
            sb.append(errorCount);
            return sb.toString();
        } else {
            return null;
        }
    }

    @Test
    public void testCumulativeResultCounter() throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(4);
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        final TaskContainer tc = new TaskContainer(input);
        InputLoader.loadInput(tc);
        TaskContainerUtils.checkTaskValidity(tc);

        tc.setDisplacement(0, CUMULATIVE_ZERO);
        tc.setDisplacement(1, CUMULATIVE_ZERO);
        tc.setDisplacement(2, CUMULATIVE_ONE);
        tc.setDisplacement(3, CUMULATIVE_ONE);

        tc.setCumulativeDisplacements(CumulativeResultsCounter.calculate(tc, tc.getDisplacements()));

        assert equals(tc.getCumulativeDisplacement(0), CUMULATIVE_ZERO);
        assert equals(tc.getCumulativeDisplacement(1), CUMULATIVE_ZERO);
        assert equals(tc.getCumulativeDisplacement(2), CUMULATIVE_ONE);
        assert equals(tc.getCumulativeDisplacement(3), CUMULATIVE_TWO);
    }

    private boolean equals(final double[][][] A, final double[][][] B) {
        boolean result = true;

        if (A != null && B != null) {
            loop:
            for (int x = 0; x < A.length; x++) {
                for (int y = 0; y < A[x].length; y++) {
                    for (int z = 0; z < A[x][y].length; z++) {
                        if (A[x][y][z] != B[x][y][z]) {
                            result = false;
                            break loop;
                        }
                    }
                }
            }
        } else {
            result = false;
        }

        return result;
    }

    @Test
    public void testMultiFacet() throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/" + DEF_ZERO_FIRST_SECOND_FILES[0] + ".bmp").toURI()).toFile());

        final TaskContainer tc = new TaskContainer(input);
        InputLoader.loadInput(tc);

        final ROI roi = new RectangleROI(85, 85, 95, 95);
        final int fs = 11;

        tc.addRoi(ROUND, roi);
        tc.setDeformationLimits(ROUND, roi, DEF_SECOND_F);
        tc.setParameter(TaskParameter.FACET_SIZE, fs);

        TaskContainerUtils.checkTaskValidity(tc);

        final CorrelationCalculator correlation = new CorrelationCalculator();
        correlation.setKernel((KernelType) tc.getParameter(TaskParameter.KERNEL));
        correlation.setInterpolation((Interpolation) tc.getParameter(TaskParameter.INTERPOLATION));
        final TaskSplitMethod taskSplit = (TaskSplitMethod) tc.getParameter(TaskParameter.TASK_SPLIT_METHOD);
        correlation.setTaskSplitVariant(taskSplit);
        final Object taskSplitValue = new Object[]{
            TaskContainerUtils.getDeformationArrayLength(tc, ROUND, roi),
            tc.getFacetSize(ROUND, roi)};

        Map<ROI, List<Facet>> facets = new HashMap<>(1);
        final List<Facet> roiFacets = new ArrayList<>(4);
        roiFacets.add(Facet.createFacet(11, roi.getX1(), roi.getY1()));
        roiFacets.add(Facet.createFacet(11, roi.getX1(), roi.getY1()));
        roiFacets.add(Facet.createFacet(11, roi.getX1(), roi.getY1()));
        roiFacets.add(Facet.createFacet(11, roi.getX1(), roi.getY1()));
        facets.put(roi, roiFacets);

        tc.setResult(
                ROUND,
                roi,
                correlation.computeCorrelations(
                        tc.getImage(ROUND), tc.getImage(ROUND + 1),
                        roi, roiFacets,
                        tc.getDeformationLimits(ROUND, roi),
                        DeformationUtils.getDegreeFromLimits(tc.getDeformationLimits(ROUND, roi)),
                        TaskContainerUtils.getDeformationArrayLength(tc, ROUND, roi),
                        tc.getFacetSize(ROUND, roi), taskSplitValue));

        DisplacementCalculator.computeDisplacement(tc, ROUND, facets);

        Assert.assertNull(checkResultsBack(tc, DEF_ZERO_FIRST_SECOND_FILES[0]));
    }
}
