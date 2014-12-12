/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic.engine.opencl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLMemory;
import com.jogamp.opencl.CLResource;
import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.engine.opencl.kernels.Kernel;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

/**
 *
 * @author Petr Ječmen
 */
public class OpenCLMemoryManager {

    private static final CLImageFormat IMAGE_FORMAT = new CLImageFormat(CLImageFormat.ChannelOrder.RGBA, CLImageFormat.ChannelType.UNSIGNED_INT8);
    private Image imageA, imageB;
    private List<Facet> facets;
    private List<double[]> deformationLimits;
    private List<int[]> deformationCounts;
    private int maxDeformationCount;
    // OpenCL entities
    private CLMemory<IntBuffer> clImageA, clImageB;
    private CLBuffer<IntBuffer> clFacetData;
    private CLBuffer<FloatBuffer> clFacetCenters;
    private CLBuffer<FloatBuffer> clDeformationLimits;
    private CLBuffer<IntBuffer> clDefStepCount;
    private CLBuffer<FloatBuffer> clResults;
    // OpenCL context        
    private final CLCommandQueue queue;
    private final CLDevice device;
    private final CLContext context;

    public OpenCLMemoryManager() {
        device = DeviceManager.getDevice();
        context = DeviceManager.getContext();
        queue = device.createCommandQueue(CLCommandQueue.Mode.PROFILING_MODE);
    }

    public void assignData(Image imageA, Image imageB, List<Facet> facets, List<double[]> deformationLimits, Kernel kernel) throws ComputationException {        
        if (imageA != this.imageA) {
            release(clImageA);
            this.imageA = imageA;

            if (kernel.usesImage()) {
                clImageA = generateImage2d_t(imageA);
                queue.putWriteImage((CLImage2d<IntBuffer>) clImageA, false);
            } else {
                clImageA = generateImageArray(imageA);
                queue.putWriteBuffer((CLBuffer<IntBuffer>) clImageA, false);
            }
        }
        if (imageB != this.imageB) {
            release(clImageB);
            this.imageB = imageB;

            if (kernel.usesImage()) {
                clImageB = generateImage2d_t(imageB);
                queue.putWriteImage((CLImage2d<IntBuffer>) clImageB, false);
            } else {
                clImageB = generateImageArray(imageB);
                queue.putWriteBuffer((CLBuffer<IntBuffer>) clImageB, false);
            }
        }

        boolean changedResults = false;
        if (facets != this.facets) {
            release(clFacetData);
            release(clFacetCenters);
            this.facets = facets;

            clFacetData = generateFacetData(facets, kernel.usesMemoryCoalescing());
            queue.putWriteBuffer(clFacetData, false);

            clFacetCenters = generateFacetCenters(facets);
            queue.putWriteBuffer(clFacetCenters, false);

            changedResults = true;
        }
        if (deformationLimits != this.deformationLimits) {
            release(clDeformationLimits);
            release(clDefStepCount);
            this.deformationLimits = deformationLimits;

            clDeformationLimits = generateDeformationLimits(deformationLimits);
            queue.putWriteBuffer(clDeformationLimits, false);

            deformationCounts = DeformationUtils.generateDeformationCounts(deformationLimits);
            clDefStepCount = generateDeformationStepCounts(deformationCounts);
            queue.putWriteBuffer(clDefStepCount, false);

            changedResults = true;
        }

        if (changedResults) {
            release(clResults);

            maxDeformationCount = DeformationUtils.findMaxDeformationCount(deformationCounts);
            final long size = facets.size() * maxDeformationCount;
            if (size <= 0 || size >= Integer.MAX_VALUE) {
                throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, "Illegal size of resulting array - " + size);
            }
            clResults = context.createFloatBuffer((int) size, CLMemory.Mem.READ_WRITE);
        }
    }

    private CLImage2d<IntBuffer> generateImage2d_t(final Image image) {
        final CLImage2d<IntBuffer> result = context.createImage2d(
                Buffers.newDirectIntBuffer(image.toBWArray()),
                image.getWidth(), image.getHeight(),
                IMAGE_FORMAT, CLMemory.Mem.READ_ONLY);
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
        return result;
    }

    private CLBuffer<IntBuffer> generateFacetData(final List<Facet> facets, final boolean useMemoryCoalescing) {
        final int facetSize = facets.get(0).getSize();
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
        return result;
    }

    private CLBuffer<IntBuffer> generateDeformationStepCounts(final List<int[]> counts) {
        final CLBuffer<IntBuffer> result = context.createIntBuffer(counts.size() * counts.get(0).length, CLMemory.Mem.READ_ONLY);
        final IntBuffer buffer = result.getBuffer();
        for (int[] iA : counts) {
            for (int i : iA) {
                buffer.put(i);
            }
        }
        buffer.rewind();
        return result;
    }

    private void release(CLResource mem) {
        if (mem != null && !mem.isReleased()) {
            mem.release();
        }
    }

    public void releaseAll() {
        release(clDefStepCount);
        release(clDeformationLimits);
        release(clFacetCenters);
        release(clFacetData);
        release(clImageA);
        release(clImageB);
        release(clResults);
        release(context);
        release(queue);        
    }

    public CLMemory<IntBuffer> getClImageA() {
        return clImageA;
    }

    public CLMemory<IntBuffer> getClImageB() {
        return clImageB;
    }

    public CLBuffer<IntBuffer> getClFacetData() {
        return clFacetData;
    }

    public CLBuffer<FloatBuffer> getClFacetCenters() {
        return clFacetCenters;
    }

    public CLBuffer<FloatBuffer> getClDeformationLimits() {
        return clDeformationLimits;
    }

    public CLBuffer<IntBuffer> getClDefStepCount() {
        return clDefStepCount;
    }

    public CLBuffer<FloatBuffer> getClResults() {
        return clResults;
    }

    public int getMaxDeformationCount() {
        return maxDeformationCount;
    }

    public CLContext getContext() {
        return context;
    }

    public CLCommandQueue getQueue() {
        return queue;
    }

    public CLDevice getDevice() {
        return device;
    }

}
