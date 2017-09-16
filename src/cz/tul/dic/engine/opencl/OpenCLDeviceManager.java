/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl;

import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLDevice.Type;
import com.jogamp.opencl.CLMemory;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.CLProgram;
import cz.tul.dic.engine.DeviceType;
import cz.tul.dic.engine.AbstractDeviceManager;
import cz.tul.pj.journal.Journal;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public final class OpenCLDeviceManager extends AbstractDeviceManager {

    private CLDevice device;
    private CLContext context;
    private CLCommandQueue queue;

    public OpenCLDeviceManager() {
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> clearMemory()
                ));
    }

    @Override
    public void clearMemory() {
        if (context != null) {
            for (CLMemory mem : context.getMemoryObjects()) {
                if (mem != null && !mem.isReleased()) {
                    mem.release();
                }
            }
            for (CLProgram mem : context.getPrograms()) {
                if (mem != null && !mem.isReleased()) {
                    mem.release();
                }
            }
        }
    }

    @Override
    public void prepareDevice(DeviceType deviceType) {
        clearMemory();

        device = findDevice(deviceType);        
        Journal.addEntry("Using new OpenCL device.", "{0}", device);

        context = CLContext.create(device);
        context.addCLErrorHandler((String string, ByteBuffer bb, long l)
                -> {
            Journal.addEntry("CLError - " + string);            
            System.out.println(l + " -- " + string + "; " + bb);
            
        });

        queue = device.createCommandQueue(CLCommandQueue.Mode.PROFILING_MODE);
    }

    private static CLDevice findDevice(final DeviceType deviceType) {
        final List<CLDevice> results = new LinkedList<>();

        for (CLPlatform platform : CLPlatform.listCLPlatforms()) {
            switch (deviceType) {
                case CPU:
                    joinDevicesToList(results, platform.listCLDevices(Type.CPU));
                    break;
                case GPU:
                    joinDevicesToList(results, platform.listCLDevices(Type.GPU));
                    final Iterator<CLDevice> it = results.iterator();
                    String name;
                    while (it.hasNext()) {
                        name = it.next().getName().toLowerCase();
                        if (name.contains("intel") || name.contains("simulator")) {
                            it.remove();    // Intel graphics are iGPU, skip simulators
                        }
                    }
                    break;
                case iGPU:
                    if (platform.getName().toLowerCase().contains("intel")) {
                        joinDevicesToList(results, platform.listCLDevices(Type.GPU));
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type of device - " + deviceType);
            }
        }

        Collections.sort(results, (CLDevice o1, CLDevice o2) -> {
            final int performance1 = o1.getMaxComputeUnits() * o1.getMaxClockFrequency();
            final int performance2 = o2.getMaxComputeUnits() * o2.getMaxClockFrequency();
            return Integer.compare(performance2, performance1);
        });
        if (results.size() > 1) {
            Logger.warn("Found multiple devices for device type " + deviceType + ": " + results.toString() + "using " + results.get(0).toString());
        }
        return results.get(0);
    }

    private static void joinDevicesToList(final List<CLDevice> deviceList, final CLDevice[] newDevices) {
        for (CLDevice newDevice : newDevices) {
            String nameNew = newDevice.getName();
            boolean add = true;
            for (CLDevice device : deviceList) {
                if (device.getName().equals(nameNew)) {
                    add = false;
                }
            }
            if (add) {
                deviceList.add(newDevice);
            }
        }
    }

    public CLContext getContext() {
        return context;
    }

    public CLDevice getDevice() {
        return device;
    }

    public CLCommandQueue getQueue() {
        return queue;
    }

}
