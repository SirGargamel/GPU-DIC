/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task.splitter;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.subset.SubsetUtils;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.engine.opencl.DeviceManager;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 *
 * @author Petr Jecmen
 */
public class OpenCLSplitter extends AbstractTaskSplitter {

    private static final long SIZE_INT = 4;
    private static final long SIZE_FLOAT = 4;
    private static final long SIZE_PIXEL = 4;
    private static final double COEFF_LIMIT_ADJUST = 0.75;
    private final int subsetSize;
    private boolean hasNextElement;
    private int subsetIndex;

    public OpenCLSplitter(final ComputationTask task) {
        super(task);
        if (!subsets.isEmpty()) {
            subsetSize = subsets.get(0).getSize();
            checkIfHasNext();
        } else {
            subsetSize = -1;
            hasNextElement = false;
        }
    }

    @Override
    public boolean hasNext() {
        return hasNextElement;
    }

    private void checkIfHasNext() {
        hasNextElement = subsetIndex < subsets.size();
    }

    @Override
    public ComputationTask next() throws ComputationException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        final int deformationsArraySize = deformations.get(subsetIndex).length;
        List<AbstractSubset> sublistS = null;
        List<Integer> sublistW = null;
        List<double[]> checkedDeformations = null;
        final int rest = subsets.size() - subsetIndex;

        int taskSize = rest;
        final long deformationCount = DeformationUtils.findMaxDeformationCount(deformations, order, usesLimits);
        while (taskSize > 1 && !isMemOk(deformationCount, taskSize, subsetSize, deformationsArraySize)) {
            taskSize *= COEFF_LIMIT_ADJUST;
        }

        if (taskSize == 1 && !isMemOk(deformationCount, taskSize, subsetSize, deformationsArraySize)) {
            hasNextElement = false;
            throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, "Not enough GPU memory, too many deformations.");
        } else {
            checkedDeformations = new ArrayList<>(taskSize);
            sublistS = new ArrayList<>(taskSize);
            sublistW = new ArrayList<>(taskSize);
            final int subsetCount = subsets.size();

            int count = 0;
            while (count < taskSize && subsetIndex < subsetCount) {
                sublistS.add(subsets.get(subsetIndex));
                sublistW.add(subsetWeights.get(subsetIndex));
                checkedDeformations.add(deformations.get(subsetIndex));

                count++;
                subsetIndex++;
            }
        }

        checkIfHasNext();

        return new ComputationTask(image1, image2, sublistS, sublistW, checkedDeformations, order, usesLimits);
    }

    private boolean isMemOk(final long deformationCount, final long subsetCount, final long subsetSize, final long deformationsArraySize) {
        final long imageSize = image1.getHeight() * image1.getWidth() * SIZE_PIXEL * 2;
        final long deformationsSize = deformationsArraySize * subsetCount * SIZE_FLOAT;
        final long reserve = 32 * SIZE_INT;
        final long subsetDataSize = SubsetUtils.computeSubsetCoordCount((int) subsetSize) * 2 * SIZE_INT * subsetCount;
        final long subsetCentersSize = 2 * SIZE_FLOAT * subsetCount;
        final long subsetWeightsSize = SIZE_INT * subsetCount;
        final long resultCount = subsetCount * deformationCount;
        final long resultSize = resultCount * SIZE_FLOAT;
        final long fullSize = imageSize + deformationsSize + subsetDataSize + subsetCentersSize + subsetWeightsSize + resultSize + reserve;

        final long maxAllocMem = DeviceManager.getDevice().getMaxMemAllocSize();
        final long maxMem = DeviceManager.getDevice().getGlobalMemSize();
        boolean result = fullSize >= 0 && fullSize <= maxMem;
        result &= resultCount <= Integer.MAX_VALUE;
        result &= subsetCount <= Integer.MAX_VALUE;
        result &= deformationCount <= Integer.MAX_VALUE;
        result &= resultSize >= 0 && resultSize <= maxAllocMem;
        result &= imageSize >= 0 && imageSize <= maxAllocMem;
        result &= deformationsSize >= 0 && deformationsSize <= maxAllocMem;
        result &= subsetDataSize >= 0 && subsetDataSize <= maxAllocMem;
        result &= subsetCentersSize >= 0 && subsetCentersSize <= maxAllocMem;
        result &= subsetWeightsSize >= 0 && subsetWeightsSize <= maxAllocMem;
        return result;
    }

}
