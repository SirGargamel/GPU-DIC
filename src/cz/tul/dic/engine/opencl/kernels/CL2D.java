/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernels;

import com.jogamp.opencl.CLBuffer;
import cz.tul.dic.engine.opencl.memory.AbstractOpenCLMemoryManager;
import com.jogamp.opencl.CLEvent;
import com.jogamp.opencl.CLEventList;
import cz.tul.dic.engine.opencl.WorkSizeManager;
import java.nio.IntBuffer;

public class CL2D extends Kernel {

    private static final int ARGUMENT_INDEX_D_COUNT = 11;
    private static final int ARGUMENT_INDEX_D_BASE = 12;
    private static final int ARGUMENT_INDEX_G_COUNT = 13;
    private static final int ARGUMENT_INDEX_S_COUNT = 14;
    private static final int ARGUMENT_INDEX_S_BASE = 15;
    private static final int LWS0_BASE = 1;
    private static final int LWS1_BASE = 64;
    private boolean stop;

    public CL2D(final KernelInfo kernelInfo, final AbstractOpenCLMemoryManager memManager, final WorkSizeManager wsm) {
        super(kernelInfo, memManager, wsm);
    }

    @Override
    public void runKernel(final OpenCLDataPackage data,
            final long deformationCount, final int imageWidth,
            final int subsetSize, final int subsetCount) {
        stop = false;
        final int subsetArea = subsetSize * subsetSize;

        final int lws0 = calculateLws0();
        long lws1 = Kernel.roundUp(calculateLws1Base(), subsetArea);
        lws1 = Math.min(lws1, getMaxWorkItemSize());

        kernelDIC.rewind();
        kernelDIC.putArgs(data.getMemoryObjects())
                .putArg(imageWidth)
                .putArg(deformationCount)
                .putArg(subsetSize)
                .putArg(subsetCount)
                .putArg(0L)
                .putArg(0L)
                .putArg(0L)
                .putArg(0L)
                .putArg(0L);
        final CLBuffer<IntBuffer> weights = data.getWeights();
        if (kernelInfo.getCorrelation() == KernelInfo.Correlation.WZNSSD) {
            kernelDIC.putArg(weights);
        }
        kernelDIC.rewind();
        // copy data and execute kernel
        wsm.setMaxSubsetCount(subsetCount);
        wsm.setMaxDeformationCount(deformationCount);
        wsm.reset();
        long subsetGlobalWorkSize, deformationGlobalWorkSize, subsetSubCount = 1;
        long deformationSubCount;
        long time;
        CLEvent event;
        long currentBaseSubset = 0, currentBaseDeformation, groupCountPerSubset;
        int counter = 0;
        CLEventList eventList = new CLEventList(subsetCount);
        while (currentBaseSubset < subsetCount) {
            currentBaseDeformation = 0;

            while (currentBaseDeformation < deformationCount) {
                if (counter == eventList.capacity()) {
                    eventList = new CLEventList(subsetCount);
                    counter = 0;
                }
                if (stop) {
                    return;
                }

                subsetSubCount = Math.min(wsm.getSubsetCount(), subsetCount - currentBaseSubset);
                deformationSubCount = Math.min(wsm.getDeformationCount(), deformationCount - currentBaseDeformation);

                subsetGlobalWorkSize = Kernel.roundUp(lws0, subsetSubCount);
                deformationGlobalWorkSize = Kernel.roundUp(lws1, deformationSubCount);

                groupCountPerSubset = deformationSubCount / lws1;
                if (deformationCount % lws1 > 0) {
                    groupCountPerSubset++;
                }

                kernelDIC.setArg(ARGUMENT_INDEX_G_COUNT, groupCountPerSubset);
                kernelDIC.setArg(ARGUMENT_INDEX_S_COUNT, subsetSubCount);
                kernelDIC.setArg(ARGUMENT_INDEX_S_BASE, currentBaseSubset);
                kernelDIC.setArg(ARGUMENT_INDEX_D_COUNT, deformationSubCount);
                kernelDIC.setArg(ARGUMENT_INDEX_D_BASE, currentBaseDeformation);
                queue.put2DRangeKernel(kernelDIC, 0, 0, subsetGlobalWorkSize, deformationGlobalWorkSize, lws0, lws1, eventList);

                queue.putWaitForEvent(eventList, counter, true);
                event = eventList.getEvent(counter);
                time = event.getProfilingInfo(CLEvent.ProfilingCommand.END) - event.getProfilingInfo(CLEvent.ProfilingCommand.START);
                wsm.storeTime(subsetSubCount, deformationSubCount, time);

                currentBaseDeformation += deformationSubCount;
                counter++;
            }

            currentBaseSubset += subsetSubCount;
        }

        eventList.release();
    }

    private static int calculateLws1Base() {
        return LWS1_BASE;
    }

    private static int calculateLws0() {
        return LWS0_BASE;
    }

    @Override
    public boolean is2D() {
        return true;
    }

    @Override
    public boolean subsetsGroupped() {
        return true;
    }

    @Override
    public void stopComputation() {
        stop = true;
    }

}
