package cz.tul.dic.engine.memory;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.engine.AbstractKernel;
import cz.tul.dic.engine.platform.Platform;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author user
 */
public abstract class AbstractMemoryManager {
    
    protected final ReentrantLock lock;
    private ComputationTask task;    

    public AbstractMemoryManager() {
        lock = new ReentrantLock();
    }
    
    public abstract void setPlatform(final Platform platform);
    
    public abstract void assignTask(final TaskContainer task);

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

    public abstract void clearMemory();

    public enum Type {
        STATIC,
        DYNAMIC,
        PREFETCH,
        BASIC
    }

}
