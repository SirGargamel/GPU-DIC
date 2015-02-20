/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic.engine.opencl.memory;

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
import cz.tul.dic.engine.opencl.DeviceManager;
import cz.tul.dic.engine.opencl.kernels.Kernel;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

/**
 *
 * @author Petr Ječmen
 */
public abstract class OpenCLMemoryManager {

    private static final CLImageFormat IMAGE_FORMAT = new CLImageFormat(CLImageFormat.ChannelOrder.RGBA, CLImageFormat.ChannelType.UNSIGNED_INT8);    
    protected int maxDeformationCount;
    // OpenCL entities
    protected CLMemory<IntBuffer> clImageA, clImageB;
    protected CLBuffer<IntBuffer> clFacetData;
    protected CLBuffer<FloatBuffer> clFacetCenters;
    protected CLBuffer<FloatBuffer> clDeformationLimits;
    protected CLBuffer<IntBuffer> clDefStepCount;
    protected CLBuffer<FloatBuffer> clResults;
    // OpenCL context        
    protected final CLCommandQueue queue;
    protected final CLDevice device;
    protected final CLContext context;

    public OpenCLMemoryManager() {
        device = DeviceManager.getDevice();
        context = DeviceManager.getContext();
        queue = device.createCommandQueue(CLCommandQueue.Mode.PROFILING_MODE);
    }

    public abstract void assignData(Image imageA, Image imageB, List<Facet> facets, List<double[]> deformationLimits, Kernel kernel) throws ComputationException;

    protected CLImage2d<IntBuffer> generateImage2d_t(final Image image) {
        final CLImage2d<IntBuffer> result = context.createImage2d(
                Buffers.newDirectIntBuffer(image.toBWArray()),
                image.getWidth(), image.getHeight(),
                IMAGE_FORMAT, CLMemory.Mem.READ_ONLY);
        return result;
    }

    protected CLBuffer<IntBuffer> generateImageArray(final Image image) {
        final int[] data = image.toBWArray();
        final CLBuffer<IntBuffer> result = context.createIntBuffer(data.length, CLMemory.Mem.READ_ONLY);
        final IntBuffer buffer = result.getBuffer();
        for (int i : data) {
            buffer.put(i);
        }
        buffer.rewind();
        return result;
    }

    protected CLBuffer<IntBuffer> generateFacetData(final List<Facet> facets, final boolean useMemoryCoalescing) {
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

    protected CLBuffer<FloatBuffer> generateFacetCenters(final List<Facet> facets) {
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

    protected CLBuffer<FloatBuffer> generateDeformationLimits(final List<double[]> deformationLimits) {
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

    protected CLBuffer<IntBuffer> generateDeformationStepCounts(final List<int[]> counts) {
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

    protected void release(CLResource mem) {
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
