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
import cz.tul.dic.engine.opencl.kernels.Kernel;
import java.nio.IntBuffer;
import java.util.List;
import org.pmw.tinylog.Logger;

public class DynamicMemoryManager extends AbstractOpenCLMemoryManager {

    private Image imageA, imageB;
    private List<AbstractSubset> subsets;
    private List<double[]> deformationLimits;
    private List<long[]> deformationCounts;

    @Override
    public void assignDataToGPU(final ComputationTask task, final Kernel kernel) throws ComputationException {
        try {
            if (task.getImageA() != imageA || clImageA.isReleased()) {
                release(clImageA);
                imageA = task.getImageA();

                if (task.getImageA() == imageB) {
                    clImageA = clImageB;
                } else {
                    if (kernel.usesImage()) {
                        clImageA = generateImage2d(imageA);
                        queue.putWriteImage((CLImage2d<IntBuffer>) clImageA, false);
                    } else {
                        clImageA = generateImageArray(imageA);
                        queue.putWriteBuffer((CLBuffer<IntBuffer>) clImageA, false);
                    }
                }
            }
            if (task.getImageB() != imageB || clImageB.isReleased()) {
                if (clImageA != clImageB) {
                    release(clImageB);
                }
                imageB = task.getImageB();

                if (kernel.usesImage()) {
                    clImageB = generateImage2d(imageB);
                    queue.putWriteImage((CLImage2d<IntBuffer>) clImageB, false);
                } else {
                    clImageB = generateImageArray(imageB);
                    queue.putWriteBuffer((CLBuffer<IntBuffer>) clImageB, false);
                }
            }

            boolean changedResults = false;
            if (task.getSubsets() != subsets || !task.getSubsets().equals(subsets) || clSubsetData.isReleased()) {
                release(clSubsetData);
                release(clSubsetCenters);
                subsets = task.getSubsets();

                clSubsetData = generateSubsetData(subsets, kernel.usesMemoryCoalescing());
                queue.putWriteBuffer(clSubsetData, false);

                clSubsetCenters = generateSubsetCenters(subsets);
                queue.putWriteBuffer(clSubsetCenters, false);

                changedResults = true;
            }
            if (task.getDeformationLimits() != deformationLimits || !task.getDeformationLimits().equals(deformationLimits) || clDeformationLimits.isReleased()) {
                release(clDeformationLimits);
                release(clDefStepCount);
                deformationLimits = task.getDeformationLimits();

                clDeformationLimits = generateDeformationLimits(deformationLimits);
                queue.putWriteBuffer(clDeformationLimits, false);

                deformationCounts = DeformationUtils.generateDeformationCounts(deformationLimits);
                clDefStepCount = generateDeformationStepCounts(deformationCounts);
                queue.putWriteBuffer(clDefStepCount, false);

                changedResults = true;
            }

            if (changedResults || clResults.isReleased()) {
                release(clResults);

                maxDeformationCount = DeformationUtils.findMaxDeformationCount(deformationCounts);
                final long size = task.getSubsets().size() * maxDeformationCount;
                if (size <= 0 || size >= Integer.MAX_VALUE) {
                    throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, "Illegal size of resulting array - " + size);
                }
                clResults = context.createFloatBuffer((int) size, CLMemory.Mem.READ_WRITE);
            }
        } catch (OutOfMemoryError e) {
            Logger.debug(e);
            throw new ComputationException(ComputationExceptionCause.MEMORY_ERROR, e.getLocalizedMessage());
        }
    }

}
