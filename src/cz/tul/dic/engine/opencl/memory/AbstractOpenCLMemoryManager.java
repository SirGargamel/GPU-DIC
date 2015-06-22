/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.memory;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLImageFormat;
import com.jogamp.opencl.CLMemory;
import com.jogamp.opencl.CLResource;
import cz.tul.dic.ComputationException;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.subset.SubsetUtils;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.engine.opencl.DeviceManager;
import cz.tul.dic.engine.opencl.kernels.Kernel;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Petr Ječmen
 */
public abstract class AbstractOpenCLMemoryManager {

    private static final AbstractOpenCLMemoryManager INSTANCE;    
    private static final CLImageFormat IMAGE_FORMAT;    
    protected int maxDeformationCount;
    // OpenCL entities
    protected CLMemory<IntBuffer> clImageA, clImageB;
    protected CLBuffer<IntBuffer> clFacetData;
    protected CLBuffer<FloatBuffer> clFacetCenters;
    protected CLBuffer<FloatBuffer> clDeformationLimits;
    protected CLBuffer<IntBuffer> clDefStepCount;
    protected CLBuffer<FloatBuffer> clResults;
    // OpenCL context        
    protected CLCommandQueue queue;
    protected CLContext context;    
    private final Lock lock;

    static {
        DeviceManager.clearMemory();
        INSTANCE = new StaticMemoryManager();
        IMAGE_FORMAT = new CLImageFormat(CLImageFormat.ChannelOrder.RGBA, CLImageFormat.ChannelType.UNSIGNED_INT8);
    }
    
    public static AbstractOpenCLMemoryManager init() {
        return INSTANCE;
    }

    protected AbstractOpenCLMemoryManager() {
        lock = new ReentrantLock();
    }

    public void assignData(final ComputationTask task, final Kernel kernel) throws ComputationException {
        lock.lock();
        context = DeviceManager.getContext();
        queue = DeviceManager.getQueue();
        assignDataToGPU(task, kernel);
    }

    public abstract void assignDataToGPU(final ComputationTask task, final Kernel kernel) throws ComputationException;

    public void unlockData() {
        lock.unlock();
    }

    protected CLImage2d<IntBuffer> generateImage2d(final Image image) {        
        return context.createImage2d(
                Buffers.newDirectIntBuffer(image.toBWArray()),
                image.getWidth(), image.getHeight(),
                IMAGE_FORMAT, CLMemory.Mem.READ_ONLY);
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

    protected CLBuffer<IntBuffer> generateFacetData(final List<AbstractSubset> subsets, final boolean useMemoryCoalescing) {
        final int subsetSize = subsets.get(0).getSize();
        final int subsetArea = SubsetUtils.computeSubsetCoordCount(subsetSize);
        final int dataSize = subsetArea * Coordinates.DIMENSION;
        final int[] completeData = new int[subsets.size() * dataSize];

        int pointer = 0;
        int[] subsetData;
        for (AbstractSubset f : subsets) {
            subsetData = f.getData();
            System.arraycopy(subsetData, 0, completeData, pointer, dataSize);
            pointer += dataSize;
        }

        final CLBuffer<IntBuffer> result = context.createIntBuffer(completeData.length, CLMemory.Mem.READ_ONLY);
        final IntBuffer resultBuffer = result.getBuffer();
        if (useMemoryCoalescing) {
            int index;
            for (int i = 0; i < subsetArea; i++) {
                for (int f = 0; f < subsets.size(); f++) {
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

    protected CLBuffer<FloatBuffer> generateFacetCenters(final List<AbstractSubset> subsets) {
        final int dataSize = Coordinates.DIMENSION;
        final float[] data = new float[subsets.size() * dataSize];

        int pointer = 0;
        double[] centerData;
        for (AbstractSubset f : subsets) {
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

    protected void release(final CLResource mem) {
        if (mem != null && !mem.isReleased()) {
            mem.release();
        }
    }

    public void clearMemory() {
        release(clDefStepCount);
        release(clDeformationLimits);
        release(clFacetCenters);
        release(clFacetData);
        release(clImageA);
        release(clImageB);
        release(clResults);
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

}
