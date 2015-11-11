/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernels;

import cz.tul.dic.engine.opencl.kernels.info.KernelInfo;
import cz.tul.dic.engine.opencl.memory.AbstractOpenCLMemoryManager;
import com.jogamp.opencl.CLEvent;
import com.jogamp.opencl.CLEventList;
import cz.tul.dic.engine.opencl.WorkSizeManager;

public class CL1D_I_V_LL_MC_D extends Kernel {

    private static final int ARGUMENT_INDEX_D_COUNT = 11;
    private static final int ARGUMENT_INDEX_D_BASE = 12;
    private static final int ARGUMENT_INDEX_G_COUNT = 13;
    private static final int ARGUMENT_INDEX_S_COUNT = 14;
    private static final int ARGUMENT_INDEX_S_BASE = 15;
    private static final int LWS0_BASE = 32;
    private boolean stop;

    public CL1D_I_V_LL_MC_D(final KernelInfo kernelInfo, final AbstractOpenCLMemoryManager memManager, final WorkSizeManager wsm) {
        super(kernelInfo, memManager, wsm);
    }

    @Override
    public void runKernel(final OpenCLDataPackage data,
            final long deformationCount, final int imageWidth,
            final int subsetSize, final int subsetCount) {
        stop = false;
        final int subsetArea = subsetSize * subsetSize;
        long lws0 = Kernel.roundUp(calculateLws0base(), subsetArea);
        lws0 = Math.min(lws0, getMaxWorkItemSize());

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
        kernelDIC.rewind();
        // copy data and execute kernel
        wsm.setMaxSubsetCount(subsetCount);
        wsm.setMaxDeformationCount(deformationCount);
        wsm.reset();
        long subsetGlobalWorkSize, subsetSubCount = 1, deformationSubCount, groupCountPerSubset;
        long time;
        CLEvent event;
        long currentBaseSubset = 0, currentBaseDeformation;
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

                subsetGlobalWorkSize = Kernel.roundUp(lws0, deformationSubCount) * subsetSubCount;

                groupCountPerSubset = deformationSubCount / lws0;
                if (deformationCount % lws0 > 0) {
                    groupCountPerSubset++;
                }

                kernelDIC.setArg(ARGUMENT_INDEX_G_COUNT, groupCountPerSubset);
                kernelDIC.setArg(ARGUMENT_INDEX_S_COUNT, subsetSubCount);
                kernelDIC.setArg(ARGUMENT_INDEX_S_BASE, currentBaseSubset);
                kernelDIC.setArg(ARGUMENT_INDEX_D_COUNT, deformationSubCount);
                kernelDIC.setArg(ARGUMENT_INDEX_D_BASE, currentBaseDeformation);
                queue.put1DRangeKernel(kernelDIC, 0, subsetGlobalWorkSize, lws0, eventList);

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

    private static int calculateLws0base() {
        return LWS0_BASE;
    }

    @Override
    public boolean usesMemoryCoalescing() {
        return true;
    }

    @Override
    public boolean usesVectorization() {
        return true;
    }

    @Override
    public boolean usesLocalMemory() {
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
