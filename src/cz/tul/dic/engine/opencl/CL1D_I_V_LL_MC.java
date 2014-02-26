package cz.tul.dic.engine.opencl;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLImage2d;
import cz.tul.dic.engine.EngineMath;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class CL1D_I_V_LL_MC extends Kernel {

    private static final int KERNEL_BEST_LWS0 = 128;

    public CL1D_I_V_LL_MC() {
        super("CL1D_I_V_LL_MC");
    }

    @Override
    void runKernel(final CLImage2d<IntBuffer> imgA, final CLImage2d<IntBuffer> imgB,
            final CLBuffer<IntBuffer> facetData,
            final CLBuffer<FloatBuffer> facetCenters,
            final CLBuffer<FloatBuffer> deformations, final CLBuffer<FloatBuffer> results,
            final int deformationCount, final int imageWidth,
            final int facetSize, final int facetCount) {
        final int lws0 = KERNEL_BEST_LWS0;
        final int facetGlobalWorkSize = EngineMath.roundUp(lws0, deformationCount) * facetCount;

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
                .putArg(groupCountPerFacet);
        kernel.rewind();

        queue.put1DRangeKernel(kernel, 0, facetGlobalWorkSize, lws0);
    }

    @Override
    boolean usesMemoryCoalescing() {
        return true;
    }

}
