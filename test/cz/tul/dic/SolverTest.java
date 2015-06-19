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
import java.util.LinkedHashSet;
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
    private static final int COEFF_COUNT = 6;
    private static final double MAX_STEP_DIF = 1;
    private static final double[] LIMITS_0 = new double[]{
        -4, 12, 0.5, -2, 8, 0.5};
    private static final double[] LIMITS_0_EXTENDED = new double[]{
        -4, 8, 0.5, -2, 4, 0.5,
        -0.01, 0.01, 0.01, -0.01, 0.01, 0.01, -0.01, 0.01, 0.01, -0.01, 0.01, 0.01};
    private static final double[] LIMITS_1 = new double[]{
        -2, 2, 0.5, -1, 1, 0.5,
        -0.02, 0.02, 0.01, -0.02, 0.02, 0.01, -0.02, 0.02, 0.01, -0.02, 0.02, 0.01};
    private final Map<String, double[]> testFiles0, testFilesF;

    public SolverTest() {
        testFiles0 = new HashMap<>(3);
        testFiles0.put("speckle-[0.0, 0.0, 0.0, 0.0, 0.0, 0.0].bmp", new double[]{0, 0, 0, 0, 0, 0});
        testFiles0.put("speckle-[5.0, 0.0, 0.0, 0.0, 0.0, 0.0].bmp", new double[]{5, 0, 0, 0, 0, 0});
        testFiles0.put("speckle-[2.0, 3.0, 0.0, 0.0, 0.0, 0.0].bmp", new double[]{2, 3, 0, 0, 0, 0});

        testFilesF = new HashMap<>(4);
        testFilesF.put("speckle-[0.0, 0.0, 0.02, 0.0, 0.0, 0.0].bmp", new double[]{0, 0, 0.02, 0, 0, 0});
        testFilesF.put("speckle-[0.0, 0.0, 0.0, 0.0, -0.02, 0.0].bmp", new double[]{0, 0, 0, 0, -0.02, 0});
        testFilesF.put("speckle-[1.0, 0.0, 0.0, 0.0, 0.0, 0.01].bmp", new double[]{1.0, 0, 0, 0, 0, 0.01});
        testFilesF.put("speckle-[-1.0, -1.0, 0.0, 0.01, 0.0, 0.0].bmp", new double[]{-1.0, -1.0, 0, 0.01, 0, 0});
    }

    @Test
    public void testSolvers() throws IOException, URISyntaxException, ComputationException {
        final Set<String> errors = new LinkedHashSet<>();
        int counter = 0;
        String msg;
        TaskContainer task;
        for (Solver solver : Solver.values()) {
            for (Entry<String, double[]> e : testFiles0.entrySet()) {
                task = generateAndcomputeTask(e.getKey(), solver, LIMITS_0);
                msg = checkResult(e.getValue(), task);
                if (msg != null) {
                    errors.add(msg);
                    System.out.print("FAILED - ");
                }
                dumpResultsToConsole(solver, e.getKey(), LIMITS_0, task.getResult(BASE_ROUND, BASE_ROUND + 1).getCorrelations());
                counter++;

                task = generateAndcomputeTask(e.getKey(), solver, LIMITS_0_EXTENDED);
                msg = checkResult(e.getValue(), task);
                if (msg != null) {
                    errors.add(msg);
                    System.out.print("FAILED - ");
                }
                dumpResultsToConsole(solver, e.getKey(), LIMITS_0_EXTENDED, task.getResult(BASE_ROUND, BASE_ROUND + 1).getCorrelations());
                counter++;
            }
            for (Entry<String, double[]> e : testFilesF.entrySet()) {
                task = generateAndcomputeTask(e.getKey(), solver, LIMITS_1);
                msg = checkResult(e.getValue(), task);
                if (msg != null) {
                    errors.add(msg);
                    System.out.print("FAILED - ");
                }
                dumpResultsToConsole(solver, e.getKey(), LIMITS_1, task.getResult(BASE_ROUND, BASE_ROUND + 1).getCorrelations());
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
        task.setParameter(TaskParameter.FACET_GENERATOR_METHOD, FacetGeneratorMethod.TIGHT);
        task.setParameter(TaskParameter.FACET_GENERATOR_PARAM, 40);
        task.setParameter(TaskParameter.SOLVER, solver);

        Engine.getInstance().computeTask(task);

        return task;
    }

    private static String checkResult(final double[] expected, final TaskContainer task) {
        final double[] actual = condenseResults(task);
        final double[] limits = (double[]) task.getParameter(TaskParameter.DEFORMATION_LIMITS);

        String result = null;
        double maxDif;
        for (int dim = 0; dim < COEFF_COUNT; dim++) {
            maxDif = MAX_STEP_DIF * limits[dim * 3 + 2];
            if (Math.abs(expected[dim] - actual[dim]) > maxDif) {
                result = generateMessage(expected, actual, task);
                break;
            }
        }

        return result;
    }

    private static double[] condenseResults(final TaskContainer task) {
        final double[] result = new double[COEFF_COUNT];
        int counter = 0;

        double[] tmp;
        final Map<ROI, List<CorrelationResult>> results = task.getResult(BASE_ROUND, BASE_ROUND + 1).getCorrelations();
        for (List<CorrelationResult> l : results.values()) {
            for (CorrelationResult cor : l) {
                tmp = cor.getDeformation();
                for (int i = 0; i < COEFF_COUNT; i++) {
                    result[i] += tmp[i];
                }
                counter++;
            }
        }
        for (int i = 0; i < COEFF_COUNT; i++) {
            result[i] /= (double) counter;
        }

        return result;
    }

    private static void dumpResultsToConsole(final Solver solver, final String file, final double[] limits, final Map<ROI, List<CorrelationResult>> results) {
        final StringBuilder sb = new StringBuilder();
        sb.append(solver);
        sb.append(" -- ");
        sb.append(file);
        sb.append(" -- ");
        sb.append(Arrays.toString(limits));
        for (Entry<ROI, List<CorrelationResult>> e : results.entrySet()) {
            for (CorrelationResult cr : e.getValue()) {
                sb.append("\n  ");
                sb.append(cr);
            }
        }
        System.out.println(sb);
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
