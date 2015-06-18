/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.memory;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLMemory;
import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.engine.opencl.kernels.Kernel;
import java.nio.IntBuffer;
import java.util.List;
import org.pmw.tinylog.Logger;

public class StaticMemoryManager extends AbstractOpenCLMemoryManager {

    @Override
    public void assignDataToGPU(final ComputationTask task, final Kernel kernel) throws ComputationException {
        try {
            release(clImageA);
            release(clImageB);
            if (kernel.usesImage()) {
                clImageA = generateImage2d(task.getImageA());
                queue.putWriteImage((CLImage2d<IntBuffer>) clImageA, false);
                clImageB = generateImage2d(task.getImageB());
                queue.putWriteImage((CLImage2d<IntBuffer>) clImageB, false);
            } else {
                clImageA = generateImageArray(task.getImageA());
                queue.putWriteBuffer((CLBuffer<IntBuffer>) clImageA, false);
                clImageB = generateImageArray(task.getImageB());
                queue.putWriteBuffer((CLBuffer<IntBuffer>) clImageB, false);
            }
            
            release(clFacetData);
            release(clFacetCenters);
            clFacetData = generateFacetData(task.getFacets(), kernel.usesMemoryCoalescing());
            queue.putWriteBuffer(clFacetData, false);
            clFacetCenters = generateFacetCenters(task.getFacets());
            queue.putWriteBuffer(clFacetCenters, false);
            
            release(clDeformationLimits);
            release(clDefStepCount);
            clDeformationLimits = generateDeformationLimits(task.getDeformationLimits());
            queue.putWriteBuffer(clDeformationLimits, false);
            final List<int[]> deformationCounts = DeformationUtils.generateDeformationCounts(task.getDeformationLimits());
            clDefStepCount = generateDeformationStepCounts(deformationCounts);
            queue.putWriteBuffer(clDefStepCount, false);
            
            release(clResults);
            maxDeformationCount = DeformationUtils.findMaxDeformationCount(deformationCounts);
            final long size = task.getFacets().size() * maxDeformationCount;
            if (size <= 0 || size >= Integer.MAX_VALUE) {
                throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, "Illegal size of resulting array - " + size);
            }
            clResults = context.createFloatBuffer((int) size, CLMemory.Mem.READ_WRITE);
        } catch (OutOfMemoryError e) {
            Logger.debug(e);
            throw new ComputationException(ComputationExceptionCause.MEMORY_ERROR, e.getLocalizedMessage());
        }
    }

}
