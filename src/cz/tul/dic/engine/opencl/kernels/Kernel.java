package cz.tul.dic.engine.opencl.kernels;

import cz.tul.dic.engine.opencl.OpenCLMemoryManager;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory;
import com.jogamp.opencl.CLProgram;
import com.jogamp.opencl.CLResource;
import com.sun.prism.impl.BufferUtil;
import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.debug.Stats;
import cz.tul.dic.engine.opencl.DeviceManager;
import cz.tul.dic.engine.opencl.solvers.CorrelationResult;
import cz.tul.dic.engine.opencl.WorkSizeManager;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
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

    public static Kernel createKernel(final KernelType kernelType, final OpenCLMemoryManager memManager) {
        try {
            final Class<?> cls = Class.forName("cz.tul.dic.engine.opencl.kernels.".concat(kernelType.toString()));
            return (Kernel) cls.getConstructor(OpenCLMemoryManager.class).newInstance(memManager);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.warn("Error instantiating class {0}, using default kernel.", kernelType);
            Logger.error(ex);
            return new CL1D_I_V_LL_MC_D(memManager);
        }
    }

    private static final String KERNEL_REDUCE = "reduce";
    private static final String KERNEL_FIND_POS = "findPos";
    private final String kernelName;
    protected final CLContext context;
    protected CLKernel kernelDIC, kernelReduce, kernelFindPos;
    protected final CLDevice device;
    protected final CLCommandQueue queue;
    private final Set<CLResource> clMem;
    private final OpenCLMemoryManager memManager;

    protected Kernel(String kernelName, final OpenCLMemoryManager memManager) {
        this.kernelName = kernelName;
        clMem = new HashSet<>();
        this.memManager = memManager;

        device = memManager.getDevice();
        queue = memManager.getQueue();
        context = memManager.getContext();
    }

    public void prepareKernel(final int facetSize, final DeformationDegree deg, final Interpolation interpolation) throws ComputationException {
        try {
            CLProgram program = context.createProgram(KernelSourcePreparator.prepareKernel(kernelName, facetSize, deg, usesVectorization(), interpolation, usesImage())).build();
            clMem.add(program);
            kernelDIC = program.createCLKernel(kernelName);
            clMem.add(kernelDIC);

            try (BufferedReader bin = new BufferedReader(new InputStreamReader(WorkSizeManager.class.getResourceAsStream(KERNEL_REDUCE.concat(KernelSourcePreparator.KERNEL_EXTENSION))))) {
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

            try (BufferedReader bin = new BufferedReader(new InputStreamReader(WorkSizeManager.class.getResourceAsStream(KERNEL_FIND_POS.concat(KernelSourcePreparator.KERNEL_EXTENSION))))) {
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
            throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, ex.getLocalizedMessage());
        }
    }

    public List<CorrelationResult> compute(Image imageA, Image imageB, List<Facet> facets, List<double[]> deformationLimits) throws ComputationException {
        final int facetCount = facets.size();
        if (facets.isEmpty()) {
            Logger.warn("Empty facets for computation.");
            return new ArrayList<>(0);
        }
        final int facetSize = facets.get(0).getSize();

        memManager.assignData(imageA, imageB, facets, deformationLimits, this);
        final CLBuffer<FloatBuffer> clResults = memManager.getClResults();
        final int maxDeformationCount = memManager.getMaxDeformationCount();

        runKernel(memManager.getClImageA(), memManager.getClImageB(),
                memManager.getClFacetData(), memManager.getClFacetCenters(),
                memManager.getClDeformationLimits(), memManager.getClDefStepCount(),
                clResults,
                maxDeformationCount,
                imageA.getWidth(), facetSize, facetCount);

        if (Stats.isGpuDebugEnabled()) {
            queue.putReadBuffer(clResults, true);
            final float[] results = readBuffer(clResults.getBuffer());
            Stats.dumpGpuResults(results, facets, deformationLimits);
        }

        final CLBuffer<FloatBuffer> maxValuesCl = findMax(clResults, facetCount, (int) maxDeformationCount);
        final int[] positions = findPos(clResults, facetCount, (int) maxDeformationCount, maxValuesCl);

        return createResults(readBuffer(maxValuesCl.getBuffer()), positions, deformationLimits);
    }

    abstract void runKernel(final CLMemory<IntBuffer> imgA, final CLMemory<IntBuffer> imgB,
            final CLBuffer<IntBuffer> facetData,
            final CLBuffer<FloatBuffer> facetCenters,
            final CLBuffer<FloatBuffer> deformationLimits, final CLBuffer<IntBuffer> defStepCounts,
            final CLBuffer<FloatBuffer> results,
            final int maxDeformationCount, final int imageWidth,
            final int facetSize, final int facetCount);

    private CLBuffer<FloatBuffer> findMax(final CLBuffer<FloatBuffer> results, final int facetCount, final int deformationCount) {
        final int lws0 = device.getMaxWorkItemSizes()[0];
        final CLBuffer<FloatBuffer> maxVal = context.createFloatBuffer(facetCount, CLMemory.Mem.WRITE_ONLY);

        kernelReduce.rewind();
        kernelReduce.setArg(0, results);
        context.getCL().clSetKernelArg(kernelReduce.ID, 1, lws0 * BufferUtil.SIZEOF_FLOAT, null);
        kernelReduce.setArg(2, maxVal);
        kernelReduce.setArg(3, deformationCount);
        kernelReduce.setArg(4, 0);
        kernelReduce.rewind();

        for (int i = 0; i < facetCount; i++) {
            kernelReduce.setArg(4, i);
            queue.put1DRangeKernel(kernelReduce, 0, lws0, lws0);
        }
        queue.putReadBuffer(maxVal, true);

        return maxVal;
    }

    private int[] findPos(final CLBuffer<FloatBuffer> results, final int facetCount, final int deformationCount, final CLBuffer<FloatBuffer> vals) {
        final int lws0 = device.getMaxWorkItemSizes()[0];
        final CLBuffer<IntBuffer> maxVal = context.createIntBuffer(facetCount, CLMemory.Mem.WRITE_ONLY);

        kernelFindPos.rewind();
        kernelFindPos.setArg(0, results);
        kernelFindPos.setArg(1, vals);
        kernelFindPos.setArg(2, maxVal);
        kernelFindPos.setArg(3, deformationCount);
        kernelFindPos.setArg(4, 0);
        kernelFindPos.rewind();

        for (int i = 0; i < facetCount; i++) {
            kernelFindPos.setArg(4, i);
            queue.put1DRangeKernel(kernelFindPos, 0, Kernel.roundUp(lws0, deformationCount), lws0);
        }
        queue.putReadBuffer(maxVal, true);
        final int[] result = readBuffer(maxVal.getBuffer());

        results.release();
        maxVal.release();
        return result;
    }

    private List<CorrelationResult> createResults(final float[] values, final int[] positions, final List<double[]> deformationLimits) {
        if (values.length != positions.length) {
            throw new IllegalArgumentException("Array lengths mismatch.");
        }

        final List<CorrelationResult> result = new ArrayList<>(values.length);

        double[] limits;
        int[] counts;
        for (int f = 0; f < deformationLimits.size(); f++) {

            limits = deformationLimits.get(f);
            counts = DeformationUtils.generateDeformationCounts(limits);

            result.add(new CorrelationResult(values[f], DeformationUtils.extractDeformation(positions[f], limits, counts)));
        }

        return result;
    }

    public boolean usesMemoryCoalescing() {
        return false;
    }

    public boolean usesVectorization() {
        return false;
    }

    public boolean usesImage() {
        return false;
    }

    public void finishComputation() {
        if (queue != null && !queue.isReleased()) {
            queue.finish();
        }
        clearMem(clMem);
        DeviceManager.clearContext();
    }

    private void clearMem(final Set<CLResource> mems) {
        for (CLResource mem : mems) {
            if (mem != null && !mem.isReleased()) {
                mem.release();
            }
        }
        mems.clear();
    }

    private float[] readBuffer(final FloatBuffer buffer) {
        buffer.rewind();
        final float[] result = new float[buffer.remaining()];
        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.get(i);
        }
        buffer.rewind();
        return result;
    }

    private int[] readBuffer(final IntBuffer buffer) {
        buffer.rewind();
        final int[] result = new int[buffer.remaining()];
        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.get(i);
        }
        buffer.rewind();
        return result;
    }

    @Override
    public String toString() {
        return kernelName;
    }

    public abstract void stop();

    static int roundUp(int groupSize, int globalSize) {
        int r = globalSize % groupSize;
        int result;
        if (r == 0) {
            result = globalSize;
        } else {
            result = globalSize + groupSize - r;
        }
        return result;
    }

}
