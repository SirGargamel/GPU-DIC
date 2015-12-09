/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.data.deformation.DeformationOrder;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.data.roi.AbstractROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.engine.opencl.solvers.Solver;
import cz.tul.dic.data.subset.generator.SubsetGenerator;
import cz.tul.dic.engine.opencl.kernel.KernelInfo;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    private final Map<String, double[]> testFiles0, testFilesF;
    private final Map<String, String> testFilesSEM;
    private static final int BASE_ROUND = 0;
    private static final double LIMIT_ABS_DIF = 0.1;
    private static final double LIMIT_QUALITY_GOOD = 0.75;
    private static final double LIMIT_RATIO_BAD = 0.1;
    private static final Solver[] SOLVERS
            = Solver.values();
//            = new Solver[] {Solver.COARSE_FINE};    
    private static final int PARAM_SUBSET_SIZE = 20;

    public SolverTest() {
        testFiles0 = new LinkedHashMap<>(3);
        testFiles0.put("speckle-[0.0, 0.0, 0.0, 0.0, 0.0, 0.0].bmp", new double[]{0, 0, 0, 0, 0, 0});
        testFiles0.put("speckle-[0.72, 0.0, 0.0, 0.0, 0.0, 0.0].bmp", new double[]{0.72, 0, 0, 0, 0, 0});
        testFiles0.put("speckle-[-0.25, 0.85, 0.0, 0.0, 0.0, 0.0].bmp", new double[]{-0.25, 0.85, 0, 0, 0, 0});

        testFilesF = new LinkedHashMap<>(8);
        testFilesF.put("speckle-[0.0, 0.0, 0.07, 0.0, 0.0, 0.0].bmp", new double[]{0, 0, 0.07, 0, 0, 0});
        testFilesF.put("speckle-[0.0, 0.0, 0.0, 0.05, 0.0, 0.0].bmp", new double[]{0, 0, 0, 0.05, 0, 0});
        testFilesF.put("speckle-[0.0, 0.0, 0.0, 0.0, -0.04, 0.0].bmp", new double[]{0, 0, 0, 0, -0.04, 0});
        testFilesF.put("speckle-[0.0, 0.0, 0.0, 0.0, 0.0, 0.1].bmp", new double[]{0, 0, 0, 0, 0, 0.1});
        testFilesF.put("speckle-[0.0, 0.0, 0.02, -0.05, 0.0, 0.0].bmp", new double[]{0, 0, 0.02, -0.05, 0, 0.0});
        testFilesF.put("speckle-[0.0, 0.0, 0.0, 0.0, 0.14, -0.09].bmp", new double[]{0, 0, 0, 0, 0.14, -0.09});
        testFilesF.put("speckle-[0.42, 0.0, 0.0, 0.0, 0.0, 0.01].bmp", new double[]{0.42, 0, 0, 0, 0, 0.01});
        testFilesF.put("speckle-[-0.21, 0.75, 0.01, 0.0, 0.0, 0.0].bmp", new double[]{-0.21, 0.75, 0.01, 0.0, 0, 0});

        testFilesSEM = new HashMap<>(1);
        testFilesSEM.put("SEM-10-in.bmp", "SEM-10-out.bmp");
    }

    @Test
    public void testSolversDefault() throws IOException, URISyntaxException, ComputationException {
        final Set<String> errors = new LinkedHashSet<>();
        int counter = 0;
        String msg;
        TaskContainer task;
        for (Solver solver : SOLVERS) {
            if (solver.equals(Solver.BRUTE_FORCE)) {
                continue;
            }

            for (Entry<String, double[]> e : testFiles0.entrySet()) {
                task = generateAndComputeTask(e.getKey(), solver, DeformationOrder.ZERO);
                msg = checkResult(e.getValue(), task);
                errors.add(msg);
                counter++;
            }

            if (solver.supportsHigherOrderDeformation()) {
                for (Entry<String, double[]> e : testFiles0.entrySet()) {
                    task = generateAndComputeTask(e.getKey(), solver, DeformationOrder.FIRST);
                    msg = checkResult(e.getValue(), task);
                    errors.add(msg);
                    counter++;
                }

                for (Entry<String, double[]> e : testFilesF.entrySet()) {
                    task = generateAndComputeTask(e.getKey(), solver, DeformationOrder.FIRST);
                    msg = checkResult(e.getValue(), task);
                    errors.add(msg);
                    counter++;
                }
            }
        }
        errors.remove(null);
        Assert.assertEquals(errors.toString() + "\nTotal: " + counter + ",", 0, errors.size());
    }

    private static TaskContainer generateAndComputeTask(final String fileOut, final Solver solver, final DeformationOrder degree) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(SolverTest.class.getResource("/resources/solver/speckle.bmp").toURI()).toFile());
        input.add(Paths.get(SolverTest.class.getResource("/resources/solver/" + fileOut).toURI()).toFile());
        final TaskContainer task = TaskContainer.initTaskContainer(input);

        task.setParameter(TaskParameter.IN, input.get(0));
        task.setParameter(TaskParameter.ROUND_LIMITS, new int[]{0, 1});
        task.setParameter(TaskParameter.DEFORMATION_ORDER, degree);
        task.setParameter(TaskParameter.SUBSET_SIZE, PARAM_SUBSET_SIZE);
        task.setParameter(TaskParameter.SUBSET_GENERATOR_METHOD, SubsetGenerator.EQUAL);
        task.setParameter(TaskParameter.SUBSET_GENERATOR_PARAM, PARAM_SUBSET_SIZE);
        task.setParameter(TaskParameter.SOLVER, solver);
        task.setParameter(TaskParameter.FILTER_KERNEL_SIZE, -1);
        task.setParameter(TaskParameter.KERNEL, new KernelInfo(KernelInfo.Type.BEST, KernelInfo.Input.BEST, KernelInfo.Correlation.ZNSSD, KernelInfo.MemoryCoalescing.BEST));

        Engine.getInstance().computeTask(task);

        return task;
    }

    private static String checkResult(final double[] expected, final TaskContainer task) throws ComputationException {
        final int coeffCount = DeformationUtils.getDeformationCoeffCount((DeformationOrder) task.getParameter(TaskParameter.DEFORMATION_ORDER));

        String resultMsg = null;
        final Map<AbstractROI, List<CorrelationResult>> results = task.getResult(BASE_ROUND, BASE_ROUND + 1).getCorrelations();
        final Set<CorrelationResult> good = new LinkedHashSet<>();
        final Set<CorrelationResult> close = new LinkedHashSet<>();
        final Set<CorrelationResult> wrong = new LinkedHashSet<>();

        final double[] maxDif = new double[coeffCount];
        for (int dim = 0; dim < coeffCount; dim++) {
            maxDif[dim] = LIMIT_ABS_DIF * expected[dim];
        }

        double[] actual;
        boolean goodResult;
        for (List<CorrelationResult> l : results.values()) {
            for (CorrelationResult cor : l) {
                goodResult = true;
                actual = cor.getDeformation();
                for (int dim = 0; dim < coeffCount; dim++) {
                    if (Math.abs(expected[dim] - actual[dim]) > maxDif[dim]) {
                        if (cor.getQuality() >= LIMIT_QUALITY_GOOD) {
                            close.add(cor);
                        } else {
                            wrong.add(cor);
                        }
                        goodResult = false;
                        break;
                    }
                }
                if (goodResult) {
                    good.add(cor);
                }
            }
        }

        final StringBuilder sb = new StringBuilder();
        if (wrong.size() > 0) {
            final double badRatio = wrong.size() / (double) (good.size() + close.size() + wrong.size());
            if (badRatio > LIMIT_RATIO_BAD) {
                resultMsg = generateMessage(expected, task);
                sb.append("FAILED - ");
            }
        }
        if (resultMsg == null) {
            if (!close.isEmpty()) {
                sb.append("GOOD QUALITY - ");
            } else {
                sb.append("FINISHED - ");
            }
        }

        sb.append(Arrays.toString(expected));
        sb.append(" -- ");
        sb.append(task.getParameter(TaskParameter.DEFORMATION_ORDER));
        sb.append(" -- ");
        sb.append(task.getParameter(TaskParameter.SOLVER));
        if (!good.isEmpty()) {
            sb.append("\nGOOD -- ");
            for (CorrelationResult cr : good) {
                sb.append("\n  ");
                sb.append(cr);
            }
        }
        if (!close.isEmpty()) {
            sb.append("\nCLOSE -- ");
            for (CorrelationResult cr : close) {
                sb.append("\n  ");
                sb.append(cr);
            }
        }
        if (!wrong.isEmpty()) {
            sb.append("\nWRONG -- ");
            for (CorrelationResult cr : wrong) {
                sb.append("\n  ");
                sb.append(cr);
            }
        }
        System.out.println(sb);

        return resultMsg;
    }

    private static String generateMessage(final double[] expected, final TaskContainer task) {
        final StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(Arrays.toString(expected));

        sb.append("; Solver: ");
        sb.append(task.getParameter(TaskParameter.SOLVER));
        sb.append("; Limits: ");
        sb.append(Arrays.toString((double[]) task.getParameter(TaskParameter.DEFORMATION_LIMITS)));

        return sb.toString();
    }

    @Test
    public void testSolversWeighed() throws IOException, URISyntaxException, ComputationException {
        final Set<String> errors = new LinkedHashSet<>();
        int counter = 0;
        String msg;
        TaskContainer task;
        for (Solver solver : SOLVERS) {
            if (!solver.supportsWeighedCorrelation()) {
                continue;
            }

            for (Entry<String, double[]> e : testFiles0.entrySet()) {
                task = generateAndComputeTaskWeighed(e.getKey(), solver, DeformationOrder.ZERO);
                msg = checkResult(e.getValue(), task);
                errors.add(msg);
                counter++;
            }

            if (solver.supportsHigherOrderDeformation()) {
                for (Entry<String, double[]> e : testFiles0.entrySet()) {
                    task = generateAndComputeTaskWeighed(e.getKey(), solver, DeformationOrder.FIRST);
                    msg = checkResult(e.getValue(), task);
                    errors.add(msg);
                    counter++;
                }

                for (Entry<String, double[]> e : testFilesF.entrySet()) {
                    task = generateAndComputeTaskWeighed(e.getKey(), solver, DeformationOrder.FIRST);
                    msg = checkResult(e.getValue(), task);
                    errors.add(msg);
                    counter++;
                }
            }
        }
        errors.remove(null);
        Assert.assertEquals(errors.toString() + "\nTotal: " + counter + ",", 0, errors.size());
    }

    private static TaskContainer generateAndComputeTaskWeighed(final String fileOut, final Solver solver, final DeformationOrder degree) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(SolverTest.class.getResource("/resources/solver/speckle.bmp").toURI()).toFile());
        input.add(Paths.get(SolverTest.class.getResource("/resources/solver/" + fileOut).toURI()).toFile());
        final TaskContainer task = TaskContainer.initTaskContainer(input);

        task.setParameter(TaskParameter.IN, input.get(0));
        task.setParameter(TaskParameter.ROUND_LIMITS, new int[]{0, 1});
        task.setParameter(TaskParameter.DEFORMATION_ORDER, degree);
        task.setParameter(TaskParameter.SUBSET_SIZE, 20);
        task.setParameter(TaskParameter.SUBSET_GENERATOR_METHOD, SubsetGenerator.EQUAL);
        task.setParameter(TaskParameter.SUBSET_GENERATOR_PARAM, PARAM_SUBSET_SIZE);
        task.setParameter(TaskParameter.SOLVER, solver);
        task.setParameter(TaskParameter.FILTER_KERNEL_SIZE, -1);
        // weighed correlation weigh too large to show effect
        task.setParameter(TaskParameter.KERNEL, new KernelInfo(KernelInfo.Type.BEST, KernelInfo.Input.BEST, KernelInfo.Correlation.WZNSSD, KernelInfo.MemoryCoalescing.BEST));
        task.setParameter(TaskParameter.CORRELATION_WEIGHT, 2.0);

        Engine.getInstance().computeTask(task);

        return task;
    }

    @Test
    public void testSolversSEM() throws IOException, URISyntaxException, ComputationException {
        final Set<String> errors = new LinkedHashSet<>();
        int counter = 0;
        String msg;
        TaskContainer task;
        for (Solver solver : SOLVERS) {
            if (solver.supportsHigherOrderDeformation()) {
                for (Entry<String, String> e : testFilesSEM.entrySet()) {
                    task = generateAndComputeTaskSEM(e.getKey(), e.getValue(), solver);
                    msg = checkResultSEM(task);
                    errors.add(msg);
                    counter++;
                }
            }
        }
        errors.remove(null);
        Assert.assertEquals(errors.toString() + "\nTotal: " + counter + ",", 0, errors.size());
    }

    private static TaskContainer generateAndComputeTaskSEM(final String fileIn, final String fileOut, final Solver solver) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(SolverTest.class.getResource("/resources/solver/" + fileIn).toURI()).toFile());
        input.add(Paths.get(SolverTest.class.getResource("/resources/solver/" + fileOut).toURI()).toFile());
        final TaskContainer task = TaskContainer.initTaskContainer(input);

        task.setParameter(TaskParameter.IN, input.get(0));
        task.setParameter(TaskParameter.ROUND_LIMITS, new int[]{0, 1});
        task.setParameter(TaskParameter.SUBSET_SIZE, PARAM_SUBSET_SIZE);
        task.setParameter(TaskParameter.SUBSET_GENERATOR_METHOD, SubsetGenerator.EQUAL);
        task.setParameter(TaskParameter.SUBSET_GENERATOR_PARAM, PARAM_SUBSET_SIZE * 2);
        task.setParameter(TaskParameter.SOLVER, solver);
        task.setParameter(TaskParameter.FILTER_KERNEL_SIZE, -1);

        Engine.getInstance().computeTask(task);

        return task;
    }

    private static String checkResultSEM(final TaskContainer task) throws ComputationException {
        String resultMsg = null;
        final Map<AbstractROI, List<CorrelationResult>> results = task.getResult(BASE_ROUND, BASE_ROUND + 1).getCorrelations();
        final Set<CorrelationResult> good = new LinkedHashSet<>();
        final Set<CorrelationResult> wrong = new LinkedHashSet<>();

        for (List<CorrelationResult> l : results.values()) {
            for (CorrelationResult cor : l) {
                if (cor.getQuality() >= LIMIT_QUALITY_GOOD) {
                    good.add(cor);
                } else {
                    wrong.add(cor);
                }
            }
        }

        final StringBuilder sb = new StringBuilder();
        if (wrong.size() > 0) {
            final double badRatio = wrong.size() / (double) (good.size() + wrong.size());
            if (badRatio > LIMIT_RATIO_BAD) {
                resultMsg = generateMessage(task);
                sb.append("FAILED - ");
            }
        }
        if (resultMsg == null) {
            sb.append("FINISHED - ");
        }

        sb.append(task.getParameter(TaskParameter.IN));
        sb.append(" -- ");
        sb.append(task.getParameter(TaskParameter.DEFORMATION_ORDER));
        sb.append(" -- ");
        sb.append(task.getParameter(TaskParameter.SOLVER));
        if (!good.isEmpty()) {
            sb.append("\nGOOD QUALITY -- ");
            for (CorrelationResult cr : good) {
                sb.append("\n  ");
                sb.append(cr);
            }
        }
        if (!wrong.isEmpty()) {
            sb.append("\nBAD QUALITY -- ");
            for (CorrelationResult cr : wrong) {
                sb.append("\n  ");
                sb.append(cr);
            }
        }
        System.out.println(sb);

        return resultMsg;
    }

    private static String generateMessage(final TaskContainer task) {
        final StringBuilder sb = new StringBuilder();
        sb.append("\nSolver: ");
        sb.append(task.getParameter(TaskParameter.SOLVER));
        sb.append("; Limits: ");
        sb.append(Arrays.toString((double[]) task.getParameter(TaskParameter.DEFORMATION_LIMITS)));

        return sb.toString();
    }
}
