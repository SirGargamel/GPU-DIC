package cz.tul.dic.engine.opencl;

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
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationDegree;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Petr Jecmen
 */
public abstract class Kernel {

    public static Kernel createKernel(final KernelType kernelType) {
        switch (kernelType) {
            case CL_2D_I:
                return new CL2DImage();
            case CL_1D_I_V_LL_MC:
                return new CL1D_I_V_LL_MC();
            case CL_1D_I_V_LL_MC_D:
                return new CL1D_I_V_LL_MC_D();
            default:
                System.out.println("Using default kernel.");
                return new CL1D_I_V_LL_MC_D();
        }
    }

    private static final CLImageFormat IMAGE_FORMAT = new CLImageFormat(CLImageFormat.ChannelOrder.RGBA, CLImageFormat.ChannelType.UNSIGNED_INT8);
    private final String kernelName;
    protected CLContext context;
    protected CLKernel kernel;
    protected CLDevice device;
    protected CLCommandQueue queue;
    private CLProgram program;
    private final Set<CLResource> clMem;

    public Kernel(String kernelName) {
        this.kernelName = kernelName;
        clMem = new HashSet<>();
    }

    public void prepareKernel(final CLContext context, final CLDevice device, final int facetSize, final DeformationDegree deg, final int defArrayLength) throws IOException {
        this.context = context;
        this.device = device;

        program = context.createProgram(KernelSourcePreparator.prepareKernel(kernelName, facetSize, deg, defArrayLength, usesVectorization())).build();
        clMem.add(program);
        kernel = program.createCLKernel(kernelName);
        clMem.add(kernel);
        if (isDriven()) {
            queue = device.createCommandQueue(CLCommandQueue.Mode.PROFILING_MODE);
        } else {
            queue = device.createCommandQueue();
        }
        clMem.add(queue);
    }

    public float[] compute(Image imageA, Image imageB, List<Facet> facets, double[] deformations, int deformationLength) {
        final CLImage2d<IntBuffer> clImageA, clImageB;
        final CLBuffer<IntBuffer> clFacetData;
        final CLBuffer<FloatBuffer> clFacetCenters;
        final CLBuffer<FloatBuffer> clDeformations, clResults;
        // prepare data                
        clImageA = generateImage(imageA);
        queue.putWriteImage(clImageA, false);
        clImageB = generateImage(imageB);
        queue.putWriteImage(clImageB, false);

        final int facetCount = facets.size();
        final int facetSize = facets.get(0).getSize();

        clFacetData = generateFacetData(facets, facetSize, usesMemoryCoalescing());
        queue.putWriteBuffer(clFacetData, false);

        clFacetCenters = generateFacetCenters(facets);
        queue.putWriteBuffer(clFacetCenters, false);

        clDeformations = generateDeformations(deformations);
        queue.putWriteBuffer(clDeformations, false);
        final int deformationCount = deformations.length / deformationLength;

        clResults = context.createFloatBuffer(facetCount * deformationCount, CLMemory.Mem.WRITE_ONLY);
        clMem.add(clResults);

        runKernel(clImageA, clImageB,
                clFacetData, clFacetCenters,
                clDeformations, clResults,
                deformationCount, imageA.getWidth(), facetSize, facetCount);

        // read result
        // copy data back            
        queue.putReadBuffer(clResults, true);
        return readBuffer(clResults.getBuffer());
    }

    abstract void runKernel(final CLImage2d<IntBuffer> imgA, final CLImage2d<IntBuffer> imgB,
            final CLBuffer<IntBuffer> facetData,
            final CLBuffer<FloatBuffer> facetCenters,
            final CLBuffer<FloatBuffer> deformations, final CLBuffer<FloatBuffer> results,
            final int deformationCount, final int imageWidth,
            final int facetSize, final int facetCount);

    boolean usesMemoryCoalescing() {
        return false;
    }

    boolean usesVectorization() {
        return false;
    }

    boolean isDriven() {
        return false;
    }

    public void finish() {
        queue.finish();

        clearMem(clMem);
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
        clMem.add(result);
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

        clMem.add(result);
        return result;
    }

    private CLBuffer<FloatBuffer> generateFacetCenters(final List<Facet> facets) {
        final int dataSize = Coordinates.DIMENSION;
        final float[] data = new float[facets.size() * dataSize];

        int pointer = 0;
        float[] centerData;
        for (Facet f : facets) {
            centerData = f.getCenter();
            System.arraycopy(centerData, 0, data, pointer, dataSize);
            pointer += dataSize;
        }

        final CLBuffer<FloatBuffer> result = context.createFloatBuffer(data.length, CLMemory.Mem.READ_ONLY);
        final FloatBuffer buffer = result.getBuffer();
        for (float f : data) {
            buffer.put(f);
        }
        buffer.rewind();

        clMem.add(result);
        return result;
    }

    private CLBuffer<FloatBuffer> generateDeformations(final double[] deformations) {
        final CLBuffer<FloatBuffer> result = context.createFloatBuffer(deformations.length, CLMemory.Mem.READ_ONLY);
        final FloatBuffer buffer = result.getBuffer();
        for (double d : deformations) {
            buffer.put((float) d);
        }
        buffer.rewind();

        clMem.add(result);
        return result;
    }

    private float[] readBuffer(final FloatBuffer buffer) {
        buffer.rewind();
        float[] result = new float[buffer.remaining()];
        for (int i = 0; i < result.length; i++) {
            result[i] = buffer.get(i);
        }
        return result;
    }

}
