package cz.tul.dic.engine.memory;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.AppSettings;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.engine.kernel.AbstractKernel;
import cz.tul.dic.engine.kernel.KernelInfo;
import cz.tul.dic.engine.opencl.DeviceManager;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author user
 */
public class MemoryManager {

    private static final Map<MemoryManager.Type, MemoryManager> INSTANCES;
    protected final ReentrantLock lock;
    private ComputationTask task;

    static {
        DeviceManager.getContext();
        DeviceManager.clearMemory();

        INSTANCES = new EnumMap<>(AbstractOpenCLMemoryManager.Type.class);
        INSTANCES.put(MemoryManager.Type.STATIC, new StaticMemoryManager());
        INSTANCES.put(MemoryManager.Type.DYNAMIC, new DynamicMemoryManager());
        INSTANCES.put(MemoryManager.Type.PREFETCH, new StaticMemoryManager());
        INSTANCES.put(MemoryManager.Type.BASIC, new MemoryManager());
    }

    public static MemoryManager getInstance(final KernelInfo kernelInfo) {
        if (kernelInfo.getType() == KernelInfo.Type.JavaKernel) {
            return INSTANCES.get(MemoryManager.Type.BASIC);
        } else {
            return INSTANCES.get(AppSettings.getInstance().getMemManagerType());
        }
    }

    public static void assignTaskForInit(final TaskContainer task) {
        final AbstractOpenCLMemoryManager prefetch = (AbstractOpenCLMemoryManager) INSTANCES.get(MemoryManager.Type.PREFETCH);
        prefetch.assignTask(task);
    }

    public MemoryManager() {
        lock = new ReentrantLock();
    }

    public void assignData(final ComputationTask task, final AbstractKernel kernel) throws ComputationException {
        this.task = task;
        lock.lock();
    }

    public void unlockData() {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        } else {
            System.err.println("Illegal unlock request...");
        }
    }

    public ComputationTask getTask() {
        return task;
    }

    public void clearMemory() {
    }

    public enum Type {
        STATIC,
        DYNAMIC,
        PREFETCH,
        BASIC
    }

}
