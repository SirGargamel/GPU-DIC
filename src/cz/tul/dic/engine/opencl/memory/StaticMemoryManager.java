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
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.engine.opencl.kernels.Kernel;
import java.util.List;
import org.pmw.tinylog.Logger;

public class StaticMemoryManager extends AbstractOpenCLMemoryManager {

    @Override
    public void assignDataToGPU(final ComputationTask task, final Kernel kernel) throws ComputationException {
        try {
            release(clImageA);
            release(clImageB);
            if (kernel.getKernelInfo().usesImage()) {
                clImageA = generateImage2d(task.getImageA());
                queue.putWriteImage((CLImage2d<?>) clImageA, false);
                clImageB = generateImage2d(task.getImageB());
                queue.putWriteImage((CLImage2d<?>) clImageB, false);
            } else {
                clImageA = generateImageArray(task.getImageA());
                queue.putWriteBuffer((CLBuffer<?>) clImageA, false);
                clImageB = generateImageArray(task.getImageB());
                queue.putWriteBuffer((CLBuffer<?>) clImageB, false);
            }
            
            release(clSubsetData);
            release(clSubsetCenters);
            clSubsetData = generateSubsetData(task.getSubsets(), kernel.usesMemoryCoalescing());
            queue.putWriteBuffer(clSubsetData, false);
            clSubsetCenters = generateSubsetCenters(task.getSubsets());
            queue.putWriteBuffer(clSubsetCenters, false);
            
            release(clDeformationLimits);
            release(clDefStepCount);
            clDeformationLimits = generateDeformationLimits(task.getDeformationLimits());
            queue.putWriteBuffer(clDeformationLimits, false);
            final List<long[]> deformationCounts = DeformationUtils.generateDeformationCounts(task.getDeformationLimits());
            clDefStepCount = generateDeformationStepCounts(deformationCounts);
            queue.putWriteBuffer(clDefStepCount, false);
            
            release(clResults);
            maxDeformationCount = DeformationUtils.findMaxDeformationCount(deformationCounts);
            final long size = task.getSubsets().size() * maxDeformationCount;
            if (size <= 0 || size >= Integer.MAX_VALUE) {
                throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, "Illegal size of resulting array - " + size);
            }
            clResults = context.createFloatBuffer((int) size, CLMemory.Mem.READ_WRITE);
        } catch (OutOfMemoryError e) {
            Logger.debug(e);
            throw new ComputationException(ComputationExceptionCause.MEMORY_ERROR, e.getLocalizedMessage());
        }
    }

    @Override
    public void assignTask(TaskContainer task) {
        // do nothing, data are copied right before computation
    }

}
