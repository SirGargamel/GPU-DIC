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
    private static final int MAX_TIME_BASE = 1000000000;
    private static final double MAX_TIME;
    private static final int INITIAL_WORK_SIZE = 1;
    private final Map<Integer, Long> timeData;
    private int workSize;

    static {
        final String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("win") >= 0) {
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

    public void reset() {
        workSize = INITIAL_WORK_SIZE;
    }

    public void storeTime(final int workSize, final long time) {        
        timeData.put(workSize, time);
        computeNextWorkSize();
    }

    private void computeNextWorkSize() {
        int maxCount = 0;
        double maxTime = -1;        

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

    private static int computeNewCount(final int oldCount, final double time) {
        int result = oldCount;
        if (time < (MAX_TIME / 2)) {
            result = 2 * oldCount;
        } else if (time < (MAX_TIME / 4 * 3)) {
            result = oldCount * 3 / 2;
        }
        return result;
    }
}
