package cz.tul.dic.engine.opencl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 *
 * @author Petr Jecmen
 */
public final class WorkSizeManager {

    private static final long MAX_TIME_WIN = 2;
    private static final long MAX_TIME_LIN = 5;
    private static final long MAX_TIME_BASE = 1_000_000_000; // 1s
    private static final long MAX_TIME;
    private static final double LIMIT_RATIO = 0.75;
    private static final int INITIAL_WORK_SIZE_F = 1;
    private static final int INITIAL_WORK_SIZE_D = 1_000;
    private static final double GROWTH_LIMIT_A = 0.5;
    private static final double GROWTH_LIMIT_B = 0.75;
    private static final double GROWTH_FACTOR_A = 2;
    private static final double GROWTH_FACTOR_B = 1.5;
    private final Map<Integer, Map<Integer, Long>> timeData;
    private int workSizeF, workSizeD, maxF, maxD;

    static {
        final String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            MAX_TIME = MAX_TIME_WIN * MAX_TIME_BASE;
        } else {
            MAX_TIME = MAX_TIME_LIN * MAX_TIME_BASE;
        }
    }

    public WorkSizeManager() {
        timeData = new HashMap<>();
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
        timeData.clear();
    }

    public void storeTime(final int workSizeF, final int workSizeD, final long time) {
        Map<Integer, Long> m = timeData.get(workSizeF);
        if (m == null) {
            m = new TreeMap<>();
            timeData.put(workSizeF, m);
        }

        m.put(workSizeD, time);
        computeNextWorkSize();
    }

    private void computeNextWorkSize() {
        if (!timeData.isEmpty()) {
            final long[] max = findMaxValue();
            final int[] newMax = computeNewCount((int) max[0], (int) max[1], max[2]);
            workSizeF = newMax[0];
            workSizeD = newMax[1];
        }
    }

    private long[] findMaxValue() {
        final long[] result = new long[]{0, 0, -1};

        long t;
        int f, d;
        for (Entry<Integer, Map<Integer, Long>> e : timeData.entrySet()) {
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
            result = (int) Math.floor(value * GROWTH_FACTOR_A);
        } else if (ratio < GROWTH_LIMIT_B) {
            result = (int) Math.floor(value * GROWTH_FACTOR_B);
        } else {
            result = value;
        }

        return Math.min(result, maxValue);
    }
}
