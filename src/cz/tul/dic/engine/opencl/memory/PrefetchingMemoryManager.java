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
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.opencl.kernels.Kernel;
import cz.tul.dic.engine.opencl.kernels.info.KernelInfo;
import cz.tul.dic.engine.opencl.kernels.KernelManager;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pmw.tinylog.Logger;

public class PrefetchingMemoryManager extends AbstractOpenCLMemoryManager {

    private final Map<Image, CLMemory<ByteBuffer>> imageBuffer;
    private List<AbstractSubset> subsets;
    private List<double[]> deformationLimits;
    private List<long[]> deformationCounts;
    private boolean inited;
    private TaskContainer task;

    public PrefetchingMemoryManager() {
        imageBuffer = new HashMap<>();
        inited = false;
    }

    @Override
    public void assignDataToGPU(final ComputationTask task, final Kernel kernel) throws ComputationException {
        try {
            if (KernelManager.isInited()) {
                if (!inited) {
                    init(this.task);
                }

                clImageA = imageBuffer.get(task.getImageA());
                clImageB = imageBuffer.get(task.getImageB());
            } else {
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

    @Override
    public void assignTask(final TaskContainer task) {
        for (CLMemory<ByteBuffer> m : imageBuffer.values()) {
            release(m);
        }
        imageBuffer.clear();

        if (KernelManager.isInited()) {
            init(task);
        } else {
            inited = false;
        }
        this.task = task;
    }

    private void init(final TaskContainer task) {
        final KernelInfo kt = (KernelInfo) task.getParameter(TaskParameter.KERNEL);
        final Kernel k = Kernel.createInstance(kt, this);
        switch (k.getKernelInfo().getInput()) {
            case IMAGE:
                generateImagesAsImage2Dt(task.getImages());
                break;
            case ARRAY:
                generateImagesAsArray(task.getImages());
                break;
            default:
                throw new IllegalArgumentException("Unsupported type of input - " + k.getKernelInfo().getInput());
        }

        inited = true;
    }

    private void generateImagesAsImage2Dt(final List<Image> images) {
        CLMemory<ByteBuffer> clImage;
        for (Image image : images) {
            clImage = generateImage2d(image);
            queue.putWriteImage((CLImage2d<?>) clImage, false);
            imageBuffer.put(image, clImage);
        }
    }

    private void generateImagesAsArray(final List<Image> images) {
        CLMemory<ByteBuffer> clImage;
        for (Image image : images) {
            clImage = generateImageArray(image);
            queue.putWriteBuffer((CLBuffer<?>) clImage, false);
            imageBuffer.put(image, clImage);
        }
    }

}
