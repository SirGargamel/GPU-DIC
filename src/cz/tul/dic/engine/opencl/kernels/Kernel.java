package cz.tul.dic.engine.opencl.kernels;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.CLMemory;
import com.jogamp.opencl.CLProgram;
import com.jogamp.opencl.CLResource;
import com.sun.prism.impl.BufferUtil;
import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.debug.DebugControl;
import cz.tul.dic.debug.Stats;
import cz.tul.dic.engine.CorrelationResult;
import cz.tul.dic.engine.opencl.KernelSourcePreparator;
import cz.tul.dic.engine.opencl.WorkSizeManager;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public abstract class Kernel {

    public static Kernel createKernel(final KernelType kernelType) {
        try {
            final Class<?> cls = Class.forName("cz.tul.dic.engine.opencl.kernels.".concat(kernelType.toString()));
            return (Kernel) cls.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            Logger.warn("Error instantiating class {0}, using default kernel.", kernelType);
            Logger.error(ex);
            return new CL1D_I_V_LL_MC_D();
        }
    }

    private static final CLImageFormat IMAGE_FORMAT = new CLImageFormat(CLImageFormat.ChannelOrder.RGBA, CLImageFormat.ChannelType.UNSIGNED_INT8);
    private static final String KERNEL_REDUCE = "reduce";
    private static final String KERNEL_FIND_POS = "findPos";
    private final String kernelName;
    protected CLContext context;
    protected CLKernel kernelDIC, kernelReduce, kernelFindPos;
    protected CLDevice device;
    protected CLCommandQueue queue;
    private final Set<CLResource> clGlobalMem, clRoundMem;

    protected Kernel(String kernelName) {
        this.kernelName = kernelName;
        clGlobalMem = new HashSet<>();
        clRoundMem = new HashSet<>();
    }

    public void prepareKernel(final CLContext context, final CLDevice device, final int facetSize, final DeformationDegree deg, final Interpolation interpolation) throws ComputationException {
        try {
            this.context = context;
            this.device = device;

            CLProgram program = context.createProgram(KernelSourcePreparator.prepareKernel(kernelName, facetSize, deg, usesVectorization(), interpolation, usesImage())).build();
            clGlobalMem.add(program);
            kernelDIC = program.createCLKernel(kernelName);
            clGlobalMem.add(kernelDIC);
            if (isDriven()) {
                queue = device.createCommandQueue(CLCommandQueue.Mode.PROFILING_MODE);
            } else {
                queue = device.createCommandQueue();
            }
            clGlobalMem.add(queue);

            try (BufferedReader bin = new BufferedReader(new InputStreamReader(WorkSizeManager.class.getResourceAsStream(KERNEL_REDUCE.concat(KernelSourcePreparator.KERNEL_EXTENSION))))) {
                final StringBuilder sb = new StringBuilder();
                while (bin.ready()) {
                    sb.append(bin.readLine());
                    sb.append("\n");
                }
                program = context.createProgram(sb.toString()).build();
                clGlobalMem.add(program);
                kernelReduce = program.createCLKernel(KERNEL_REDUCE);
                clGlobalMem.add(kernelReduce);
            }

            try (BufferedReader bin = new BufferedReader(new InputStreamReader(WorkSizeManager.class.getResourceAsStream(KERNEL_FIND_POS.concat(KernelSourcePreparator.KERNEL_EXTENSION))))) {
                final StringBuilder sb = new StringBuilder();
                while (bin.ready()) {
                    sb.append(bin.readLine());
                    sb.append("\n");
                }
                program = context.createProgram(sb.toString()).build();
                clGlobalMem.add(program);
                kernelFindPos = program.createCLKernel(KERNEL_FIND_POS);
                clGlobalMem.add(kernelFindPos);
            }

        } catch (IOException ex) {
            throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, ex.getLocalizedMessage());
        }
    }

    public List<CorrelationResult> compute(Image imageA, Image imageB, List<Facet> facets, List<double[]> deformationLimits, int deformationLength) throws ComputationException {
        final CLMemory<IntBuffer> clImageA, clImageB;
        final CLBuffer<IntBuffer> clFacetData, clDefStepCount;
        final CLBuffer<FloatBuffer> clFacetCenters;
        final CLBuffer<FloatBuffer> clDeformationLimits, clResults;
        // prepare data      
        if (usesImage()) {
            clImageA = generateImage(imageA);
            queue.putWriteImage((CLImage2d<IntBuffer>) clImageA, false);
            clImageB = generateImage(imageB);
            queue.putWriteImage((CLImage2d<IntBuffer>) clImageB, false);
        } else {
            clImageA = generateImageArray(imageA);
            queue.putWriteBuffer((CLBuffer<IntBuffer>) clImageA, false);
            clImageB = generateImageArray(imageB);
            queue.putWriteBuffer((CLBuffer<IntBuffer>) clImageB, false);
        }

        final int facetCount = facets.size();
        if (facets.isEmpty()) {
            Logger.warn("Empty facets for computation.");
            return new ArrayList<>(0);
        }

        final int facetSize = facets.get(0).getSize();

        clFacetData = generateFacetData(facets, facetSize, usesMemoryCoalescing());
        queue.putWriteBuffer(clFacetData, false);

        clFacetCenters = generateFacetCenters(facets);
        queue.putWriteBuffer(clFacetCenters, false);

        clDeformationLimits = generateDeformationLimits(deformationLimits);
        queue.putWriteBuffer(clDeformationLimits, false);
        clDefStepCount = generateDeformationStepCounts(deformationLimits);
        queue.putWriteBuffer(clDefStepCount, true);
        final long deformationCount = (int) DeformationUtils.calculateDeformationCount(deformationLimits.get(0));

        Logger.trace("Computing task " + facetCount + " facets, " + deformationCount + " deformations.");
        final long size = facetCount * deformationCount;
        if (size <= 0 || size >= Integer.MAX_VALUE) {
            throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, "Illegal size of resulting array - " + size);
        }
        clResults = context.createFloatBuffer((int) size, CLMemory.Mem.READ_WRITE);

        clRoundMem.add(clResults);

        runKernel(clImageA, clImageB,
                clFacetData, clFacetCenters,
                clDeformationLimits, clDefStepCount, clResults, (int) deformationCount, imageA.getWidth(), facetSize, facetCount);

        if (DebugControl.isGpuDebugEnabled()) {
            queue.putReadBuffer(clResults, true);
            final float[] results = readBuffer(clResults.getBuffer());
            Stats.dumpGpuResults(results, facets, deformationLimits);

        }

        final CLBuffer<FloatBuffer> maxValuesCl = findMax(clResults, facetCount, (int) deformationCount);
        final int[] positions = findPos(clResults, facetCount, (int) deformationCount, maxValuesCl);

        return createResults(readBuffer(maxValuesCl.getBuffer()), positions, deformationLimits);
    }

    abstract void runKernel(final CLMemory<IntBuffer> imgA, final CLMemory<IntBuffer> imgB,
            final CLBuffer<IntBuffer> facetData,
            final CLBuffer<FloatBuffer> facetCenters,
            final CLBuffer<FloatBuffer> deformationLimits, final CLBuffer<IntBuffer> defStepCounts,
            final CLBuffer<FloatBuffer> results,
            final int deformationCount, final int imageWidth,
            final int facetSize, final int facetCount);

    private CLBuffer<FloatBuffer> findMax(final CLBuffer<FloatBuffer> results, final int facetCount, final int deformationCount) {
        final int lws0 = device.getMaxWorkItemSizes()[0];
        final CLBuffer<FloatBuffer> maxVal = context.createFloatBuffer(facetCount, CLMemory.Mem.WRITE_ONLY);
        clRoundMem.add(maxVal);

        kernelReduce.rewind();
        kernelReduce.setArg(0, results);
        device.getContext().getCL().clSetKernelArg(kernelReduce.ID, 1, lws0 * BufferUtil.SIZEOF_FLOAT, null);
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
        clRoundMem.add(maxVal);

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

    boolean usesMemoryCoalescing() {
        return false;
    }

    boolean usesVectorization() {
        return false;
    }

    boolean usesImage() {
        return false;
    }

    boolean isDriven() {
        return true;
    }

    public void finishRound() {
        clearMem(clRoundMem);
    }

    public void finishComputation() {
        queue.finish();
        clearMem(clGlobalMem);
    }

    private void clearMem(final Set<CLResource> mems) {
        // round memory cleanup
        for (CLResource mem : mems) {
            if (!mem.isReleased()) {
                mem.release();
            }
        }
        mems.clear();
    }

    private CLImage2d<IntBuffer> generateImage(final Image image) {
        final CLImage2d<IntBuffer> result = context.createImage2d(
                Buffers.newDirectIntBuffer(image.toBWArray()),
                image.getWidth(), image.getHeight(),
                IMAGE_FORMAT, CLMemory.Mem.READ_ONLY);
        clRoundMem.add(result);
        return result;
    }

    private CLBuffer<IntBuffer> generateImageArray(final Image image) {
        final int[] data = image.toBWArray();
        final CLBuffer<IntBuffer> result = context.createIntBuffer(data.length, CLMemory.Mem.READ_ONLY);
        final IntBuffer buffer = result.getBuffer();
        for (int i : data) {
            buffer.put(i);
        }
        buffer.rewind();
        clRoundMem.add(result);
        return result;
    }

    private CLBuffer<IntBuffer> generateFacetData(final List<Facet> facets, final int facetSize, final boolean useMemoryCoalescing) {
        final int facetArea = facetSize * facetSize;
        final int dataSize = facetArea * Coordinates.DIMENSION;
        final int[] completeData = new int[facets.size() * dataSize];

        int pointer = 0;
        int[] facetData;
        for (Facet f : facets) {
            facetData = f.getData();
            System.arraycopy(facetData, 0, completeData, pointer, dataSize);
            pointer += dataSize;
        }

        final CLBuffer<IntBuffer> result = context.createIntBuffer(completeData.length, CLMemory.Mem.READ_ONLY);
        final IntBuffer resultBuffer = result.getBuffer();
        if (useMemoryCoalescing) {
            int index;
            for (int i = 0; i < facetArea; i++) {
                for (int f = 0; f < facets.size(); f++) {
                    index = f * dataSize + 2 * i;
                    resultBuffer.put(completeData[index]);
                    resultBuffer.put(completeData[index + 1]);
                }
            }
        } else {
            for (int i : completeData) {
                resultBuffer.put(i);
            }
        }
        resultBuffer.rewind();

        clRoundMem.add(result);
        return result;
    }

    private CLBuffer<FloatBuffer> generateFacetCenters(final List<Facet> facets) {
        final int dataSize = Coordinates.DIMENSION;
        final float[] data = new float[facets.size() * dataSize];

        int pointer = 0;
        double[] centerData;
        for (Facet f : facets) {
            centerData = f.getCenter();
            for (int i = 0; i < dataSize; i++) {
                data[pointer + i] = (float) centerData[i];
            }
            pointer += dataSize;
        }

        final CLBuffer<FloatBuffer> result = context.createFloatBuffer(data.length, CLMemory.Mem.READ_ONLY);
        final FloatBuffer buffer = result.getBuffer();
        for (float f : data) {
            buffer.put(f);
        }
        buffer.rewind();

        clRoundMem.add(result);
        return result;
    }

    private CLBuffer<FloatBuffer> generateDeformationLimits(final List<double[]> deformationLimits) {
        final CLBuffer<FloatBuffer> result = context.createFloatBuffer(deformationLimits.size() * deformationLimits.get(0).length, CLMemory.Mem.READ_ONLY);
        final FloatBuffer buffer = result.getBuffer();
        for (double[] dA : deformationLimits) {
            for (double d : dA) {
                buffer.put((float) d);
            }
        }
        buffer.rewind();

        clRoundMem.add(result);
        return result;
    }

    private CLBuffer<IntBuffer> generateDeformationStepCounts(final List<double[]> deformationLimits) {
        final List<int[]> counts = new ArrayList<>(deformationLimits.size());
        for (double[] dA : deformationLimits) {
            counts.add(DeformationUtils.generateDeformationCounts(dA));
        }

        final CLBuffer<IntBuffer> result = context.createIntBuffer(counts.size() * counts.get(0).length, CLMemory.Mem.READ_ONLY);
        final IntBuffer buffer = result.getBuffer();
        for (int[] iA : counts) {
            for (int i : iA) {
                buffer.put(i);
            }
        }
        buffer.rewind();
        clRoundMem.add(result);
        return result;
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
