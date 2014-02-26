package cz.tul.dic.engine.opencl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLKernel;
import com.jogamp.opencl.llb.CLKernelBinding;
import cz.tul.dic.engine.EngineMath;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class CL1D_I_V_LL_MC_D extends Kernel {

    private static final int ARGUMENT_INDEX = 12;
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
        final int facetSubCount = Math.min(wsm.getWorkSize(this.getClass()), facetCount);
        final int lws0base = calculateLws0base(kernel);
        final int lws0;
        if (deformationCount > facetArea) {
            lws0 = EngineMath.roundUp(lws0base, facetArea);
        } else {
            lws0 = EngineMath.roundUp(lws0base, deformationCount);
        }
        final int facetGlobalWorkSize = EngineMath.roundUp(lws0, deformationCount) * facetSubCount;

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
                .putArg(facetSubCount)
                .putArg(0);
        kernel.rewind();
        // copy data and execute kernel

        final int kernelRoundCount = (int) Math.ceil(facetCount / (double) facetSubCount);
        for (int kernelRound = 0; kernelRound < kernelRoundCount; kernelRound++) {
            kernel.setArg(ARGUMENT_INDEX, kernelRound * facetSubCount);
            queue.put1DRangeKernel(kernel, 0, facetGlobalWorkSize, lws0);
        }

        queue.put1DRangeKernel(kernel, 0, facetGlobalWorkSize, lws0);
    }

    private int calculateLws0base(final CLKernel kernel) {
        final IntBuffer val = Buffers.newDirectIntBuffer(2);
        context.getCL().clGetKernelWorkGroupInfo(kernel.getID(), queue.getDevice().getID(), CLKernelBinding.CL_KERNEL_PREFERRED_WORK_GROUP_SIZE_MULTIPLE, Integer.SIZE, val, null);
        return val.get(0);
    }

    @Override
    boolean usesMemoryCoalescing() {
        return true;
    }
    
    @Override
    boolean usesVectorization() {
        return true;
    }

}
