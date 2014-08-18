package cz.tul.dic.engine.opencl;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLImage2d;
import cz.tul.dic.engine.EngineMath;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class CL2DImage extends Kernel {

    private static final int KERNEL_BEST_LWS0 = 1;
    private static final int KERNEL_BEST_LWS1 = 128;

    public CL2DImage() {
        super("CL2DImage");
    }

    @Override
    void runKernel(final CLImage2d<IntBuffer> imgA, final CLImage2d<IntBuffer> imgB,
            final CLBuffer<IntBuffer> facetData,
            final CLBuffer<FloatBuffer> facetCenters,
            final CLBuffer<FloatBuffer> deformationLimits, final CLBuffer<IntBuffer> defStepCounts,
            final CLBuffer<FloatBuffer> results,
            final int deformationCount, final int imageWidth,
            final int facetSize, final int facetCount) {
        final int lws0 = KERNEL_BEST_LWS0;
        final int lws1 = KERNEL_BEST_LWS1;
        final int facetGlobalWorkSize = EngineMath.roundUp(lws0, facetCount);
        final int deformationsGlobalWorkSize = EngineMath.roundUp(lws1, deformationCount);

        kernel.rewind();
        kernel.putArgs(imgA, imgB, facetData, facetCenters, deformationLimits, defStepCounts, results)
                .putArg(imageWidth)
                .putArg(deformationCount)
                .putArg(facetSize)
                .putArg(facetCount);
        kernel.rewind();

        queue.put2DRangeKernel(kernel, 0, 0, facetGlobalWorkSize, deformationsGlobalWorkSize, lws0, lws1);
    }

    @Override
    boolean usesMemoryCoalescing() {
        return false;
    }

    @Override
    boolean usesVectorization() {
        return false;
    }

}
