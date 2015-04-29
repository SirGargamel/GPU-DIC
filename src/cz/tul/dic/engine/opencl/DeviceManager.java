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
import com.jogamp.opencl.util.Filter;
import java.nio.ByteBuffer;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class DeviceManager {

    private static final CLDevice.Type DEVICE_TYPE = CLDevice.Type.GPU;
    private static CLPlatform platform;
    private static CLDevice device;
    private static CLContext context;
    private static CLCommandQueue queue;

    static {
        initContext();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            clearMem();
        }));
    }

    public static void initContext() {
        clearMem();

        @SuppressWarnings("unchecked")
        final CLPlatform tmpP = CLPlatform.getDefault((Filter<CLPlatform>) (CLPlatform i) -> i.getMaxFlopsDevice(CLDevice.Type.GPU) != null && i.listCLDevices(CLDevice.Type.CPU).length == 0);
        if (tmpP == null) {
            platform = CLPlatform.getDefault();
        } else {
            platform = tmpP;
        }

        final CLDevice tmpD = platform.getMaxFlopsDevice(DEVICE_TYPE);
        if (tmpD == null) {
            device = platform.getMaxFlopsDevice();
        } else {
            device = tmpD;
        }
        Logger.debug("Using " + device);

        context = CLContext.create(device);
        context.addCLErrorHandler((String string, ByteBuffer bb, long l) -> {
            Logger.error("CLError - " + string);
        });
        
        queue = device.createCommandQueue(CLCommandQueue.Mode.PROFILING_MODE);
    }

    public static CLContext getContext() {
        return context;
    }

    private static void clearMem() {
        if (context != null) {
            Logger.warn("Reseting context memory.");
            for (CLMemory mem : context.getMemoryObjects()) {
                if (mem != null && !mem.isReleased()) {
                    mem.release();
                }
            }
            if (!context.isReleased()) {
                Logger.warn("Releasing context.");
                context.release();
            }
        }
        if (queue != null && !queue.isReleased()) {
            Logger.warn("Releasing command queue.");
            queue.release();
        }
    }

    public static CLDevice getDevice() {
        return device;
    }

    public static CLCommandQueue getQueue() {
        return queue;
    }

}
