package cz.tul.dic.engine.opencl.memory;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLMemory;
import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.engine.opencl.kernels.Kernel;
import java.nio.IntBuffer;
import java.util.List;

public class StaticMemoryManager extends OpenCLMemoryManager {

    public StaticMemoryManager() {
    }

    @Override
    public void assignDataToGPU(Image imageA, Image imageB, List<Facet> facets, List<double[]> deformationLimits, Kernel kernel) throws ComputationException {
        try {
            release(clImageA);
            release(clImageB);
            if (kernel.usesImage()) {
                clImageA = generateImage2d_t(imageA);
                queue.putWriteImage((CLImage2d<IntBuffer>) clImageA, false);
                clImageB = generateImage2d_t(imageB);
                queue.putWriteImage((CLImage2d<IntBuffer>) clImageB, false);
            } else {
                clImageA = generateImageArray(imageA);
                queue.putWriteBuffer((CLBuffer<IntBuffer>) clImageA, false);
                clImageB = generateImageArray(imageB);
                queue.putWriteBuffer((CLBuffer<IntBuffer>) clImageB, false);
            }

            release(clFacetData);
            release(clFacetCenters);
            clFacetData = generateFacetData(facets, kernel.usesMemoryCoalescing());
            queue.putWriteBuffer(clFacetData, false);
            clFacetCenters = generateFacetCenters(facets);
            queue.putWriteBuffer(clFacetCenters, false);

            release(clDeformationLimits);
            release(clDefStepCount);
            clDeformationLimits = generateDeformationLimits(deformationLimits);
            queue.putWriteBuffer(clDeformationLimits, false);
            final List<int[]> deformationCounts = DeformationUtils.generateDeformationCounts(deformationLimits);
            clDefStepCount = generateDeformationStepCounts(deformationCounts);
            queue.putWriteBuffer(clDefStepCount, false);

            release(clResults);
            maxDeformationCount = DeformationUtils.findMaxDeformationCount(deformationCounts);
            final long size = facets.size() * maxDeformationCount;
            if (size <= 0 || size >= Integer.MAX_VALUE) {
                throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, "Illegal size of resulting array - " + size);
            }
            clResults = context.createFloatBuffer((int) size, CLMemory.Mem.READ_WRITE);
        } catch (OutOfMemoryError e) {
            throw new ComputationException(ComputationExceptionCause.MEMORY_ERROR, e.getLocalizedMessage());
        }
    }

}
