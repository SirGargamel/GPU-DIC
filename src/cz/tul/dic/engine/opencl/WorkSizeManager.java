/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl;

import cz.tul.dic.engine.opencl.kernel.KernelInfo;
import cz.tul.dic.engine.opencl.kernel.TimeDataStorage;
import java.util.Map;
import java.util.Map.Entry;

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
    private final KernelInfo kernel;
    private long workSizeS, workSizeD, maxF, maxD;

    static {
        final String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            MAX_TIME = MAX_TIME_WIN * MAX_TIME_BASE;
        } else {
            MAX_TIME = MAX_TIME_LIN * MAX_TIME_BASE;
        }
    }

    public WorkSizeManager(final KernelInfo kernel) {
        this.kernel = kernel;
        reset();
    }

    public long getSubsetCount() {
        return workSizeS;
    }

    public long getDeformationCount() {
        return workSizeD;
    }

    public void setMaxSubsetCount(final int max) {
        this.maxF = max;
    }

    public void setMaxDeformationCount(final long max) {
        this.maxD = max;
    }

    public void reset() {
        workSizeS = INITIAL_WORK_SIZE_F;
        workSizeD = INITIAL_WORK_SIZE_D;
    }

    public void storeTime(final long workSizeF, final long workSizeD, final long time) {
        TimeDataStorage.getInstance().storeTime(kernel, workSizeF, workSizeD, time);
        computeNextWorkSize();
    }

    private void computeNextWorkSize() {
        if (!TimeDataStorage.getInstance().getTimeData(kernel).isEmpty()) {
            final long[] max = findMaxTimeValue(kernel);
            final long[] newMax = computeNewCount((int) max[0], (int) max[1], max[2]);
            workSizeS = newMax[0];
            workSizeD = newMax[1];
        }
    }

    private static long[] findMaxTimeValue(final KernelInfo kernel) {
        final long[] result = new long[]{0, 0, -1};

        long time;
        long subsetCount, deformationCount;
        for (Entry<Long, Map<Long, Long>> e : TimeDataStorage.getInstance().getTimeData(kernel).entrySet()) {
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

    private static long adjustValue(final long currentTime, final long maxTime, final long value, final long maxValue) {
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
