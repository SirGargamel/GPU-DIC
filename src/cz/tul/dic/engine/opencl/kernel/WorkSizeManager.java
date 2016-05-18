/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernel;

import cz.tul.dic.engine.DeviceType;
import cz.tul.dic.engine.platform.Platform;
import cz.tul.dic.engine.platform.PlatformDefinition;
import cz.tul.dic.engine.platform.PlatformType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 *
 * @author Petr Jecmen
 */
public final class WorkSizeManager {

    private static final Map<PlatformType, Map<DeviceType, PerformanceData>> PERFORMANCE_DATA;
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
    private final PerformanceData performance;
    private long workSizeS, workSizeD, maxF, maxD;

    static {
        final String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            MAX_TIME = MAX_TIME_WIN * MAX_TIME_BASE;
        } else {
            MAX_TIME = MAX_TIME_LIN * MAX_TIME_BASE;
        }
        PERFORMANCE_DATA = new EnumMap<>(PlatformType.class);
    }

    public WorkSizeManager(final Platform platform) {
        final PlatformDefinition paltformDef = platform.getPlatformDefinition();
        Map<DeviceType, PerformanceData> m = PERFORMANCE_DATA.get(paltformDef.getPlatform());
        if (m == null) {
            m = new EnumMap<>(DeviceType.class);
            PERFORMANCE_DATA.put(paltformDef.getPlatform(), m);
        }
        if (m.containsKey(paltformDef.getDevice())) {
            performance = m.get(paltformDef.getDevice());
        } else {
            performance = new PerformanceData();
            performance.storeTime(1, 1000, (long) (MAX_TIME * 0.75));
            m.put(paltformDef.getDevice(), performance);
        }
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
        computeNextWorkSize();
    }

    public void storeTime(final long workSizeF, final long workSizeD, final long time) {
        computeNextWorkSize();
    }

    private void computeNextWorkSize() {
        final long[] max = findMaxTimeValue();
        final long[] newMax = computeNewCount((int) max[0], (int) max[1], max[2]);
        workSizeS = newMax[0];
        workSizeD = newMax[1];
    }

    private long[] findMaxTimeValue() {
        final long[] result = new long[]{0, 0, -1};

        long time;
        long subsetCount, deformationCount;
        for (Entry<Long, Map<Long, Long>> e : performance.data.entrySet()) {
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

    private static class PerformanceData {

        private final Map<Long, Map<Long, Long>> data;

        public PerformanceData() {
            this.data = new TreeMap<>();
        }

        public void storeTime(final long subsetCount, final long deformationCount, final long time) {
            Map<Long, Long> m = data.get(subsetCount);
            if (m == null) {
                m = new TreeMap<>();
                data.put(subsetCount, m);
            }
            m.put(deformationCount, time);
        }
    }
}
