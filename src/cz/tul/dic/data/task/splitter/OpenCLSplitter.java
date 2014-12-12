package cz.tul.dic.data.task.splitter;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.engine.opencl.DeviceManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class OpenCLSplitter extends TaskSplitter {

    private static int COUNTER = 0;
    private static final long SIZE_INT = 4;
    private static final long SIZE_FLOAT = 4;
    private static final long SIZE_PIXEL = 4;
    private static final double COEFF_LIMIT_ADJUST = 0.75;
    private static final long COEFF_MEM_LIMIT_MAX = 6;
    private static final long COEFF_MEM_LIMIT_INIT = COEFF_MEM_LIMIT_MAX - 1;
    private static long COEFF_MEM_LIMIT = COEFF_MEM_LIMIT_INIT;
    private final int facetSize, ID;
    private final List<OpenCLSplitter> subSplitters;
    private final boolean subSplitter;
    private boolean hasNext;
    private int facetIndex;

    public OpenCLSplitter(Image image1, Image image2, List<Facet> facets, List<double[]> deformationLimits) {
        this(image1, image2, facets, deformationLimits, false);
    }

    private OpenCLSplitter(Image image1, Image image2, List<Facet> facets, List<double[]> deformationLimits, boolean subSplitter) {
        super(image1, image2, facets, deformationLimits);

        subSplitters = new LinkedList<>();
        this.subSplitter = subSplitter;
        if (!facets.isEmpty()) {
            facetSize = facets.get(0).getSize();
            checkIfHasNext();
        } else {
            facetSize = -1;
            hasNext = false;
        }

        ID = COUNTER++;        
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    private void checkIfHasNext() {
        hasNext = false;
        while (!subSplitters.isEmpty() && !hasNext) {
            if (subSplitters.get(0).hasNext()) {
                hasNext = true;
            } else {
                subSplitters.remove(0);
            }
        }

        if (!hasNext) {
            hasNext = facetIndex < facets.size();
        }
    }

    @Override
    public ComputationTask next() {
        final int deformationLimitsArraySize = deformationLimits.get(facetIndex).length;
        List<Facet> sublist = null;
        List<double[]> checkedDeformations = null;
        ComputationTask ct = null;
        if (!subSplitters.isEmpty()) {
            ct = subSplitters.get(0).next();
        } else {
            final int rest = facets.size() - facetIndex;

            int taskSize = rest;
            final List<int[]> deformationCounts = DeformationUtils.generateDeformationCounts(deformationLimits);
            final long deformaiontCount = DeformationUtils.findMaxDeformationCount(deformationCounts);
            while (taskSize > 1 && !isMemOk(deformaiontCount, taskSize, facetSize, deformationLimitsArraySize)) {
                taskSize *= COEFF_LIMIT_ADJUST;
            }

            if (taskSize == 1 && !isMemOk(deformaiontCount, taskSize, facetSize, deformationLimitsArraySize)) {
                sublist = new ArrayList<>(1);
                sublist.add(facets.get(facetIndex));

                final double[] oldLimits = deformationLimits.get(facetIndex);
                final int[] stepCounts = DeformationUtils.generateDeformationCounts(oldLimits);
                final int minIndex = findMinIndexBiggerThanZero(stepCounts);
                final int newStepCount = stepCounts[minIndex] / 2 - 1;
                final double midPoint = oldLimits[minIndex * 3] + newStepCount * oldLimits[minIndex * 3 + 2];

                double[] newLimits = new double[deformationLimitsArraySize];
                System.arraycopy(oldLimits, 0, newLimits, 0, deformationLimitsArraySize);
                newLimits[minIndex * 3 + 1] = midPoint;
                Logger.trace("{0} - {1}", Arrays.toString(oldLimits), Arrays.toString(newLimits));
                checkedDeformations = new ArrayList<>(1);
                checkedDeformations.add(newLimits);
                subSplitters.add(new OpenCLSplitter(image1, image2, sublist, checkedDeformations, true));

                newLimits = new double[deformationLimitsArraySize];
                System.arraycopy(oldLimits, 0, newLimits, 0, deformationLimitsArraySize);
                newLimits[minIndex * 3] = midPoint + oldLimits[minIndex * 3 + 2];
                Logger.trace("{0} - {1}", Arrays.toString(oldLimits), Arrays.toString(newLimits));
                checkedDeformations = new ArrayList<>(1);
                checkedDeformations.add(newLimits);
                subSplitters.add(new OpenCLSplitter(image1, image2, sublist, checkedDeformations, true));

                if (subSplitter) {
                    Logger.warn("Too many deformations in subtask, {0} generating subsplitters - {1}, {2}.", ID, subSplitters.get(0).ID, subSplitters.get(1).ID);
                } else {
                    Logger.warn("Too many deformations in task, {0} generating subsplitters - {1}, {2}.", ID, subSplitters.get(0).ID, subSplitters.get(1).ID);
                }
                ct = subSplitters.get(0).next();
            } else {
                checkedDeformations = new ArrayList<>(taskSize);
                sublist = new ArrayList<>(taskSize);
                final int facetCount = facets.size();

                int count = 0;
                while (count < taskSize && facetIndex < facetCount) {
                    sublist.add(facets.get(facetIndex));
                    checkedDeformations.add(deformationLimits.get(facetIndex));

                    count++;
                    facetIndex++;
                }
            }
        }

        checkIfHasNext();

        if (ct == null) {
            ct = new ComputationTask(image1, image2, sublist, checkedDeformations, subSplitter);
            if (subSplitter) {
                Logger.trace("{0} computing subtask {1}", ID, Arrays.toString(ct.getDeformationLimits().get(0)));
            } else if (sublist != null) {
                Logger.trace("{0} computing task with {1} facets.", ID, sublist.size());
            } else {
                Logger.error("NULL facet sublist !!!");
            }
        } else {
            if (subSplitters.isEmpty()) {
                ct.setSubtask(false);
                facetIndex++;
                checkIfHasNext();
            } else {
                ct.setSubtask(true);
            }
        }

        return ct;
    }

    private int findMinIndexBiggerThanZero(final int[] counts) {
        int min = Integer.MAX_VALUE, minPos = 0;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] < min && counts[i] > 1) {
                min = counts[i];
                minPos = i;
            }
        }
        return minPos;
    }

    private boolean isMemOk(final long deformationCount, final long facetCount, final long facetSize, final long deformationLimitsArraySize) {
        final long imageSize = image1.getHeight() * image1.getWidth() * SIZE_PIXEL * 2;
        final long deformationsSize = 2 * deformationLimitsArraySize * facetCount * SIZE_FLOAT;
        final long reserve = 32 * SIZE_INT;
        final long facetDataSize = facetSize * facetSize * 2 * SIZE_INT * facetCount;
        final long facetCentersSize = 2 * SIZE_FLOAT * facetCount;
        final long resultCount = facetCount * deformationCount;
        final long resultSize = resultCount * SIZE_FLOAT;
        final long fullSize = imageSize + deformationsSize + reserve + facetDataSize + facetCentersSize + resultSize;

        final long maxAllocMem = DeviceManager.getDevice().getMaxMemAllocSize();
        final long maxMem = DeviceManager.getDevice().getGlobalMemSize() / COEFF_MEM_LIMIT_MAX * COEFF_MEM_LIMIT;
        boolean result = fullSize >= 0 && fullSize <= maxMem;
        result &= resultCount <= Integer.MAX_VALUE;
        result &= facetCount <= Integer.MAX_VALUE;
        result &= deformationCount <= Integer.MAX_VALUE;
        result &= resultSize >= 0 && resultSize <= maxAllocMem;
        result &= imageSize >= 0 && imageSize <= maxAllocMem;
        result &= deformationsSize >= 0 && deformationsSize <= maxAllocMem;
        result &= facetDataSize >= 0 && facetDataSize <= maxAllocMem;
        result &= facetCentersSize >= 0 && facetCentersSize <= maxAllocMem;
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

    @Override
    public void resetTaskSize() {
        COEFF_MEM_LIMIT = COEFF_MEM_LIMIT_INIT;
        Logger.debug("Reseting task size full.");
    }

}
