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
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.engine.opencl.kernel.Kernel;
import cz.tul.dic.engine.opencl.kernel.KernelInfo;
import java.util.List;

public class DynamicMemoryManager extends AbstractOpenCLMemoryManager {

    private Image imageA, imageB;
    private List<AbstractSubset> subsets;
    private List<Integer> subsetWeights;
    private List<double[]> deformationLimits;
    private List<long[]> deformationCounts;

    protected DynamicMemoryManager() {
    }

    @Override
    public void assignDataToGPU(final ComputationTask task, final Kernel kernel) throws ComputationException {
        try {
            if (task.getImageA() != imageA || clImageA.isReleased()) {
                release(clImageA);
                imageA = task.getImageA();

                if (task.getImageA() == imageB) {
                    clImageA = clImageB;
                } else {
                    switch (kernel.getKernelInfo().getInput()) {
                        case IMAGE:
                            clImageA = generateImage2d(task.getImageA());
                            queue.putWriteImage((CLImage2d<?>) clImageA, false);
                            break;
                        case ARRAY:
                            clImageA = generateImageArray(task.getImageA());
                            queue.putWriteBuffer((CLBuffer<?>) clImageA, false);
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported type of input - " + kernel.getKernelInfo().getInput());
                    }
                }
            }
            if (task.getImageB() != imageB || clImageB.isReleased()) {
                if (clImageA != clImageB) {
                    release(clImageB);
                }
                imageB = task.getImageB();

                switch (kernel.getKernelInfo().getInput()) {
                    case IMAGE:
                        clImageB = generateImage2d(task.getImageB());
                        queue.putWriteImage((CLImage2d<?>) clImageB, false);
                        break;
                    case ARRAY:
                        clImageB = generateImageArray(task.getImageB());
                        queue.putWriteBuffer((CLBuffer<?>) clImageB, false);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported type of input - " + kernel.getKernelInfo().getInput());
                }
            }

            boolean changedResults = false;
            if (task.getSubsets() != subsets || !task.getSubsets().equals(subsets) || clSubsetData.isReleased()) {
                release(clSubsetData);
                release(clSubsetCenters);
                subsets = task.getSubsets();

                clSubsetData = generateSubsetData(subsets, kernel.getKernelInfo().getMemoryCoalescing() == KernelInfo.MemoryCoalescing.YES);
                queue.putWriteBuffer(clSubsetData, false);

                clSubsetCenters = generateSubsetCenters(subsets);
                queue.putWriteBuffer(clSubsetCenters, false);

                changedResults = true;
            }
            if (task.getSubsetWeights() != subsetWeights || !task.getSubsetWeights().equals(subsetWeights) || clSubsetWeights.isReleased()) {
                release(clSubsetWeights);
                subsetWeights = task.getSubsetWeights();
                clSubsetWeights = generateSubsetWeights(subsetWeights);
                queue.putWriteBuffer(clSubsetWeights, false);
            }
            if (task.getDeformations() != deformationLimits || !task.getDeformations().equals(deformationLimits) || clDeformations.isReleased()) {
                release(clDeformations);
                release(clDefStepCount);
                deformationLimits = task.getDeformations();

                clDeformations = generateDeformationLimits(deformationLimits);
                queue.putWriteBuffer(clDeformations, false);

                deformationCounts = DeformationUtils.generateDeformationCounts(deformationLimits);
                clDefStepCount = generateDeformationStepCounts(deformationCounts);
                queue.putWriteBuffer(clDefStepCount, false);

                changedResults = true;
            }

            if (changedResults || clResults.isReleased()) {
                release(clResults);

                maxDeformationCount = DeformationUtils.findMaxDeformationCount(task.getDeformations(), task.getOrder(), task.usesLimits());
                final long size = task.getSubsets().size() * maxDeformationCount;
                if (size <= 0 || size >= Integer.MAX_VALUE) {
                    throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, "Illegal size of resulting array - " + size);
                }
                clResults = context.createFloatBuffer((int) size, CLMemory.Mem.READ_WRITE);
            }
        } catch (OutOfMemoryError e) {
            throw new ComputationException(ComputationExceptionCause.MEMORY_ERROR, e);
        }
    }

    @Override
    public void assignTask(TaskContainer task) {
        // do nothing, data are copied right before computation or reused from previous one 
    }

}
