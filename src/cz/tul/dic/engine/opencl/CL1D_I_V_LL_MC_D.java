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

    private static final int ARGUMENT_INDEX_COUNT = 11;
    private static final int ARGUMENT_INDEX_BASE = 12;
    private static final int LWS0_BASE = 0;
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
        final int lws0base = calculateLws0base(kernel);
        int lws0;
        if (deformationCount > facetArea) {
            lws0 = EngineMath.roundUp(lws0base, facetArea);
        } else {
            lws0 = EngineMath.roundUp(lws0base, deformationCount);
        }
        lws0 = Math.min(lws0, device.getMaxWorkItemSizes()[0]);

        int groupCountPerFacet = deformationCount / lws0;
        if (deformationCount % lws0 > 0) {
            groupCountPerFacet++;
        }

        kernel.rewind();
        kernel.putArgs(imgA, imgB, facetData, facetCenters, deformations, results)
                .putArg(imageWidth)
                .putArg(deformationCount)
                .putArg(facetSize)
                .putArg(facetCount)
                .putArg(groupCountPerFacet)
                .putArg(0)
                .putArg(0);
        kernel.rewind();
        // copy data and execute kernel
        wsm.setMaxCount(facetCount);
        wsm.reset();
        int facetSubCount = wsm.getWorkSize();
        int facetGlobalWorkSize;
        long time;
        CLEvent event;
        int actualBase = 0, counter = 0;
        final CLEventList eventList = new CLEventList((int) Math.ceil(facetCount / (double) facetSubCount));
        while (actualBase < facetCount) {
            facetSubCount = Math.min(wsm.getWorkSize(), facetCount);
            facetGlobalWorkSize = EngineMath.roundUp(lws0, deformationCount) * facetSubCount;

            kernel.setArg(ARGUMENT_INDEX_BASE, actualBase);
            kernel.setArg(ARGUMENT_INDEX_COUNT, facetSubCount);
            queue.put1DRangeKernel(kernel, 0, facetGlobalWorkSize, lws0, eventList);

            queue.putWaitForEvent(eventList, counter, true);
            event = eventList.getEvent(counter);
            time = event.getProfilingInfo(CLEvent.ProfilingCommand.END) - event.getProfilingInfo(CLEvent.ProfilingCommand.START);
            wsm.storeTime(facetSubCount, time);

            actualBase += facetSubCount;
            counter++;
        }

        eventList.release();
    }

    private int calculateLws0base(final CLKernel kernel) {
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
