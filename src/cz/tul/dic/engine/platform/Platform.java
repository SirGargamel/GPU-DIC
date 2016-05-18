package cz.tul.dic.engine.platform;

import cz.tul.dic.data.task.splitter.AbstractTaskSplitter;
import cz.tul.dic.engine.AbstractDeviceManager;
import cz.tul.dic.engine.memory.AbstractMemoryManager;

/**
 *
 * @author user
 */
public class Platform {
    
    private final PlatformDefinition platformDefinition;
    private final AbstractMemoryManager memoryManager;
    private final AbstractDeviceManager deviceManager;
    private final AbstractTaskSplitter taskSplitter;

    public Platform(PlatformDefinition platformDefinition, AbstractMemoryManager memoryManager, AbstractDeviceManager deviceManager, AbstractTaskSplitter taskSplitter) {
        this.platformDefinition = platformDefinition;
        this.memoryManager = memoryManager;
        this.deviceManager = deviceManager;
        this.taskSplitter = taskSplitter;
    }    

    public AbstractMemoryManager getMemoryManager() {
        return memoryManager;
    }

    public AbstractDeviceManager getDeviceManager() {
        return deviceManager;
    }

    public AbstractTaskSplitter getTaskSplitter() {
        return taskSplitter;
    }

    public PlatformDefinition getPlatformDefinition() {
        return platformDefinition;
    }

    public void release() {
        memoryManager.clearMemory();
        deviceManager.clearMemory();
    }
    
    @Override
    public String toString() {
        return platformDefinition.toString();
    }
}
