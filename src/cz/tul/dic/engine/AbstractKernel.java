package cz.tul.dic.engine;

import cz.tul.dic.engine.platform.Platform;
import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Interpolation;
import cz.tul.dic.data.deformation.DeformationOrder;
import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.engine.memory.AbstractMemoryManager;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 *
 * @author user
 * @param <T> type of memory manager needed for kernel
 */
public abstract class AbstractKernel<T extends AbstractMemoryManager> {

    protected final T memManager;
    private final Platform platform;

    public AbstractKernel(final Platform platform) {
        this.memManager = (T) platform.getMemoryManager();
        this.platform = platform;
    }

    public static AbstractKernel createInstance(final Platform platform) {
        AbstractKernel result;
        try {
            final KernelInfo.Type kernelType = platform.getPlatformDefinition().getKernelInfo().getType();
            final Class<?> cls = Class.forName(kernelType.getPackageName());
            result = (AbstractKernel) cls.getConstructor(Platform.class).newInstance(platform);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException ex) {            
            throw new UnsupportedOperationException(ex);
        }

        return result;
    }

    public abstract void prepareKernel(final int subsetSize, final DeformationOrder deg, final boolean usesLimits, final Interpolation interpolation) throws ComputationException;

    public abstract List<CorrelationResult> computeFindBest(final ComputationTask task) throws ComputationException;

    public abstract double[] computeRaw(final ComputationTask task) throws ComputationException;

    public abstract void stopComputation();

    public abstract boolean usesGPU();

    public KernelInfo getKernelInfo() {
        return platform.getPlatformDefinition().getKernelInfo();
    }

    public void clearMemory() {
        //memManager.clearMemory();
    }

}
