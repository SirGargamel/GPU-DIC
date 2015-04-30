/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.task.TaskDefaultValues;
import cz.tul.dic.engine.opencl.kernels.KernelType;
import cz.tul.dic.engine.opencl.solvers.Solver;
import cz.tul.dic.engine.opencl.solvers.TaskSolver;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public final class WorkSizeManager {

    private static final long MAX_TIME_WIN = 2;
    private static final long MAX_TIME_LIN = 5;
    private static final long MAX_TIME_BASE = 1_000_000_000; // 1s
    private static final long MAX_TIME;
    private static final int INITIAL_WORK_SIZE_F = 1;
    private static final int INITIAL_WORK_SIZE_D = 1000;
    private static final double GROWTH_LIMIT_A = 0.5;
    private static final double GROWTH_LIMIT_B = 0.75;
    private static final double GROWTH_FACTOR_A = 0.75;
    private static final double GROWTH_FACTOR_B = 1.25;
    private static final KernelType BEST_KERNEL;
    private static final Map<KernelType, Map<Integer, Map<Integer, Long>>> TIME_DATA;
    private final KernelType kernel;
    private int workSizeF, workSizeD, maxF, maxD;

    static {
        final String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            MAX_TIME = MAX_TIME_WIN * MAX_TIME_BASE;
        } else {
            MAX_TIME = MAX_TIME_LIN * MAX_TIME_BASE;
        }
        TIME_DATA = new EnumMap<>(KernelType.class);
        for (KernelType kt : KernelType.values()) {
            TIME_DATA.put(kt, new HashMap<>());
        }

        Logger.debug("Initializing best kernel.");

        final TaskSolver solver = TaskSolver.initSolver(TaskDefaultValues.DEFAULT_SOLVER);
        solver.setInterpolation(TaskDefaultValues.DEFAULT_INTERPOLATION);
        solver.setTaskSplitVariant(TaskDefaultValues.DEFAULT_TASK_SPLIT_METHOD, TaskDefaultValues.DEFAULT_TASK_SPLIT_PARAMETER);

        try {
            for (KernelType kt : KernelType.values()) {
                solver.setKernel(kt);
                final Image img = Image.createImage(new BufferedImage(50, 50, BufferedImage.TYPE_BYTE_GRAY));
                final List<double[]> deformationLimits = new ArrayList<>(2);
                final double[] limits = new double[]{-49, 50, 0.05, -49, 50, 0.05};
                deformationLimits.add(limits);
                deformationLimits.add(limits);
                final int fs = 30;
                final List<Facet> facets = new ArrayList<>(2);
                facets.add(Facet.createFacet(fs, 0, 0));
                facets.add(Facet.createFacet(fs, 0, 0));
                solver.solve(
                        img, img,
                        facets,
                        deformationLimits, DeformationDegree.ZERO,
                        fs);
            }
        } catch (ComputationException ex) {
            Logger.warn("Failed to initialize work sizes.");
            Logger.debug(ex);
        }
        solver.endTask();
        // find best performing kernel        
        double performance;
        double bestPerformance = Double.NEGATIVE_INFINITY;
        KernelType bestKernel = null;
        for (KernelType kt : KernelType.values()) {
            for (Entry<Integer, Map<Integer, Long>> e : TIME_DATA.get(kt).entrySet()) {
                for (Entry<Integer, Long> e2 : e.getValue().entrySet()) {
                    performance = (e.getKey() * e2.getKey()) / (double) e2.getValue();
                    if (performance > bestPerformance) {
                        bestPerformance = performance;
                        bestKernel = kt;
                    }
                }
            }
        }
        BEST_KERNEL = bestKernel;
        Logger.debug("{0} selected as best kernel.", BEST_KERNEL);
    }

    public static KernelType getBestKernel() {
        return BEST_KERNEL;
    }

    public WorkSizeManager(final KernelType kernel) {
        this.kernel = kernel;
        reset();
    }

    public int getFacetCount() {
        return workSizeF;
    }

    public int getDeformationCount() {
        return workSizeD;
    }

    public void setMaxFacetCount(final int max) {
        this.maxF = max;
    }

    public void setMaxDeformationCount(final int max) {
        this.maxD = max;
    }

    public void reset() {
        workSizeF = INITIAL_WORK_SIZE_F;
        workSizeD = INITIAL_WORK_SIZE_D;
    }

    public void storeTime(final int workSizeF, final int workSizeD, final long time) {
        Map<Integer, Long> m = TIME_DATA.get(kernel).get(workSizeF);
        if (m == null) {
            m = new TreeMap<>();
            TIME_DATA.get(kernel).put(workSizeF, m);
        }

        m.put(workSizeD, time);
        computeNextWorkSize();
    }

    private void computeNextWorkSize() {
        if (!TIME_DATA.get(kernel).isEmpty()) {
            final long[] max = findMaxTimeValue();
            final int[] newMax = computeNewCount((int) max[0], (int) max[1], max[2]);
            workSizeF = newMax[0];
            workSizeD = newMax[1];
        }
    }

    private long[] findMaxTimeValue() {
        final long[] result = new long[]{0, 0, -1};

        long t;
        int f, d;
        for (Entry<Integer, Map<Integer, Long>> e : TIME_DATA.get(kernel).entrySet()) {
            for (Entry<Integer, Long> e2 : e.getValue().entrySet()) {
                t = e2.getValue();
                if (t < MAX_TIME) {
                    f = e.getKey();
                    d = e2.getKey();
                    if ((f == result[0] && d > result[1])
                            || f > result[0]) {
                        result[0] = f;
                        result[1] = d;
                        result[2] = t;
                    }
                }
            }
        }

        return result;
    }

    private int[] computeNewCount(final int oldMaxF, final int oldMaxD, final long time) {
        final int[] result = new int[]{oldMaxF, oldMaxD};
        if (oldMaxD < maxD) {
            result[1] = adjustValue(time, MAX_TIME, oldMaxD, maxD);
        } else {
            result[0] = adjustValue(time, MAX_TIME, oldMaxF, maxF);
        }

        return result;
    }

    private int adjustValue(final long currentTime, final long maxTime, final int value, final int maxValue) {
        final double ratio = currentTime / (double) maxTime;

        final int result;
        if (ratio < GROWTH_LIMIT_A) {
            result = (int) Math.floor(value / ratio * GROWTH_FACTOR_A);
        } else if (ratio < GROWTH_LIMIT_B) {
            result = (int) Math.floor(value * GROWTH_FACTOR_B);
        } else {
            result = value;
        }

        return Math.min(result, maxValue);
    }
}
