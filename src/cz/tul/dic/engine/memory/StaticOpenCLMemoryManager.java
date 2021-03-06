/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.memory;

import com.jogamp.opencl.CLBuffer;
import com.jogamp.opencl.CLImage2d;
import com.jogamp.opencl.CLMemory;
import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.engine.opencl.kernel.OpenCLKernel;
import cz.tul.dic.engine.KernelInfo;
import java.util.List;

public class StaticOpenCLMemoryManager extends AbstractOpenCLMemoryManager {

    @Override
    public void assignDataToGPU(final ComputationTask task, final OpenCLKernel kernel) throws ComputationException {
        try {
            release(clImageA);
            release(clImageB);
            switch (kernel.getKernelInfo().getInput()) {
                case IMAGE:
                    clImageA = generateImage2d(task.getImageA());
                    queue.putWriteImage((CLImage2d<?>) clImageA, false);
                    clImageB = generateImage2d(task.getImageB());
                    queue.putWriteImage((CLImage2d<?>) clImageB, false);
                    break;
                case ARRAY:
                    clImageA = generateImageArray(task.getImageA());
                    queue.putWriteBuffer((CLBuffer<?>) clImageA, false);
                    clImageB = generateImageArray(task.getImageB());
                    queue.putWriteBuffer((CLBuffer<?>) clImageB, false);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type of input - " + kernel.getKernelInfo().getInput());
            }

            release(clSubsetData);
            release(clSubsetCenters);
            release(clSubsetWeights);
            clSubsetData = generateSubsetData(task.getSubsets(), kernel.getKernelInfo().getMemoryCoalescing() == KernelInfo.MemoryCoalescing.YES);
            queue.putWriteBuffer(clSubsetData, false);
            clSubsetCenters = generateSubsetCenters(task.getSubsets());
            queue.putWriteBuffer(clSubsetCenters, false);
            clSubsetWeights = generateSubsetWeights(task.getSubsetWeights());
            queue.putWriteBuffer(clSubsetWeights, false);

            release(clDeformations);
            release(clDefStepCount);
            clDeformations = generateDeformationLimits(task.getDeformations());
            queue.putWriteBuffer(clDeformations, false);
            final List<long[]> deformationCounts = DeformationUtils.generateDeformationCounts(task.getDeformations());
            clDefStepCount = generateDeformationStepCounts(deformationCounts);
            queue.putWriteBuffer(clDefStepCount, false);

            release(clResults);
            maxDeformationCount = DeformationUtils.findMaxDeformationCount(task.getDeformations(), task.getOrder(), task.usesLimits());
            final long size = task.getSubsets().size() * maxDeformationCount;
            if (size <= 0 || size >= Integer.MAX_VALUE) {
                throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, "Illegal size of resulting array - " + size);
            }
            clResults = context.createFloatBuffer((int) size, CLMemory.Mem.READ_WRITE);
        } catch (OutOfMemoryError e) {
            throw new ComputationException(ComputationExceptionCause.MEMORY_ERROR, e);
        }
    }

    @Override
    public void assignTask(TaskContainer task) {
        // do nothing, data are copied right before computation
    }

}
