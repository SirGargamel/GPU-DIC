/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task.splitter;

import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.subset.SubsetUtils;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.FullTask;
import cz.tul.dic.engine.opencl.DeviceManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class OpenCLSplitter extends AbstractTaskSplitter {

    private static int COUNTER = 0;
    private static final long SIZE_INT = 4;
    private static final long SIZE_FLOAT = 4;
    private static final long SIZE_PIXEL = 4;
    private static final double COEFF_LIMIT_ADJUST = 0.75;
    private static final long COEFF_MEM_LIMIT_MAX = 6;
    private static final long COEFF_MEM_LIMIT_INIT = COEFF_MEM_LIMIT_MAX - 1;
    private static long COEFF_MEM_LIMIT = COEFF_MEM_LIMIT_INIT;
    private final int subsetSize, splitterId;
    private final List<OpenCLSplitter> subSplitters;
    private final boolean subSplitter;
    private boolean hasNextElement;
    private int subsetIndex;

    public OpenCLSplitter(final FullTask task) {
        this(task, false);
    }

    private OpenCLSplitter(final FullTask task, boolean subSplitter) {
        super(task);

        subSplitters = new LinkedList<>();
        this.subSplitter = subSplitter;
        if (!subsets.isEmpty()) {
            subsetSize = subsets.get(0).getSize();
            checkIfHasNext();
        } else {
            subsetSize = -1;
            hasNextElement = false;
        }

        splitterId = COUNTER++;
        if (COEFF_MEM_LIMIT < COEFF_MEM_LIMIT_INIT) {
            COEFF_MEM_LIMIT++;
            Logger.debug("Increasing task size to {0} / {1}.", COEFF_MEM_LIMIT, COEFF_MEM_LIMIT_MAX);
        }
    }

    @Override
    public boolean hasNext() {
        return hasNextElement;
    }

    private void checkIfHasNext() {
        hasNextElement = false;
        while (!subSplitters.isEmpty() && !hasNextElement) {
            if (subSplitters.get(0).hasNext()) {
                hasNextElement = true;
            } else {
                subSplitters.remove(0);
            }
        }

        if (!hasNextElement) {
            hasNextElement = subsetIndex < subsets.size();
        }
    }

    @Override
    public ComputationTask next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        final int deformationLimitsArraySize = deformationLimits.get(subsetIndex).length;
        List<AbstractSubset> sublist = null;
        List<double[]> checkedDeformations = null;
        ComputationTask ct = null;
        if (!subSplitters.isEmpty()) {
            ct = subSplitters.get(0).next();
        } else {
            final int rest = subsets.size() - subsetIndex;

            int taskSize = rest;
            final List<long[]> deformationCounts = DeformationUtils.generateDeformationCounts(deformationLimits);
            final long deformaiontCount = DeformationUtils.findMaxDeformationCount(deformationCounts);
            while (taskSize > 1 && !isMemOk(deformaiontCount, taskSize, subsetSize, deformationLimitsArraySize)) {
                taskSize *= COEFF_LIMIT_ADJUST;
            }

            if (taskSize == 1 && !isMemOk(deformaiontCount, taskSize, subsetSize, deformationLimitsArraySize)) {
                sublist = new ArrayList<>(1);
                sublist.add(subsets.get(subsetIndex));

                final double[] oldLimits = deformationLimits.get(subsetIndex);
                final long[] stepCounts = DeformationUtils.generateDeformationCounts(oldLimits);
                final int minIndex = findMinIndexBiggerThanZero(stepCounts);
                final long newStepCount = stepCounts[minIndex] / 2 - 1;
                final double midPoint = oldLimits[minIndex * 3] + newStepCount * oldLimits[minIndex * 3 + 2];

                double[] newLimits = new double[deformationLimitsArraySize];
                System.arraycopy(oldLimits, 0, newLimits, 0, deformationLimitsArraySize);
                newLimits[minIndex * 3 + 1] = midPoint;
                Logger.trace("{0} - {1}", Arrays.toString(oldLimits), Arrays.toString(newLimits));
                checkedDeformations = new ArrayList<>(1);
                checkedDeformations.add(newLimits);
                subSplitters.add(new OpenCLSplitter(new FullTask(image1, image2, sublist, checkedDeformations), true));

                newLimits = new double[deformationLimitsArraySize];
                System.arraycopy(oldLimits, 0, newLimits, 0, deformationLimitsArraySize);
                newLimits[minIndex * 3] = midPoint + oldLimits[minIndex * 3 + 2];
                Logger.trace("{0} - {1}", Arrays.toString(oldLimits), Arrays.toString(newLimits));
                checkedDeformations = new ArrayList<>(1);
                checkedDeformations.add(newLimits);
                subSplitters.add(new OpenCLSplitter(new FullTask(image1, image2, sublist, checkedDeformations), true));

                if (subSplitter) {
                    Logger.warn("Too many deformations in subtask, {0} generating subsplitters - {1}, {2}.", splitterId, subSplitters.get(0).splitterId, subSplitters.get(1).splitterId);
                } else {
                    Logger.warn("Too many deformations in task, {0} generating subsplitters - {1}, {2}.", splitterId, subSplitters.get(0).splitterId, subSplitters.get(1).splitterId);
                }
                ct = subSplitters.get(0).next();
            } else {
                checkedDeformations = new ArrayList<>(taskSize);
                sublist = new ArrayList<>(taskSize);
                final int subsetCount = subsets.size();

                int count = 0;
                while (count < taskSize && subsetIndex < subsetCount) {
                    sublist.add(subsets.get(subsetIndex));
                    checkedDeformations.add(deformationLimits.get(subsetIndex));

                    count++;
                    subsetIndex++;
                }
            }
        }

        checkIfHasNext();

        if (ct == null) {
            ct = new ComputationTask(image1, image2, sublist, checkedDeformations, subSplitter);
            if (subSplitter) {
                Logger.trace("{0} computing subtask {1}", splitterId, Arrays.toString(ct.getDeformationLimits().get(0)));
            } else if (sublist != null) {
                Logger.trace("{0} computing task with {1} subsets.", splitterId, sublist.size());
            } else {
                Logger.error("NULL subset sublist !!!");
            }
        } else {
            if (subSplitters.isEmpty()) {
                ct.setSubtask(false);
                subsetIndex++;
                checkIfHasNext();
            } else {
                ct.setSubtask(true);
            }
        }

        return ct;
    }

    private int findMinIndexBiggerThanZero(final long[] counts) {
        long min = Long.MAX_VALUE;
        int minPos = 0;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] < min && counts[i] > 1) {
                min = counts[i];
                minPos = i;
            }
        }
        return minPos;
    }

    private boolean isMemOk(final long deformationCount, final long subsetCount, final long subsetSize, final long deformationLimitsArraySize) {
        final long imageSize = image1.getHeight() * image1.getWidth() * SIZE_PIXEL * 2;
        final long deformationsSize = 2 * deformationLimitsArraySize * subsetCount * SIZE_FLOAT;
        final long reserve = 32 * SIZE_INT;
        final long subsetDataSize = SubsetUtils.computeSubsetCoordCount((int) subsetSize) * 2 * SIZE_INT * subsetCount;
        final long subsetCentersSize = 2 * SIZE_FLOAT * subsetCount;
        final long resultCount = subsetCount * deformationCount;
        final long resultSize = resultCount * SIZE_FLOAT;
        final long fullSize = imageSize + deformationsSize + reserve + subsetDataSize + subsetCentersSize + resultSize;

        final long maxAllocMem = DeviceManager.getDevice().getMaxMemAllocSize() / COEFF_MEM_LIMIT_MAX * COEFF_MEM_LIMIT;
        final long maxMem = DeviceManager.getDevice().getGlobalMemSize() / COEFF_MEM_LIMIT_MAX * COEFF_MEM_LIMIT;
        boolean result = fullSize >= 0 && fullSize <= maxMem;
        result &= resultCount <= Integer.MAX_VALUE;
        result &= subsetCount <= Integer.MAX_VALUE;
        result &= deformationCount <= Integer.MAX_VALUE;
        result &= resultSize >= 0 && resultSize <= maxAllocMem;
        result &= imageSize >= 0 && imageSize <= maxAllocMem;
        result &= deformationsSize >= 0 && deformationsSize <= maxAllocMem;
        result &= subsetDataSize >= 0 && subsetDataSize <= maxAllocMem;
        result &= subsetCentersSize >= 0 && subsetCentersSize <= maxAllocMem;
        return result;
    }

    @Override
    public void signalTaskSizeTooBig() {
        COEFF_MEM_LIMIT--;
        Logger.debug("Lowering task size to {0} / {1}.", COEFF_MEM_LIMIT, COEFF_MEM_LIMIT_MAX);
    }

    @Override
    public boolean isSplitterReady() {
        return COEFF_MEM_LIMIT > 0;
    }

}
