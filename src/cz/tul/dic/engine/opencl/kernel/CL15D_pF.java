/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernel;

import cz.tul.dic.engine.opencl.OpenCLDataPackage;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLEvent;
import com.jogamp.opencl.CLEventList;
import cz.tul.dic.engine.kernel.KernelInfo;
import cz.tul.dic.engine.kernel.WorkSizeManager;
import cz.tul.dic.engine.memory.MemoryManager;
import java.nio.IntBuffer;

public class CL15D_pF extends OpenCLKernel {

    private static final int ARGUMENT_INDEX_D_COUNT = 11;
    private static final int ARGUMENT_INDEX_D_BASE = 12;
    private static final int ARGUMENT_INDEX_S_INDEX = 13;
    private static final int LWS0_BASE = 32;    
    private boolean stop;

    public CL15D_pF(final KernelInfo kernelInfo, final MemoryManager memManager, final WorkSizeManager wsm) {
        super(kernelInfo, memManager, wsm);
    }

    @Override
    public void runKernel(final OpenCLDataPackage data,
            final long deformationCount, final int imageWidth,
            final int subsetSize, final int subsetCount) {
        stop = false;
        final int subsetArea = subsetSize * subsetSize;
        long lws0 = OpenCLKernel.roundUp(calculateLws0base(), subsetArea);
        lws0 = Math.min(lws0, getMaxWorkItemSize());

        kernelDIC.rewind();
        kernelDIC.putArgs(data.getMemoryObjects())
                .putArg(imageWidth)
                .putArg(deformationCount)
                .putArg(subsetSize)
                .putArg(subsetCount)
                .putArg(0L)
                .putArg(0L)
                .putArg(0);
        final CLBuffer<IntBuffer> weights = data.getWeights();
        if (getKernelInfo().getCorrelation() == KernelInfo.Correlation.WZNSSD) {
            kernelDIC.putArg(weights);
        }
        kernelDIC.rewind();
        // copy data and execute kernel
        wsm.setMaxSubsetCount(subsetCount);
        wsm.setMaxDeformationCount(deformationCount);
        wsm.reset();
        long globalWorkSize, deformationSubCount, currentBaseDeformation;
        long time;
        CLEvent event;
        int currentBaseSubset = 0;
        int counter = 0;
        CLEventList eventList = new CLEventList(subsetCount);
        while (currentBaseSubset < subsetCount) {
            currentBaseDeformation = 0;
            kernelDIC.setArg(ARGUMENT_INDEX_S_INDEX, currentBaseSubset);

            while (currentBaseDeformation < deformationCount) {
                if (counter == eventList.capacity()) {
                    eventList = new CLEventList(subsetCount);
                    counter = 0;
                }
                if (stop) {
                    return;
                }

                deformationSubCount = Math.min(wsm.getDeformationCount(), deformationCount - currentBaseDeformation);

                globalWorkSize = OpenCLKernel.roundUp(lws0, deformationSubCount);

                kernelDIC.setArg(ARGUMENT_INDEX_D_COUNT, deformationSubCount);
                kernelDIC.setArg(ARGUMENT_INDEX_D_BASE, currentBaseDeformation);
                queue.put1DRangeKernel(kernelDIC, 0, globalWorkSize, lws0, eventList);

                queue.putWaitForEvent(eventList, counter, true);
                event = eventList.getEvent(counter);
                time = event.getProfilingInfo(CLEvent.ProfilingCommand.END) - event.getProfilingInfo(CLEvent.ProfilingCommand.START);
                wsm.storeTime(1, deformationSubCount, time);

                currentBaseDeformation += deformationSubCount;
                counter++;
            }

            currentBaseSubset += 1;
        }

        eventList.release();
    }

    private static int calculateLws0base() {
        return LWS0_BASE;
    }

    @Override
    public void stopComputation() {
        stop = true;
    }

}
