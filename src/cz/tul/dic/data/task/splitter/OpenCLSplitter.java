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

    private static final long SIZE_INT = 4;
    private static final long SIZE_FLOAT = 4;
    private static final long SIZE_PIXEL = 4;
    private static final double COEFF_LIMIT_ADJUST = 0.75;
    private final int facetSize, ID;
    private final List<OpenCLSplitter> subSplitters;
    private boolean hasNext;
    private int facetIndex, divPosition, divIndex;
    private double divStep;

    private boolean subDivision;

    public OpenCLSplitter(Image image1, Image image2, List<Facet> facets, double[] deformationLimits) {
        super(image1, image2, facets, deformationLimits);

        subSplitters = new LinkedList<>();
        if (facets.isEmpty()) {
            facetSize = facets.get(0).getSize();
            checkIfHasNext();
        } else {
            facetSize = -1;
            hasNext = false;
        }

        ID = (int) (Integer.MAX_VALUE * Math.random());
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    private void checkIfHasNext() {
        if (subSplitters.isEmpty()) {
            hasNext = facetIndex < facets.size();
        } else {
            hasNext = false;
            while (!subSplitters.isEmpty() && !hasNext) {
                if (subSplitters.get(0).hasNext()) {
                    hasNext = true;
                } else {
                    subSplitters.remove(0);
                }
            }

        }
    }

    @Override
    public ComputationTask next() {
        final int deformationLimitsArraySize = deformationLimits.length;
        List<Facet> sublist = null;
        double[] checkedDeformations = null;
        ComputationTask ct = null;
        if (subDivision) {
            if (!subSplitters.isEmpty()) {
                ct = subSplitters.get(0).next();
            } else {
                divPosition++;

                sublist = new ArrayList<>(1);
                sublist.add(facets.get(facetIndex));

                checkedDeformations = new double[deformationLimitsArraySize];
                System.arraycopy(deformationLimits, 0, checkedDeformations, 0, deformationLimitsArraySize);

                checkedDeformations[divIndex * 3] += divPosition * divStep;
                checkedDeformations[divIndex * 3 + 1] = Math.min(checkedDeformations[divIndex * 3] + divStep, checkedDeformations[divIndex * 3 + 1]);

                if (checkedDeformations[divIndex * 3 + 1] == deformationLimits[divIndex * 3 + 1]) {
                    subDivision = false;
                    facetIndex++;
                }
            }
        } else {
            final int rest = facets.size() - facetIndex;

            int taskSize = rest;
            final long l = DeformationUtils.calculateDeformationCount(deformationLimits);
            while (taskSize > 1 && !isMemOk(l, taskSize, facetSize, deformationLimitsArraySize)) {
                taskSize *= COEFF_LIMIT_ADJUST;
            }

            if (taskSize == 1 && !isMemOk(l, taskSize, facetSize, deformationLimitsArraySize)) {
                Logger.warn("Too many deformations to try, spliting facet task .");
                checkedDeformations = new double[deformationLimitsArraySize];
                System.arraycopy(deformationLimits, 0, checkedDeformations, 0, deformationLimitsArraySize);

                subDivision = true;
                divPosition = 0;
                int[] stepCounts = DeformationUtils.generateDeformationCounts(deformationLimits);
                divIndex = findMaxIndex(stepCounts);
                int stepCount = stepCounts[divIndex];

                sublist = new ArrayList<>(1);
                sublist.add(facets.get(facetIndex));

                int divCount = 1;
                int newStepCount;
                do {
                    if (divCount >= stepCounts[divIndex]) {
                        Logger.warn("Too many deformations, generating subsplitters.");
                        final int minIndex = findMinIndexBiggerThanZero(stepCounts);
                        newStepCount = stepCounts[minIndex] / 2 - 1;
                        final double midPoint = deformationLimits[minIndex * 3] + newStepCount * deformationLimits[minIndex * 3 + 2];
                        checkedDeformations = new double[deformationLimitsArraySize];
                        System.arraycopy(deformationLimits, 0, checkedDeformations, 0, deformationLimitsArraySize);
                        checkedDeformations[minIndex * 3 + 1] = midPoint;
                        Logger.trace(Arrays.toString(deformationLimits));
                        Logger.trace(Arrays.toString(checkedDeformations));
                        subSplitters.add(new OpenCLSplitter(image1, image2, sublist, checkedDeformations));
                        checkedDeformations = new double[deformationLimitsArraySize];
                        System.arraycopy(deformationLimits, 0, checkedDeformations, 0, deformationLimitsArraySize);
                        checkedDeformations[minIndex * 3] = midPoint + deformationLimits[minIndex * 3 + 2];
                        Logger.trace(Arrays.toString(checkedDeformations));
                        subSplitters.add(new OpenCLSplitter(image1, image2, sublist, checkedDeformations));
                        Logger.trace("--- " + ID + " splits into " + subSplitters.get(0).ID + "; " + subSplitters.get(1).ID);

                        ct = subSplitters.get(0).next();
                        break;
                    }

                    divCount *= 2;
                    divCount = Math.min(divCount, stepCounts[divIndex]);
                    newStepCount = (int) Math.ceil(stepCount / (double) divCount);
                    divStep = newStepCount * deformationLimits[divIndex * 3 + 2];
                    checkedDeformations[divIndex * 3 + 1] = deformationLimits[divIndex * 3] + divStep;
                } while (!isMemOk(DeformationUtils.calculateDeformationCount(checkedDeformations), rest, facetSize, deformationLimitsArraySize));
            } else {
                checkedDeformations = deformationLimits;
                sublist = new ArrayList<>(taskSize);
                final int facetCount = facets.size();

                int count = 0;
                while (count < taskSize && facetIndex < facetCount) {
                    sublist.add(facets.get(facetIndex));

                    count++;
                    facetIndex++;
                }
            }
        }

        checkIfHasNext();

        if (ct == null) {
            ct = new ComputationTask(image1, image2, sublist, checkedDeformations, subDivision);
            Logger.trace(ID + " computing " + Arrays.toString(ct.getDeformationLimits()));
        }
        return ct;
    }

    private int findMaxIndex(final int[] counts) {
        int max = -1, maxPos = 0;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > max) {
                max = counts[i];
                maxPos = i;
            }
        }
        return maxPos;
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

    private boolean isMemOk(final long deformationCount, final long facetCount, final long facetSize, final long deformationArraySize) {
        final long imageSize = image1.getHeight() * image1.getWidth() * SIZE_PIXEL * 2;
        final long deformationsSize = deformationArraySize * SIZE_FLOAT;
        final long reserve = 32 * SIZE_INT;
        final long facetDataSize = facetSize * facetSize * 2 * SIZE_INT * facetCount;
        final long facetCentersSize = 2 * SIZE_FLOAT * facetCount;
        final long resultCount = facetCount * deformationCount;
        final long resultSize = resultCount * SIZE_FLOAT;
        final long fullSize = imageSize + deformationsSize + reserve + facetDataSize + facetCentersSize + resultSize;

        final long maxAllocMem = DeviceManager.getDevice().getMaxMemAllocSize();
        final long maxMem = DeviceManager.getDevice().getGlobalMemSize();
        boolean result = fullSize >= 0 && fullSize <= maxMem;
        result &= resultCount <= Integer.MAX_VALUE;
        result &= facetCount <= Integer.MAX_VALUE;
        result &= deformationCount <= Integer.MAX_VALUE;
        result &= resultSize >= 0 && resultSize <= maxAllocMem;
        result &= imageSize >= 0 && imageSize <= maxAllocMem;
        result &= deformationsSize >= 0 && deformationsSize <= maxAllocMem;
        result &= facetDataSize >= 0 && facetDataSize <= maxAllocMem;
        result &= facetCentersSize >= 0 && facetCentersSize <= maxAllocMem;

//        System.out.print(result + " - ");
//        System.out.print(Arrays.toString(new long[]{deformations.length / deformationArraySize, facetCount}));
//        System.out.print(" - ");
//        System.out.println(Arrays.toString(new long[]{deformationsSize, facetDataSize, facetCentersSize, facetCentersSize, resultSize, fullSize, maxAllocMem, maxMem}));
        return result;
    }

}
