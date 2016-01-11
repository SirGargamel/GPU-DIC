/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernel;

import cz.tul.dic.engine.opencl.OpenCLDataPackage;
import cz.tul.dic.engine.opencl.kernel.sources.KernelSourcePreparator;
import cz.tul.dic.engine.opencl.memory.AbstractOpenCLMemoryManager;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLException;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory;
import com.jogamp.opencl.CLProgram;
import com.jogamp.opencl.CLResource;
import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.deformation.DeformationOrder;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.engine.opencl.DeviceManager;
import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.Interpolation;
import cz.tul.dic.debug.Stats;
import cz.tul.pj.journal.Journal;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public abstract class Kernel {

    private static final String CL_MEM_ERROR = "CL_OUT_OF_RESOURCES";
    private static final String KERNEL_EXTENSION = ".cl";
    private static final String KERNEL_REDUCE = "reduce";
    private static final String KERNEL_FIND_POS = "findPos";
    private static final String KERNEL_DIC_NAME = "DIC";
    protected final CLContext context;
    protected final CLCommandQueue queue;
    protected CLKernel kernelDIC, kernelReduce, kernelFindPos;
    protected final WorkSizeManager wsm;
    protected final KernelInfo kernelInfo;
    private final Set<CLResource> clMem;
    private final AbstractOpenCLMemoryManager memManager;

    protected Kernel(final KernelInfo info, final AbstractOpenCLMemoryManager memManager, final WorkSizeManager wsm) {
        this.kernelInfo = info;
        clMem = new HashSet<>();

        this.memManager = memManager;
        this.wsm = wsm;

        queue = DeviceManager.getQueue();
        context = DeviceManager.getContext();
    }

    public static Kernel createInstance(KernelInfo kernelInfo, final AbstractOpenCLMemoryManager memManager, final long deformationCount) {
        final KernelInfo concreteInfo = KernelManager.getBestKernel(kernelInfo, deformationCount);
        final WorkSizeManager wsm = new WorkSizeManager(concreteInfo);

        Kernel result;
        try {
            final KernelInfo.Type kernelType = concreteInfo.getType();
            final Class<?> cls = Class.forName("cz.tul.dic.engine.opencl.kernel.".concat(kernelType.toString()));
            result = (Kernel) cls.getConstructor(KernelInfo.class, AbstractOpenCLMemoryManager.class, WorkSizeManager.class).newInstance(concreteInfo, memManager, wsm);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.warn(ex, "Error instantiating kernel with kernel info [{}], using default kernel.", concreteInfo);
            final KernelInfo defaultInfo = KernelManager.getBestKernel(KernelManager.getBestKernel(false), deformationCount);
            result = new CL1D(defaultInfo, memManager, wsm);
        }

        return result;
    }

    public void prepareKernel(final int subsetSize, final DeformationOrder deg, final boolean usesLimits, final Interpolation interpolation) throws ComputationException {
        try {
            final boolean usesZncc, usesWeight;
            switch (kernelInfo.getCorrelation()) {
                case ZNCC:
                    usesZncc = true;
                    usesWeight = false;
                    break;
                case ZNSSD:
                    usesZncc = false;
                    usesWeight = false;
                    break;
                case WZNSSD:
                    usesZncc = false;
                    usesWeight = true;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type of kernel - " + kernelInfo.getCorrelation());
            }

            final boolean usesImage;
            switch (kernelInfo.getInput()) {
                case IMAGE:
                    usesImage = true;
                    break;
                case ARRAY:
                    usesImage = false;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type of input - " + kernelInfo.getInput());
            }

            final boolean usesMemoryCoalescing;
            switch (kernelInfo.getMemoryCoalescing()) {
                case YES:
                    usesMemoryCoalescing = true;
                    break;
                case NO:
                    usesMemoryCoalescing = false;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type of memory coalescing - " + kernelInfo.getMemoryCoalescing());
            }

            CLProgram program = context.createProgram(
                    KernelSourcePreparator.prepareKernel(
                            subsetSize, deg, usesLimits,
                            is2D(), usesVectorization(),
                            interpolation, usesImage, usesLocalMemory(), usesMemoryCoalescing, subsetsGroupped(), usesZncc, usesWeight)).build();
            clMem.add(program);
            kernelDIC = program.createCLKernel(KERNEL_DIC_NAME);
            clMem.add(kernelDIC);

            try (BufferedReader bin = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(KERNEL_REDUCE.concat(KERNEL_EXTENSION))))) {
                final StringBuilder sb = new StringBuilder();
                while (bin.ready()) {
                    sb.append(bin.readLine());
                    sb.append("\n");
                }
                program = context.createProgram(sb.toString()).build();
                clMem.add(program);
                kernelReduce = program.createCLKernel(KERNEL_REDUCE);
                clMem.add(kernelReduce);
            }

            try (BufferedReader bin = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(KERNEL_FIND_POS.concat(KERNEL_EXTENSION))))) {
                final StringBuilder sb = new StringBuilder();
                while (bin.ready()) {
                    sb.append(bin.readLine());
                    sb.append("\n");
                }
                program = context.createProgram(sb.toString()).build();
                clMem.add(program);
                kernelFindPos = program.createCLKernel(KERNEL_FIND_POS);
                clMem.add(kernelFindPos);
            }
        } catch (IOException ex) {
            throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, ex);
        }
    }

    public List<CorrelationResult> computeFindBest(final ComputationTask task) throws ComputationException {        
        final int subsetCount = task.getSubsets().size();
        final int subsetSize = task.getSubsets().get(0).getSize();

        final List<CorrelationResult> result;
        try {
            memManager.assignData(task, this);
            final OpenCLDataPackage clData = memManager.getData();
            final long maxDeformationCount = memManager.getMaxDeformationCount();

            runKernel(clData,
                    maxDeformationCount,
                    task.getImageA().getWidth(), subsetSize, subsetCount);
            queue.flush();

            if (Stats.getInstance().isGpuDebugEnabled()) {
                final CLBuffer<FloatBuffer> clResults = clData.getResults();
                queue.putReadBuffer(clResults, true);
                final double[] results = readResultBuffer(clResults.getBuffer());
                Stats.getInstance().dumpGpuResults(results, task.getSubsets(), task.getDeformations());
            }

            final CLBuffer<FloatBuffer> clResults = clData.getResults();
            final CLBuffer<FloatBuffer> maxValuesCl = findMax(clResults, subsetCount, (int) maxDeformationCount);
            final int[] positions = findPos(clResults, subsetCount, (int) maxDeformationCount, maxValuesCl);

            result = createResults(readBuffer(maxValuesCl.getBuffer()), positions, task.getDeformations(), task.getOrder(), task.usesLimits());
            return result;
        } catch (CLException ex) {
            if (ex.getCLErrorString().contains(CL_MEM_ERROR)) {
                throw new ComputationException(ComputationExceptionCause.MEMORY_ERROR, ex);
            } else {
                throw ex;
            }
        } finally {
            memManager.unlockData();
        }
    }

    public double[] computeRaw(final ComputationTask task) throws ComputationException {
        if (task.getSubsets().isEmpty()) {
            Journal.addEntry("Empty subsets for raw computation.");
            return new double[0];
        }
        final int subsetCount = task.getSubsets().size();
        final int subsetSize = task.getSubsets().get(0).getSize();

        try {
            memManager.assignData(task, this);
            final OpenCLDataPackage clData = memManager.getData();
            final long maxDeformationCount = memManager.getMaxDeformationCount();

            runKernel(clData,
                    maxDeformationCount,
                    task.getImageA().getWidth(), subsetSize, subsetCount);
            queue.flush();

            final CLBuffer<FloatBuffer> clResults = clData.getResults();
            queue.putReadBuffer(clResults, true);
            return readResultBuffer(clResults.getBuffer());
        } catch (CLException ex) {
            if (ex.getCLErrorString().contains(CL_MEM_ERROR)) {
                throw new ComputationException(ComputationExceptionCause.MEMORY_ERROR, ex);
            } else {
                throw ex;
            }
        } finally {
            memManager.unlockData();
        }
    }

    public abstract void runKernel(
            final OpenCLDataPackage data,
            final long maxDeformationCount, final int imageWidth,
            final int subsetSize, final int subsetCount);

    private CLBuffer<FloatBuffer> findMax(final CLBuffer<FloatBuffer> results, final int subsetCount, final int deformationCount) {
        final int lws0 = getMaxWorkItemSize();
        final CLBuffer<FloatBuffer> maxVal = context.createFloatBuffer(subsetCount, CLMemory.Mem.WRITE_ONLY);

        kernelReduce.rewind();
        kernelReduce.setArg(0, results);
        context.getCL().clSetKernelArg(kernelReduce.ID, 1, (long) lws0 * Float.BYTES, null);
        kernelReduce.setArg(2, maxVal);
        kernelReduce.setArg(3, deformationCount);
        kernelReduce.setArg(4, 0);
        kernelReduce.rewind();

        for (int i = 0; i < subsetCount; i++) {
            kernelReduce.setArg(4, i);
            queue.put1DRangeKernel(kernelReduce, 0, lws0, lws0);
        }
        queue.putReadBuffer(maxVal, true);
        queue.flush();

        return maxVal;
    }

    protected static int getMaxWorkItemSize() {
        return DeviceManager.getDevice().getMaxWorkItemSizes()[0];
    }

    private int[] findPos(final CLBuffer<FloatBuffer> results, final int subsetCount, final int deformationCount, final CLBuffer<FloatBuffer> vals) {
        final int lws0 = getMaxWorkItemSize();
        final CLBuffer<IntBuffer> maxVal = context.createIntBuffer(subsetCount, CLMemory.Mem.WRITE_ONLY);

        kernelFindPos.rewind();
        kernelFindPos.setArg(0, results);
        kernelFindPos.setArg(1, vals);
        kernelFindPos.setArg(2, maxVal);
        kernelFindPos.setArg(3, deformationCount);
        kernelFindPos.setArg(4, 0);
        kernelFindPos.rewind();

        for (int i = 0; i < subsetCount; i++) {
            kernelFindPos.setArg(4, i);
            queue.put1DRangeKernel(kernelFindPos, 0, Kernel.roundUp(lws0, deformationCount), lws0);
        }
        queue.putReadBuffer(maxVal, true);
        final int[] result = readBuffer(maxVal.getBuffer());

        results.release();
        maxVal.release();
        return result;
    }

    private static List<CorrelationResult> createResults(final float[] values, final int[] positions, final List<double[]> deformations, final DeformationOrder order, final boolean usesLimits) {
        if (values.length != positions.length) {
            throw new IllegalArgumentException("Array lengths mismatch.");
        }

        final List<CorrelationResult> result = new ArrayList<>(values.length);

        double[] deformation;
        long[] counts;
        CorrelationResult cr;
        for (int i = 0; i < deformations.size(); i++) {
            deformation = deformations.get(i);
            if (usesLimits) {
                counts = DeformationUtils.generateDeformationCounts(deformation);
                cr = new CorrelationResult(values[i], DeformationUtils.extractDeformationFromLimits(positions[i], deformation, counts));
            } else {
                cr = new CorrelationResult(values[i], DeformationUtils.extractDeformationFromValues(positions[i], deformation, order));
            }
            result.add(cr);
        }

        return result;
    }

    public boolean usesVectorization() {
        return false;
    }

    public boolean usesLocalMemory() {
        return false;
    }

    public boolean is2D() {
        return false;
    }

    public boolean subsetsGroupped() {
        return false;
    }

    public void clearMemory() {
        clearMem(clMem);
    }

    private static void clearMem(final Set<CLResource> mems) {
        for (CLResource mem : mems) {
            if (mem != null && !mem.isReleased()) {
                mem.release();
            }
        }
        mems.clear();
    }

    private static float[] readBuffer(final FloatBuffer buffer) {
        buffer.rewind();
        final float[] result = new float[buffer.remaining()];
        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.get(i);
        }
        buffer.rewind();
        return result;
    }

    private static int[] readBuffer(final IntBuffer buffer) {
        buffer.rewind();
        final int[] result = new int[buffer.remaining()];
        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.get(i);
        }
        buffer.rewind();
        return result;
    }

    private static double[] readResultBuffer(final FloatBuffer resultsBuffer) {
        resultsBuffer.rewind();
        final double[] result = new double[resultsBuffer.remaining()];
        for (int i = 0; i < result.length; i++) {
            result[i] = resultsBuffer.get(i);
        }
        resultsBuffer.rewind();
        return result;
    }

    @Override
    public String toString() {
        return kernelInfo.toString();
    }

    public abstract void stopComputation();

    static long roundUp(long groupSize, long globalSize) {
        long r = globalSize % groupSize;
        long result;
        if (r == 0) {
            result = globalSize;
        } else {
            result = globalSize + groupSize - r;
        }
        return result;
    }

    public KernelInfo getKernelInfo() {
        return kernelInfo;
    }

}
