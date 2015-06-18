/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernels;

import cz.tul.dic.engine.opencl.memory.AbstractOpenCLMemoryManager;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLEvent;
import com.jogamp.opencl.CLEventList;
import com.jogamp.opencl.CLMemory;
import cz.tul.dic.engine.opencl.WorkSizeManager;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class CL1D_I_V_LL_MC_D extends Kernel {

    private static final int ARGUMENT_INDEX_G_COUNT = 11;
    private static final int ARGUMENT_INDEX_F_COUNT = 12;
    private static final int ARGUMENT_INDEX_F_BASE = 13;
    private static final int ARGUMENT_INDEX_D_COUNT = 14;
    private static final int ARGUMENT_INDEX_D_BASE = 15;
    private static final int LWS0_BASE = 32;
    private final WorkSizeManager wsm;
    private boolean stop;

    public CL1D_I_V_LL_MC_D(final AbstractOpenCLMemoryManager memManager) {
        super("CL1D_I_V_LL_MC_D", memManager);
        wsm = new WorkSizeManager(KernelType.CL1D_I_V_LL_MC_D);
    }

    @Override
    void runKernel(final CLMemory<IntBuffer> imgA, final CLMemory<IntBuffer> imgB,
            final CLBuffer<IntBuffer> facetData,
            final CLBuffer<FloatBuffer> facetCenters,
            final CLBuffer<FloatBuffer> deformationLimits, final CLBuffer<IntBuffer> defStepCounts, 
            final CLBuffer<FloatBuffer> results,
            final int deformationCount, final int imageWidth,
            final int facetSize, final int facetCount) {
        stop = false;
        final int facetArea = facetSize * facetSize;
        int lws0 = Kernel.roundUp(calculateLws0base(), facetArea);
        lws0 = Math.min(lws0, getMaxWorkItemSize());

        kernelDIC.rewind();
        kernelDIC.putArgs(imgA, imgB, facetData, facetCenters, deformationLimits, defStepCounts, results)
                .putArg(imageWidth)
                .putArg(deformationCount)
                .putArg(facetSize)
                .putArg(facetCount)
                .putArg(0)
                .putArg(0)
                .putArg(0)
                .putArg(0)
                .putArg(0);
        kernelDIC.rewind();
        // copy data and execute kernel
        wsm.setMaxFacetCount(facetCount);
        wsm.setMaxDeformationCount(deformationCount);
        wsm.reset();
        int facetGlobalWorkSize, facetSubCount = 1, deformationSubCount;
        long time;
        CLEvent event;
        int currentBaseFacet = 0, currentBaseDeformation;
        int groupCountPerFacet, counter = 0;
        CLEventList eventList = new CLEventList(facetCount);
        while (currentBaseFacet < facetCount) {
            currentBaseDeformation = 0;

            while (currentBaseDeformation < deformationCount) {
                if (counter == eventList.capacity()) {
                    eventList = new CLEventList(facetCount);
                    counter = 0;                    
                }    
                if (stop) {
                    return;
                }

                facetSubCount = Math.min(wsm.getFacetCount(), facetCount - currentBaseFacet);
                deformationSubCount = Math.min(wsm.getDeformationCount(), deformationCount - currentBaseDeformation);

                facetGlobalWorkSize = Kernel.roundUp(lws0, deformationSubCount) * facetSubCount;

                groupCountPerFacet = deformationSubCount / lws0;
                if (deformationCount % lws0 > 0) {
                    groupCountPerFacet++;
                }

                kernelDIC.setArg(ARGUMENT_INDEX_G_COUNT, groupCountPerFacet);
                kernelDIC.setArg(ARGUMENT_INDEX_F_COUNT, facetSubCount);
                kernelDIC.setArg(ARGUMENT_INDEX_F_BASE, currentBaseFacet);
                kernelDIC.setArg(ARGUMENT_INDEX_D_COUNT, deformationSubCount);
                kernelDIC.setArg(ARGUMENT_INDEX_D_BASE, currentBaseDeformation);
                queue.put1DRangeKernel(kernelDIC, 0, facetGlobalWorkSize, lws0, eventList);

                queue.putWaitForEvent(eventList, counter, true);
                event = eventList.getEvent(counter);
                time = event.getProfilingInfo(CLEvent.ProfilingCommand.END) - event.getProfilingInfo(CLEvent.ProfilingCommand.START);
                wsm.storeTime(facetSubCount, deformationSubCount, time);

                currentBaseDeformation += deformationSubCount;
                counter++;
            }

            currentBaseFacet += facetSubCount;
        }

        eventList.release();
    }

    private int calculateLws0base() {
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
    public boolean usesImage() {
        return true;
    }
    
    @Override
    public void stopComputation() {
        stop = true;
    }

}
