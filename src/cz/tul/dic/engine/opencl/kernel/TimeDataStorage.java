/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernel;

import cz.tul.pj.journal.Journal;
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

    private static final String FILE_STORE_A = "performance.kernel.A";
    private static final String FILE_STORE_B = "performance.kernel.B";
    private static final TimeDataStorage INSTANCE;
    private Map<KernelInfo, Map<Long, Map<Long, Long>>> timeDataA;
    private Map<Long, Map<Long, Map<KernelInfo, Long>>> timeDataB;

    static {
        INSTANCE = new TimeDataStorage();
    }

    private TimeDataStorage() {
        timeDataA = new HashMap<>();
        timeDataB = new TreeMap<>();
    }

    public static TimeDataStorage getInstance() {
        return INSTANCE;
    }

    public Map<KernelInfo, Map<Long, Map<Long, Long>>> getFullDataByKernel() {
        return timeDataA;
    }

    public Map<Long, Map<Long, Map<KernelInfo, Long>>> getFullDataBySize() {
        return timeDataB;
    }

    public Map<Long, Map<Long, Long>> getTimeData(final KernelInfo kernel) {
        return timeDataA.get(kernel);
    }

    public void storeTime(final KernelInfo kernel, final long workSizeF, final long workSizeD, final long time) {
        // Time data per kernel
        Map<Long, Map<Long, Long>> mA = timeDataA.get(kernel);
        if (mA == null) {
            mA = new TreeMap<>();
            timeDataA.put(kernel, mA);
        }
        Map<Long, Long> mA2 = mA.get(workSizeF);
        if (mA2 == null) {
            mA2 = new TreeMap<>();
            mA.put(workSizeF, mA2);
        }
        mA2.put(workSizeD, time);
        // Time data per work size
        Map<Long, Map<KernelInfo, Long>> mB = timeDataB.get(workSizeF);
        if (mB == null) {
            mB = new TreeMap<>();
            timeDataB.put(workSizeF, mB);
        }
        Map<KernelInfo, Long> mB2 = mB.get(workSizeD);
        if (mB2 == null) {
            mB2 = new HashMap<>();
            mB.put(workSizeD, mB2);
        }
        mB2.put(kernel, time);
    }

    @SuppressWarnings("unchecked")
    public boolean loadTimeDataFromFile() {
        boolean result = false;
        try (final ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE_STORE_A))) {
            timeDataA = (Map<KernelInfo, Map<Long, Map<Long, Long>>>) in.readObject();
            result = true;
        } catch (IOException | ClassNotFoundException ex) {
            Logger.warn(ex, "Error reading time data.");
        }
        try (final ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE_STORE_B))) {
            timeDataB = (Map<Long, Map<Long, Map<KernelInfo, Long>>>) in.readObject();
            result = true;
        } catch (IOException | ClassNotFoundException ex) {
            Logger.warn(ex, "Error reading time data.");
        }
        return result;
    }

    public void storeTimeDataToFile() {
        try (final ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE_STORE_A))) {
            out.writeObject(timeDataA);
        } catch (IOException ex) {
            Logger.warn(ex, "Error storing time data.");
        }
        try (final ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE_STORE_B))) {
            out.writeObject(timeDataB);
        } catch (IOException ex) {
            Logger.warn(ex, "Error storing time data.");
        }
    }

    public void reset() {
        timeDataA = new HashMap<>();
        timeDataB = new TreeMap<>();
    }

}
