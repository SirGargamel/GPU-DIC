/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.subset.SquareSubset2D;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.roi.AbstractROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.result.DisplacementResult;
import cz.tul.dic.data.task.Hint;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.engine.opencl.solvers.AbstractTaskSolver;
import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.engine.displacement.DisplacementCalculator;
import cz.tul.dic.engine.opencl.kernels.KernelType;
import cz.tul.dic.data.Interpolation;
import cz.tul.dic.engine.opencl.solvers.Solver;
import cz.tul.dic.data.result.Result;
import cz.tul.dic.data.task.FullTask;
import cz.tul.dic.data.subset.generator.FacetGeneratorMethod;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
        -1.0, 1.0, 0.5, -1.0, 1.0, 0.5, -1.0, 1.0, 0.5, -1.0, 1.0, 0.5};
    private static final String[] DEF_ZERO_FIRST_FILES = new String[]{
        "out_2_0_1_0_0_0", "out_1_-2_0_0_0_1", "out_-2_-1_1_0_0_1"};
    private static final double[] DEF_LARGE = new double[]{
        -5, 5, 0.25, -5, 5, 0.2,
        -1.0, 1.0, 0.05, -1.0, 1.0, 0.2, -1.0, 1.0, 0.2, -1.0, 1.0, 0.1};

    @Test
    public void testEngineAll() throws IOException, URISyntaxException, ComputationException {
        TaskContainer tc;
        Set<String> errors = new LinkedHashSet<>();
        int counter = 0;
        final Solver slvr = Solver.BRUTE_FORCE;
        for (Interpolation i : Interpolation.values()) {
            for (TaskSplitMethod ts : TaskSplitMethod.values()) {
                for (FacetGeneratorMethod fgm : FacetGeneratorMethod.values()) {
                    for (KernelType kt : KernelType.values()) {
                        for (String s : DEF_ZERO_FILES) {
                            tc = generateTask(s, DEF_ZERO, kt, i, ts, fgm, slvr);
                            errors.add(computeAndCheckTask(tc, s));
                            counter++;
                            tc = generateTask(s, DEF_ZERO_F, kt, i, ts, fgm, slvr);
                            errors.add(computeAndCheckTask(tc, s));
                            counter++;
                        }

                        for (String s : DEF_FIRST_FILES) {
                            tc = generateTask(s, DEF_FIRST, kt, i, ts, fgm, slvr);
                            errors.add(computeAndCheckTask(tc, s));
                            counter++;
                            tc = generateTask(s, DEF_FIRST_F, kt, i, ts, fgm, slvr);
                            errors.add(computeAndCheckTask(tc, s));
                            counter++;
                        }

                        for (String s : DEF_ZERO_FIRST_FILES) {
                            tc = generateTask(s, DEF_FIRST_F, kt, i, ts, fgm, slvr);
                            errors.add(computeAndCheckTask(tc, s));
                            counter++;
                        }
                    }
                }
            }
        }

        errors.remove(null);
        Assert.assertEquals(errors.toString() + "\nTotal: " + counter + ",", 0, errors.size());
    }

    private TaskContainer generateTask(
            final String outFilename, final double[] deformations,
            final KernelType kernel, final Interpolation interpolation,
            final TaskSplitMethod taskSplit, final FacetGeneratorMethod fgm,
            final Solver solver) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/engine/" + outFilename + ".bmp").toURI()).toFile());

        final TaskContainer tc = TaskContainer.initTaskContainer(input);

        final AbstractROI roi = new RectangleROI(85, 85, 95, 95);

        tc.addRoi(ROUND, roi);
        tc.setDeformationLimits(ROUND, roi, deformations);

        tc.addHint(Hint.NO_STRAIN);
        tc.addHint(Hint.NO_STATS);

        tc.setParameter(TaskParameter.IN, input.get(0));
        tc.setParameter(TaskParameter.FACET_SIZE, 5);
        tc.setParameter(TaskParameter.FACET_GENERATOR_METHOD, FacetGeneratorMethod.EQUAL);
        tc.setParameter(TaskParameter.FACET_GENERATOR_PARAM, 11);
        tc.setParameter(TaskParameter.KERNEL, kernel);
        tc.setParameter(TaskParameter.INTERPOLATION, interpolation);
        tc.setParameter(TaskParameter.TASK_SPLIT_METHOD, taskSplit);
        tc.setParameter(TaskParameter.FACET_GENERATOR_METHOD, fgm);
        tc.setParameter(TaskParameter.SOLVER, solver);

        return tc;
    }

    @Test
    public void testEngineDefault() throws IOException, URISyntaxException, ComputationException {
        TaskContainer tc;
        Set<String> errors = new HashSet<>();

        for (String s : DEF_ZERO_FILES) {
            tc = generateTask(s, DEF_ZERO);
            errors.add(computeAndCheckTask(tc, s));
            tc = generateTask(s, DEF_ZERO_F);
            errors.add(computeAndCheckTask(tc, s));
        }
        for (String s : DEF_FIRST_FILES) {
            tc = generateTask(s, DEF_FIRST);
            errors.add(computeAndCheckTask(tc, s));
            tc = generateTask(s, DEF_FIRST_F);
            errors.add(computeAndCheckTask(tc, s));
        }

        errors.remove(null);
        Assert.assertEquals(errors.toString(), 0, errors.size());
    }

    @Test
    public void testEngineLarge() throws IOException, URISyntaxException, ComputationException {
        TaskContainer tc;
        Set<String> errors = new HashSet<>();

        tc = generateTask(DEF_ZERO_FIRST_FILES[2], DEF_LARGE);
        errors.add(computeAndCheckTask(tc, DEF_ZERO_FIRST_FILES[2]));
        errors.remove(null);
        Assert.assertEquals(errors.toString(), 0, errors.size());
    }

    private TaskContainer generateTask(final String outFilename, final double[] deformations) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/engine/" + outFilename + ".bmp").toURI()).toFile());

        final TaskContainer tc = TaskContainer.initTaskContainer(input);

        final AbstractROI roi = new RectangleROI(85, 85, 95, 95);

        tc.addRoi(ROUND, roi);
        tc.setDeformationLimits(ROUND, roi, deformations);

        tc.addHint(Hint.NO_STRAIN);
        tc.addHint(Hint.NO_STATS);

        tc.setParameter(TaskParameter.IN, input.get(0));
        tc.setParameter(TaskParameter.FACET_SIZE, 5);
        tc.setParameter(TaskParameter.SOLVER, Solver.BRUTE_FORCE);

        return tc;
    }

    private String computeAndCheckTask(final TaskContainer tc, final String fileName) {
        try {
            Engine.getInstance().computeTask(tc);
        } catch (ComputationException ex) {
            return generateDescription(fileName, tc, -1, ex.getLocalizedMessage());
        }

        return checkTask(tc, fileName);
    }

    private String checkTask(final TaskContainer tc, final String fileName) {
        final Image img1 = tc.getImage(ROUND);
        final Image img2 = tc.getImage(ROUND + 1);
        double[][][] results = tc.getResult(ROUND, ROUND + 1).getDisplacementResult().getDisplacement();

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
                        }
                    }
                }
            }
        }

        if (errorCount > 0) {
            final KernelType kt = (KernelType) tc.getParameter(TaskParameter.KERNEL);
            if (kt.isSafeToUse()) {
                return generateDescription(fileName, tc, errorCount);
            } else {
                System.out.println(" !!! Failed task, but kernel is not safe to use - " + generateDescription(fileName, tc, errorCount));
                return null;
            }
        } else {
            return null;
        }
    }

    private String generateDescription(final String fileName, final TaskContainer tc, int errorCount, String... extra) {
        final StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(fileName);
        sb.append("; ");
        sb.append(tc.getParameter(TaskParameter.KERNEL));
        sb.append("; ");
        sb.append(tc.getParameter(TaskParameter.INTERPOLATION));
        sb.append("; ");
        sb.append(tc.getParameter(TaskParameter.TASK_SPLIT_METHOD));
        sb.append("; ");
        sb.append(tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD));
        sb.append("; ");
        sb.append(tc.getParameter(TaskParameter.SOLVER));
        sb.append("; ");
        final Map<AbstractROI, double[]> limits = tc.getDeformationLimits(0);
        if (limits != null && limits.values().iterator().hasNext()) {
            sb.append(Arrays.toString(limits.values().iterator().next()));
        } else {
            sb.append("No limits !!!");
        }
        sb.append(" - ");
        sb.append(errorCount);
        sb.append("; ");
        if (tc.getResult(ROUND, ROUND + 1) != null) {
            sb.append(tc.getResult(ROUND, ROUND + 1).getCorrelations());
        } else {
            sb.append("NO RESULTS!");
        }
        for (String s : extra) {
            sb.append("; ");
            sb.append(s);
        }
        return sb.toString();
    }

    @Test
    public void testEngineMultiFacet() throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/engine/" + DEF_ZERO_FIRST_FILES[0] + ".bmp").toURI()).toFile());

        final TaskContainer tc = TaskContainer.initTaskContainer(input);

        final AbstractROI roi = new RectangleROI(85, 85, 95, 95);
        final int fs = 5;

        tc.addRoi(ROUND, roi);
        tc.setDeformationLimits(ROUND, roi, DEF_FIRST_F);
        tc.setParameter(TaskParameter.IN, input.get(0));
        tc.setParameter(TaskParameter.FACET_SIZE, fs);

        TaskContainerUtils.checkTaskValidity(tc);

        final AbstractTaskSolver solver = AbstractTaskSolver.initSolver(Solver.BRUTE_FORCE);
        solver.setKernel((KernelType) tc.getParameter(TaskParameter.KERNEL));
        solver.setInterpolation((Interpolation) tc.getParameter(TaskParameter.INTERPOLATION));
        final TaskSplitMethod taskSplit = (TaskSplitMethod) tc.getParameter(TaskParameter.TASK_SPLIT_METHOD);
        solver.setTaskSplitVariant(taskSplit, tc.getParameter(TaskParameter.TASK_SPLIT_PARAM));

        Map<AbstractROI, List<AbstractSubset>> subsets = new HashMap<>(1);
        final List<AbstractSubset> roiFacets = new ArrayList<>(4);
        roiFacets.add(new SquareSubset2D(fs, roi.getX1() + fs, roi.getY1() + fs));
        roiFacets.add(new SquareSubset2D(fs, roi.getX1() + fs, roi.getY1() + fs));
        roiFacets.add(new SquareSubset2D(fs, roi.getX1() + fs, roi.getY1() + fs));
        roiFacets.add(new SquareSubset2D(fs, roi.getX1() + fs, roi.getY1() + fs));
        subsets.put(roi, roiFacets);

        final Map<AbstractROI, List<CorrelationResult>> results = new HashMap<>(1);
        results.put(roi,
                solver.solve(
                        new FullTask(
                                tc.getImage(ROUND), tc.getImage(ROUND + 1),
                                roiFacets,
                                generateDeformations(tc.getDeformationLimits(ROUND, roi), roiFacets.size())),
                        DeformationUtils.getDegreeFromLimits(tc.getDeformationLimits(ROUND, roi)),
                        tc.getSubsetSize(ROUND, roi)));
        solver.endTask();

        final DisplacementResult displacement = DisplacementCalculator.computeDisplacement(results, subsets, tc, ROUND);
        tc.setResult(ROUND, ROUND + 1, new Result(results, displacement));

        Assert.assertEquals(roiFacets.size(), tc.getResult(ROUND, ROUND + 1).getCorrelations().get(roi).size());
        Assert.assertNull(checkTask(tc, DEF_ZERO_FIRST_FILES[0]));
    }

    private static List<double[]> generateDeformations(final double[] limits, final int subsetCount) {
        return Collections.nCopies(subsetCount, limits);
    }

    @Test
    public void testEngineMultiFacetLarge() throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/engine/" + DEF_ZERO_FIRST_FILES[0] + ".bmp").toURI()).toFile());

        final TaskContainer tc = TaskContainer.initTaskContainer(input);

        final AbstractROI roi = new RectangleROI(85, 85, 95, 95);
        final int fs = 5;

        tc.addRoi(ROUND, roi);
        tc.setDeformationLimits(ROUND, roi, DEF_LARGE);
        tc.setParameter(TaskParameter.IN, input.get(0));
        tc.setParameter(TaskParameter.FACET_SIZE, fs);

        TaskContainerUtils.checkTaskValidity(tc);

        final AbstractTaskSolver solver = AbstractTaskSolver.initSolver(Solver.BRUTE_FORCE);
        solver.setKernel((KernelType) tc.getParameter(TaskParameter.KERNEL));
        solver.setInterpolation((Interpolation) tc.getParameter(TaskParameter.INTERPOLATION));
        final TaskSplitMethod taskSplit = (TaskSplitMethod) tc.getParameter(TaskParameter.TASK_SPLIT_METHOD);
        solver.setTaskSplitVariant(taskSplit, tc.getParameter(TaskParameter.TASK_SPLIT_PARAM));

        Map<AbstractROI, List<AbstractSubset>> subsets = new HashMap<>(1);
        final List<AbstractSubset> roiFacets = new ArrayList<>(4);
        roiFacets.add(new SquareSubset2D(fs, roi.getX1() + fs, roi.getY1() + fs));
        roiFacets.add(new SquareSubset2D(fs, roi.getX1() + fs, roi.getY1() + fs));
        subsets.put(roi, roiFacets);

        final Map<AbstractROI, List<CorrelationResult>> results = new HashMap<>(1);
        results.put(roi,
                solver.solve(
                        new FullTask(
                                tc.getImage(ROUND), tc.getImage(ROUND + 1),
                                roiFacets,
                                generateDeformations(tc.getDeformationLimits(ROUND, roi), roiFacets.size())),
                        DeformationUtils.getDegreeFromLimits(tc.getDeformationLimits(ROUND, roi)),
                        tc.getSubsetSize(ROUND, roi)));
        solver.endTask();

        final DisplacementResult displacement = DisplacementCalculator.computeDisplacement(results, subsets, tc, ROUND);
        tc.setResult(ROUND, ROUND + 1, new Result(results, displacement));

        Assert.assertEquals(roiFacets.size(), tc.getResult(ROUND, ROUND + 1).getCorrelations().get(roi).size());
        Assert.assertNull(checkTask(tc, DEF_ZERO_FIRST_FILES[0]));

        final List<CorrelationResult> computedResults = tc.getResult(ROUND, ROUND + 1).getCorrelations().get(roi);
        CorrelationResult cr1, cr2;
        for (int i = 1; i < computedResults.size(); i++) {
            cr1 = computedResults.get(i - 1);
            Assert.assertNotNull(cr1);
            cr2 = computedResults.get(i);
            Assert.assertNotNull(cr2);
            Assert.assertArrayEquals(cr1.getDeformation(), cr2.getDeformation(), 0.001);
        }
    }
}
