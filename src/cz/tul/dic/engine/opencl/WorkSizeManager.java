/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.subset.SquareSubset2D;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.task.TaskDefaultValues;
import cz.tul.dic.data.task.FullTask;
import cz.tul.dic.engine.opencl.kernels.KernelType;
import cz.tul.dic.engine.opencl.solvers.AbstractTaskSolver;
import cz.tul.dic.engine.opencl.solvers.Solver;
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
    private static final Map<KernelType, Map<Long, Map<Long, Long>>> TIME_DATA;
    private final KernelType kernel;
    private long workSizeF, workSizeD, maxF, maxD;    

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

        final AbstractTaskSolver solver = AbstractTaskSolver.initSolver(Solver.COARSE_FINE);
        solver.setInterpolation(TaskDefaultValues.DEFAULT_INTERPOLATION);
        solver.setTaskSplitVariant(TaskDefaultValues.DEFAULT_TASK_SPLIT_METHOD, TaskDefaultValues.DEFAULT_TASK_SPLIT_PARAMETER);

        try {
            for (KernelType kt : KernelType.values()) {
                if (kt.isSafeToUse()) {
                    solver.setKernel(kt);
                    final Image img = Image.createImage(new BufferedImage(50, 50, BufferedImage.TYPE_BYTE_GRAY));
                    final List<double[]> deformationLimits = new ArrayList<>(2);
                    final double[] limits = new double[]{-49, 50, 0.05, -49, 50, 0.05};
                    deformationLimits.add(limits);
                    deformationLimits.add(limits);
                    final int fs = 14;
                    final List<AbstractSubset> subsets = new ArrayList<>(2);
                    subsets.add(new SquareSubset2D(fs, 15, 15));
                    subsets.add(new SquareSubset2D(fs, 15, 15));
                    solver.solve(
                            new FullTask(img, img, subsets, deformationLimits),
                            DeformationDegree.ZERO,
                            fs);
                }
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
            for (Entry<Long, Map<Long, Long>> e : TIME_DATA.get(kt).entrySet()) {
                for (Entry<Long, Long> e2 : e.getValue().entrySet()) {
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

    public WorkSizeManager(final KernelType kernel) {
        this.kernel = kernel;
        reset();
    }

    public static KernelType getBestKernel() {
        return BEST_KERNEL;
    }

    public long getFacetCount() {
        return workSizeF;
    }

    public long getDeformationCount() {
        return workSizeD;
    }

    public void setMaxFacetCount(final int max) {
        this.maxF = max;
    }

    public void setMaxDeformationCount(final long max) {
        this.maxD = max;
    }

    public void reset() {
        workSizeF = INITIAL_WORK_SIZE_F;
        workSizeD = INITIAL_WORK_SIZE_D;
    }

    public void storeTime(final long workSizeF, final long workSizeD, final long time) {
        Map<Long, Long> m = TIME_DATA.get(kernel).get(workSizeF);
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
            final long[] newMax = computeNewCount((int) max[0], (int) max[1], max[2]);
            workSizeF = newMax[0];
            workSizeD = newMax[1];
        }
    }

    private long[] findMaxTimeValue() {
        final long[] result = new long[]{0, 0, -1};

        long time;
        long subsetCount, deformationCount;
        for (Entry<Long, Map<Long, Long>> e : TIME_DATA.get(kernel).entrySet()) {
            for (Entry<Long, Long> e2 : e.getValue().entrySet()) {
                time = e2.getValue();
                subsetCount = e.getKey();
                deformationCount = e2.getKey();
                if (isPerformanceBetter(time, subsetCount, deformationCount, result)) {
                    result[0] = subsetCount;
                    result[1] = deformationCount;
                    result[2] = time;
                }
            }
        }

        return result;
    }

    private static boolean isPerformanceBetter(final long time, final long subsetCount, final long deformationCount, final long[] result) {
        return (time < MAX_TIME) && ((subsetCount == result[0] && deformationCount > result[1])
                || subsetCount > result[0]);
    }

    private long[] computeNewCount(final int oldMaxF, final int oldMaxD, final long time) {
        final long[] result = new long[]{oldMaxF, oldMaxD};
        if (oldMaxD < maxD) {
            result[1] = adjustValue(time, MAX_TIME, oldMaxD, maxD);
        } else {
            result[0] = adjustValue(time, MAX_TIME, oldMaxF, maxF);
        }

        return result;
    }

    private long adjustValue(final long currentTime, final long maxTime, final long value, final long maxValue) {
        final double ratio = currentTime / (double) maxTime;

        final long result;
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
