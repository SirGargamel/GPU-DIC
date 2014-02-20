package cz.tul.dic.engine.opencl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 *
 * @author Petr Jecmen
 */
public class WorkSizeManager {

    private static final double MAX_TIME_WIN = 2;
    private static final double MAX_TIME_LIN = 5;
    private static final int MAX_TIME_BASE = 1000000000;
    private static final double MAX_TIME;
    private static final int INITIAL_WORK_SIZE = 16;
    private final Map<Class, Map<Integer, Double>> timeData;
    private final Map<Class, Integer> workSize;
    private int baseWorkSize;

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
        workSize = new HashMap<>();

        baseWorkSize = INITIAL_WORK_SIZE;
    }

    public int getWorkSize(final Class cls) {
        int result = baseWorkSize;

        if (workSize.containsKey(cls)) {
            return workSize.get(cls);
        }

        return result;
    }

    public void forceWorkSize(final Class cls, final int count) {
        if (count > baseWorkSize) {
            workSize.put(cls, count);
        } else {
            workSize.put(cls, baseWorkSize);
        }
    }

    public void reset(final Class cls) {
        if (timeData.containsKey(cls)) {
            timeData.get(cls).clear();
            workSize.remove(cls);
        }
    }

    public void storeTime(final Class cls, final int workSize, final double time) {
        Map<Integer, Double> map = timeData.get(cls);
        if (map == null) {
            map = new TreeMap<>();
            timeData.put(cls, map);
        }

        map.put(workSize, time);
        computeNextWorkSize(cls);
    }

    private void computeNextWorkSize(final Class cls) {
        int maxCount = 0;
        double maxTime = -1;

        final Map<Integer, Double> values = timeData.get(cls);

        if (values.isEmpty()) {
            maxCount = baseWorkSize;
        } else if (values.size() == 1) {
            maxTime = values.values().iterator().next();
            maxCount = computeNewCount(baseWorkSize, maxTime);
        } else if (values.size() > 1) {
            for (Entry<Integer, Double> e : values.entrySet()) {
                if (e.getKey() > maxCount) {
                    maxCount = e.getKey();
                    maxTime = e.getValue();
                }
            }
            maxCount = computeNewCount(maxCount, maxTime);
        }

        workSize.put(cls, maxCount);
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

    public void setBaseWorkSize(int baseWorkSize) {
        this.baseWorkSize = baseWorkSize;
    }
}
