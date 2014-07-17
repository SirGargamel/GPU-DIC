package cz.tul.dic.engine.opencl;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLEvent;
import com.jogamp.opencl.CLEventList;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLKernel;
import cz.tul.dic.engine.EngineMath;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class CL1D_I_V_LL_MC_D extends Kernel {

    private static final int ARGUMENT_INDEX_G_COUNT = 10;
    private static final int ARGUMENT_INDEX_F_COUNT = 11;
    private static final int ARGUMENT_INDEX_F_BASE = 12;
    private static final int ARGUMENT_INDEX_D_COUNT = 13;
    private static final int ARGUMENT_INDEX_D_BASE = 14;
    private static final int LWS0_BASE = 32;
    private final WorkSizeManager wsm;

    public CL1D_I_V_LL_MC_D() {
        super("CL1D_I_V_LL_MC_D");
        wsm = new WorkSizeManager();
    }

    @Override
    void runKernel(final CLImage2d<IntBuffer> imgA, final CLImage2d<IntBuffer> imgB,
            final CLBuffer<IntBuffer> facetData,
            final CLBuffer<FloatBuffer> facetCenters,
            final CLBuffer<FloatBuffer> deformations, final CLBuffer<FloatBuffer> results,
            final int deformationCount, final int imageWidth,
            final int facetSize, final int facetCount) {
        final int facetArea = facetSize * facetSize;
        int lws0 = EngineMath.roundUp(calculateLws0base(), facetArea);
        lws0 = Math.min(lws0, device.getMaxWorkItemSizes()[0]);

        kernel.rewind();
        kernel.putArgs(imgA, imgB, facetData, facetCenters, deformations, results)
                .putArg(imageWidth)
                .putArg(deformationCount)
                .putArg(facetSize)
                .putArg(facetCount)
                .putArg(0)
                .putArg(0)
                .putArg(0)
                .putArg(0)
                .putArg(0);
        kernel.rewind();
        // copy data and execute kernel
        wsm.setMaxFacetCount(facetCount);
        wsm.setMaxDeformationCount(deformationCount);
        wsm.reset();
        int facetGlobalWorkSize, facetSubCount = 1, deformationSubCount;
        long time;
        CLEvent event;
        int currentBaseFacet = 0, currentBaseDeformation;
        int groupCountPerFacet, counter = 0;
        CLEventList eventList = new CLEventList(facetCount * 2);
        while (currentBaseFacet < facetCount) {
            currentBaseDeformation = 0;

            while (currentBaseDeformation < deformationCount) {
                if (counter == eventList.size()) {
                    eventList = new CLEventList(facetCount * 2);
                    counter = 0;
                }

                facetSubCount = Math.min(wsm.getFacetCount(), facetCount - currentBaseFacet);
                deformationSubCount = Math.min(wsm.getDeformationCount(), deformationCount - currentBaseDeformation);                

                facetGlobalWorkSize = EngineMath.roundUp(lws0, deformationSubCount) * facetSubCount;

                groupCountPerFacet = deformationSubCount / lws0;
                if (deformationCount % lws0 > 0) {
                    groupCountPerFacet++;
                }

                kernel.setArg(ARGUMENT_INDEX_G_COUNT, groupCountPerFacet);
                kernel.setArg(ARGUMENT_INDEX_F_COUNT, facetSubCount);
                kernel.setArg(ARGUMENT_INDEX_F_BASE, currentBaseFacet);
                kernel.setArg(ARGUMENT_INDEX_D_COUNT, deformationSubCount);
                kernel.setArg(ARGUMENT_INDEX_D_BASE, currentBaseDeformation);
                queue.put1DRangeKernel(kernel, 0, facetGlobalWorkSize, lws0, eventList);

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
//        final IntBuffer val = Buffers.newDirectIntBuffer(5);
//        context.getCL().clGetKernelWorkGroupInfo(kernel.getID(), queue.getDevice().getID(), CLKernelBinding.CL_KERNEL_PREFERRED_WORK_GROUP_SIZE_MULTIPLE, Integer.SIZE, val, null);
//        return val.get(0);
        return LWS0_BASE;
    }

    @Override
    boolean usesMemoryCoalescing() {
        return true;
    }

    @Override
    boolean usesVectorization() {
        return true;
    }

    @Override
    boolean isDriven() {
        return true;
    }

}
