/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl;

import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLMemory;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.CLProgram;
import cz.tul.pj.journal.Journal;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Petr Jecmen
 */
public final class DeviceManager {

    private static CLDevice DEVICE;
    private static CLContext CONTEXT;
    private static CLCommandQueue QUEUE;

    static {
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> clearMemory()
                ));
    }

    public static List<CLDevice> listAllDevices() {
        final List<CLDevice> result = new LinkedList<>();
        for (CLPlatform plf : CLPlatform.listCLPlatforms()) {
            result.addAll(Arrays.asList(plf.listCLDevices()));
        }
        return result;
    }

    private DeviceManager() {
    }

    public static void initContext(final CLDevice device) {
        clearMemory();

        DEVICE = device;
        Journal.addEntry("Using new OpenCL device.", "{0}", device);

        CONTEXT = CLContext.create(device);
        CONTEXT.addCLErrorHandler((String string, ByteBuffer bb, long l)
                -> Journal.addEntry("CLError - " + string)
        );

        QUEUE = device.createCommandQueue(CLCommandQueue.Mode.PROFILING_MODE);
    }

    public static CLContext getContext() {
        return CONTEXT;
    }

    public static void clearMemory() {
        if (CONTEXT != null) {
            for (CLMemory mem : CONTEXT.getMemoryObjects()) {
                if (mem != null && !mem.isReleased()) {
                    mem.release();
                }
            }
            for (CLProgram mem : CONTEXT.getPrograms()) {
                if (mem != null && !mem.isReleased()) {
                    mem.release();
                }
            }
        }
    }

    public static CLDevice getDevice() {
        return DEVICE;
    }

    public static CLCommandQueue getQueue() {
        return QUEUE;
    }

}
