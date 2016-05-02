package cz.tul.dic.engine.kernel;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Interpolation;
import cz.tul.dic.data.deformation.DeformationOrder;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.engine.memory.MemoryManager;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.pmw.tinylog.Logger;

/**
 *
 * @author user
 * @param <T> type of memory manager needed for kernel
 */
public abstract class AbstractKernel<T extends MemoryManager> {

    protected final WorkSizeManager wsm;
    protected final T memManager;
    private final KernelInfo kernelInfo;

    public AbstractKernel(final KernelInfo info, final T memManager, final WorkSizeManager wsm) {
        this.memManager = memManager;
        this.wsm = wsm;
        this.kernelInfo = info;
    }

    public static AbstractKernel createInstance(KernelInfo kernelInfo, final long deformationCount) {
        final KernelInfo concreteInfo = KernelManager.getBestKernel(kernelInfo, deformationCount);
        final WorkSizeManager wsm = new WorkSizeManager(concreteInfo);

        AbstractKernel result;
        try {
            final KernelInfo.Type kernelType = concreteInfo.getType();
            final Class<?> cls = Class.forName(kernelType.getPackageName());
            result = (AbstractKernel) cls.getConstructor(KernelInfo.class, MemoryManager.class, WorkSizeManager.class).newInstance(concreteInfo, MemoryManager.getInstance(kernelInfo), wsm);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.warn(ex, "Error instantiating kernel with kernel info [{}], using default Java kernel.", concreteInfo);
            final KernelInfo defaultInfo = new KernelInfo(KernelInfo.Type.JavaKernel, KernelInfo.Input.ARRAY, KernelInfo.Correlation.NO_WEIGHTS, KernelInfo.MemoryCoalescing.NO, KernelInfo.UseLimits.NO);
            result = new JavaKernel(defaultInfo, MemoryManager.getInstance(defaultInfo), wsm);
        }

        return result;
    }

    public abstract void prepareKernel(final int subsetSize, final DeformationOrder deg, final boolean usesLimits, final Interpolation interpolation) throws ComputationException;

    public List<CorrelationResult> computeFindBest(final ComputationTask task) throws ComputationException {
        final long startTime = System.currentTimeMillis();

        final List<CorrelationResult> result = computeFindBestInner(task);

        computeAndSoreExecutionTime(startTime, task);

        return result;
    }
    
    private void computeAndSoreExecutionTime(final long startTime, final ComputationTask task) {
        final List<AbstractSubset> subsets = task.getSubsets();
        final List<double[]> deformations = task.getDeformations();
        final long deformationCount;
        if (kernelInfo.getUseLimits() == KernelInfo.UseLimits.YES) {
            final long[] counts = DeformationUtils.generateDeformationCounts(deformations.get(0));
            deformationCount = counts[counts.length - 1];
        } else {
            deformationCount = deformations.get(0).length / DeformationUtils.getDeformationCoeffCount(task.getOrder());
        }
        TimeDataStorage.getInstance().storeTime(kernelInfo, subsets.size(), deformationCount, System.currentTimeMillis() - startTime);
    }        

    public abstract List<CorrelationResult> computeFindBestInner(final ComputationTask task) throws ComputationException;

    public double[] computeRaw(final ComputationTask task) throws ComputationException {
        final long startTime = System.currentTimeMillis();

        final double[] result = computeRawInner(task);

        computeAndSoreExecutionTime(startTime, task);

        return result;
    }
    
    public abstract double[] computeRawInner(final ComputationTask task) throws ComputationException;

    public abstract void stopComputation();

    public abstract boolean usesGPU();

    public KernelInfo getKernelInfo() {
        return kernelInfo;
    }

    public void clearMemory() {
        //memManager.clearMemory();
    }

}
