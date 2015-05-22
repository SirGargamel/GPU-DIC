/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.engine.opencl.solvers.Solver;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Petr Ječmen
 */
public class SolverTest {

    private static final int BASE_ROUND = 0;
    private static final int RESULT_LENGTH = 6;
    private static final double DELTA_0 = 0.5;
    private static final double DELTA_1 = 0.05;
    private static final double[] LIMITS_0 = new double[]{
        -4, 12, 0.5, -2, 8, 0.5};
    private static final double[] LIMITS_F = new double[]{
        -2, 6, 1, -1, 4, 1,
        -0.05, 0.15, 0.05, -0.05, 0.05, 0.05, -0.15, 0.15, 0.05, -0.05, 0.05, 0.05};
    private final Map<String, double[]> testFiles0, testFilesF;

    public SolverTest() {
        testFiles0 = new HashMap<>(3);
        testFiles0.put("speckle-[0.0, 0.0, 0.0, 0.0, 0.0, 0.0].bmp", new double[]{0, 0, 0, 0, 0, 0});
        testFiles0.put("speckle-[5.0, 0.0, 0.0, 0.0, 0.0, 0.0].bmp", new double[]{5, 0, 0, 0, 0, 0});
        testFiles0.put("speckle-[2.0, 3.0, 0.0, 0.0, 0.0, 0.0].bmp", new double[]{2, 3, 0, 0, 0, 0});

        testFilesF = new HashMap<>(4);
        testFilesF.put("speckle-[0.0, 0.0, 0.1, 0.0, 0.0, 0.0].bmp", new double[]{0, 0, 0.1, 0, 0, 0});
        testFilesF.put("speckle-[0.0, 0.0, 0.0, 0.0, -0.1, 0.0].bmp", new double[]{0, 0, 0, 0, -0.1, 0});
        testFilesF.put("speckle-[1.0, 0.0, 0.05, 0.0, 0.1, 0.0].bmp", new double[]{1.0, 0, 0.05, 0, 0.1, 0});
        testFilesF.put("speckle-[-1.0, -1.0, 0.0, 0.25, -0.1, 0.0].bmp", new double[]{-1.0, -1.0, 0, 0.25, -0.1, 0});
    }

    @Test
    public void testSolvers() throws IOException, URISyntaxException, ComputationException {
        final Set<String> errors = new HashSet<>();
        int counter = 0;
        for (Solver solver : Solver.values()) {
            for (Entry<String, double[]> e : testFiles0.entrySet()) {
                errors.add(checkResult(e.getValue(), generateAndcomputeTask(e.getKey(), solver, LIMITS_0)));                
                counter++;
            }
            for (Entry<String, double[]> e : testFilesF.entrySet()) {
                errors.add(checkResult(e.getValue(), generateAndcomputeTask(e.getKey(), solver, LIMITS_F)));
                counter++;
            }
        }
        errors.remove(null);
        Assert.assertEquals(errors.toString() + "\nTotal: " + counter + ",", 0, errors.size());        
    }

    private static TaskContainer generateAndcomputeTask(final String fileOut, final Solver solver, final double[] defLimits) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(SolverTest.class.getResource("/resources/speckle.bmp").toURI()).toFile());
        input.add(Paths.get(SolverTest.class.getResource("/resources/" + fileOut).toURI()).toFile());
        final TaskContainer task = new TaskContainer(input);
        InputLoader.loadInput(task);

        task.setParameter(TaskParameter.IN, input.get(0));
        task.setParameter(TaskParameter.ROUND_LIMITS, new int[]{0, 1});
        task.setParameter(TaskParameter.DEFORMATION_LIMITS, defLimits);
        task.setParameter(TaskParameter.FACET_SIZE, 40);
        task.setParameter(TaskParameter.FACET_GENERATOR_METHOD, FacetGeneratorMethod.CLASSIC);
        task.setParameter(TaskParameter.FACET_GENERATOR_PARAM, 0);
        task.setParameter(TaskParameter.SOLVER, solver);

        Engine.getInstance().computeTask(task);

        return task;
    }

    private static String checkResult(final double[] expected, final TaskContainer task) {
        final double[] actual = condenseResults(task);

        String result = null;
        for (int dim = 0; dim < 2; dim++) {
            if (Math.abs(expected[dim] - actual[dim]) > DELTA_0) {
                result = generateMessage(expected, actual, task);
                break;
            }
        }
        if (result == null) {
            for (int dim = 2; dim < RESULT_LENGTH; dim++) {
                if (Math.abs(expected[dim] - actual[dim]) > DELTA_1) {
                    result = generateMessage(expected, actual, task);
                    break;
                }
            }
        }
        return result;
    }

    private static double[] condenseResults(final TaskContainer task) {
        final double[] result = new double[RESULT_LENGTH];
        int counter = 0;

        double[] tmp;
        final Map<ROI, List<CorrelationResult>> results = task.getResult(BASE_ROUND, BASE_ROUND + 1).getCorrelations();
        for (List<CorrelationResult> l : results.values()) {
            for (CorrelationResult cor : l) {
                tmp = cor.getDeformation();
                for (int i = 0; i < RESULT_LENGTH; i++) {
                    result[i] += tmp[i];
                }
                counter++;
            }
        }
        for (int i = 0; i < RESULT_LENGTH; i++) {
            result[i] /= (double) counter;
        }

        return result;
    }

    private static String generateMessage(final double[] expected, final double[] actual, final TaskContainer task) {
        final StringBuilder sb = new StringBuilder();
        sb.append("\nExpected ");
        sb.append(Arrays.toString(expected));
        sb.append(", was ");
        sb.append(Arrays.toString(actual));

        sb.append("; Solver: ");
        sb.append(task.getParameter(TaskParameter.SOLVER));
        sb.append("; Limits: ");
        sb.append(Arrays.toString((double[]) task.getParameter(TaskParameter.DEFORMATION_LIMITS)));

        return sb.toString();
    }
}
