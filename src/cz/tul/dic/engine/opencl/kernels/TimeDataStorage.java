/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernels;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class TimeDataStorage implements Serializable {

    private static final String FILE_STORE = "performance.kernel";
    private static final TimeDataStorage INSTANCE;
    private Map<KernelInfo, Map<Long, Map<Long, Long>>> timeData;

    static {
        INSTANCE = new TimeDataStorage();
    }

    private TimeDataStorage() {
        timeData = new HashMap<>();
    }

    public static TimeDataStorage getInstance() {
        return INSTANCE;
    }

    public Map<KernelInfo, Map<Long, Map<Long, Long>>> getFullData() {
        return timeData;
    }

    public Map<Long, Map<Long, Long>> getTimeData(final KernelInfo kernel) {
        return timeData.get(kernel);
    }

    public void storeTime(final KernelInfo kernel, final long workSizeF, final long workSizeD, final long time) {
        Map<Long, Map<Long, Long>> m = timeData.get(kernel);
        if (m == null) {
            m = new TreeMap<>();
            timeData.put(kernel, m);
        }

        Map<Long, Long> m2 = m.get(workSizeF);
        if (m2 == null) {
            m2 = new TreeMap<>();
            m.put(workSizeF, m2);
        }

        m2.put(workSizeD, time);
    }

    public boolean loadTimeDataFromFile() {
        boolean result = false;
        try (final ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE_STORE))) {
            timeData = (Map<KernelInfo, Map<Long, Map<Long, Long>>>) in.readObject();
            result = true;
        } catch (IOException | ClassNotFoundException ex) {
            Logger.error(ex, "Error reading time data.");
        }
        return result;
    }

    public void storeTimeDataToFile() {
        try (final ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE_STORE))) {
            out.writeObject(timeData);
        } catch (IOException ex) {
            Logger.error(ex, "Error storing time data.");
        }
    }

}
