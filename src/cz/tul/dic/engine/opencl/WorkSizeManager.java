package cz.tul.dic.engine.opencl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Petr Jecmen
 */
public class WorkSizeManager {

    private static final double MAX_TIME_WIN = 2;
    private static final double MAX_TIME_LIN = 5;
    private static final int MAX_TIME_BASE = 1_000_000_000;
    private static final double MAX_TIME;
    private static final double LIMIT_RATIO = 0.75;
    private static final int INITIAL_WORK_SIZE = 32;
    private final Map<Integer, Long> timeData;
    private int workSize, max;

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
        workSize = INITIAL_WORK_SIZE;
    }

    public int getWorkSize() {
        return workSize;
    }

    public void setMaxCount(final int max) {
        this.max = max;
    }

    public void reset() {
        workSize = INITIAL_WORK_SIZE;
        timeData.clear();
    }

    public void storeTime(final int workSize, final long time) {
        timeData.put(workSize, time);
        computeNextWorkSize();
    }

    private void computeNextWorkSize() {
        int maxCount = 0;
        long maxTime = -1;

        if (timeData.isEmpty()) {
            maxCount = INITIAL_WORK_SIZE;
        } else if (timeData.size() == 1) {
            maxTime = timeData.values().iterator().next();
            maxCount = computeNewCount(INITIAL_WORK_SIZE, maxTime);
        } else if (timeData.size() > 1) {
            for (Entry<Integer, Long> e : timeData.entrySet()) {
                if (e.getKey() > maxCount) {
                    maxCount = e.getKey();
                    maxTime = e.getValue();
                }
            }
            maxCount = computeNewCount(maxCount, maxTime);
        }

        workSize = maxCount;
    }

    private int computeNewCount(final int oldCount, final long time) {
        int result;
        if (oldCount < max) {
            final long timePerFacet = time / oldCount;
            final int maxCount = (int) (MAX_TIME / timePerFacet);
            result = (int) (maxCount / LIMIT_RATIO);
        } else {
            result = Math.min(max, oldCount);
        }
        return result;
    }
}
