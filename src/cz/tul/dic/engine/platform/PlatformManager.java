package cz.tul.dic.engine.platform;

import cz.tul.dic.data.task.splitter.AbstractTaskSplitter;
import cz.tul.dic.data.task.splitter.NoSplit;
import cz.tul.dic.data.task.splitter.OpenCLSplitter;
import cz.tul.dic.engine.AbstractDeviceManager;
import cz.tul.dic.engine.DeviceType;
import cz.tul.dic.engine.KernelInfo;
import cz.tul.dic.engine.KernelPerformanceManager;
import cz.tul.dic.engine.java.JavaDeviceManager;
import cz.tul.dic.engine.memory.AbstractMemoryManager;
import cz.tul.dic.engine.memory.BasicMemoryManager;
import cz.tul.dic.engine.memory.DynamicOpenCLMemoryManager;
import cz.tul.dic.engine.opencl.OpenCLDeviceManager;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author user
 */
public class PlatformManager {

    private static final PlatformManager INSTANCE;
    private final Map<PlatformType, AbstractDeviceManager> deviceManagers;
    private final Map<PlatformType, AbstractMemoryManager> memoryManagers;
    private final Map<PlatformType, AbstractTaskSplitter> taskSplitters;

    static {
        INSTANCE = new PlatformManager();
    }

    private PlatformManager() {
        deviceManagers = new HashMap<>(2);
        deviceManagers.put(PlatformType.JAVA, new JavaDeviceManager());
        deviceManagers.put(PlatformType.OPENCL, new OpenCLDeviceManager());

        memoryManagers = new HashMap<>(2);
        memoryManagers.put(PlatformType.JAVA, new BasicMemoryManager());
        memoryManagers.put(PlatformType.OPENCL, new DynamicOpenCLMemoryManager());

        taskSplitters = new HashMap<>(2);
        taskSplitters.put(PlatformType.JAVA, new NoSplit());
        taskSplitters.put(PlatformType.OPENCL, new OpenCLSplitter());
    }

    public static PlatformManager getInstance() {
        return INSTANCE;
    }

    public Platform initPlatform() {
        final PlatformDefinition bestPlatform = KernelPerformanceManager.getInstance().getBestPlatform();
        final AbstractDeviceManager deviceManager = deviceManagers.get(bestPlatform.getPlatform());
        deviceManager.prepareDevice(bestPlatform.getDevice());
        final AbstractMemoryManager memoryManager = memoryManagers.get(bestPlatform.getPlatform());
        final AbstractTaskSplitter taskSplitter = taskSplitters.get(bestPlatform.getPlatform());
        taskSplitter.assignDeviceManager(deviceManager);
        final Platform result = new Platform(bestPlatform, memoryManager, deviceManager, taskSplitter);
        memoryManager.setPlatform(result);
        return result;
    }

    public Platform initPlatform(final PlatformType platform, final DeviceType device, final KernelInfo kernelInfo) {
        final AbstractMemoryManager memoryManager = memoryManagers.get(platform);
        final AbstractDeviceManager deviceManager = deviceManagers.get(platform);
        deviceManager.prepareDevice(device);
        final AbstractTaskSplitter taskSplitter = taskSplitters.get(platform);
        taskSplitter.assignDeviceManager(deviceManager);
        final Platform result = new Platform(new PlatformDefinition(platform, device, kernelInfo), memoryManager, deviceManager, taskSplitter);
        memoryManager.setPlatform(result);
        return result;
    }

}
