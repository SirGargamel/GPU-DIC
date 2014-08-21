package cz.tul.dic.data.task.splitter;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.engine.opencl.DeviceManager;
import java.util.ArrayList;
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
    private final int defArrayLength, facetSize;
    private boolean hasNext;
    private int facetIndex, divPosition, divIndex;
    private double divStep;

    private boolean subDivision;

    public OpenCLSplitter(Image image1, Image image2, List<Facet> facets, double[] deformationLimits, ROI roi, final Object taskSplitValue) throws ComputationException {
        super(image1, image2, facets, deformationLimits, roi);

        if (taskSplitValue instanceof Object[]) {
            final Object[] vals = (Object[]) taskSplitValue;

            defArrayLength = (int) vals[0];
            facetSize = (int) vals[1];
        } else {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Illegal data passed to OpenCLSplitter");
        }

        checkIfHasNext();
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    private void checkIfHasNext() {
        hasNext = facetIndex < facets.size();
    }

    @Override
    public ComputationTask next() {
        final List<Facet> sublist;
        double[] checkedDeformations;
        if (subDivision) {
            divPosition++;

            sublist = new ArrayList<>(1);
            sublist.add(facets.get(facetIndex));

            checkedDeformations = new double[deformationLimits.length];
            System.arraycopy(deformationLimits, 0, checkedDeformations, 0, deformationLimits.length);

            checkedDeformations[divIndex * 3] += divPosition * divStep;
            checkedDeformations[divIndex * 3 + 1] = Math.min(checkedDeformations[divIndex * 3] + divStep, checkedDeformations[divIndex * 3 + 1]);

            if (checkedDeformations[divIndex * 3 + 1] == deformationLimits[divIndex * 3 + 1]) {
                subDivision = false;
                facetIndex++;
            }
        } else {
            final int rest = facets.size() - facetIndex;

            int taskSize = rest;
            final long l = DeformationUtils.calculateDeformationCount(deformationLimits);
            while (taskSize > 1 && !isMemOk(l, taskSize, facetSize, deformationLimits.length)) {
                taskSize *= COEFF_LIMIT_ADJUST;
            }

            if (taskSize == 1 && !isMemOk(l, taskSize, facetSize, defArrayLength)) {
                Logger.warn("Too many deformations to try, spliting facet task .");
                checkedDeformations = new double[deformationLimits.length];
                System.arraycopy(deformationLimits, 0, checkedDeformations, 0, deformationLimits.length);

                subDivision = true;
                divPosition = 0;
                int[] stepCounts = DeformationUtils.generateDeformationCounts(deformationLimits);
                divIndex = findMaxIndex(stepCounts);
                int stepCount = stepCounts[divIndex];

                int divCount = 1;
                int newStepCount;
                do {
                    if (divCount >= stepCounts[divIndex]) {
                        Logger.warn("Too much deformations, increasing step size.");
                        deformationLimits[divIndex * 3 + 2] *= 2;
                        stepCounts = DeformationUtils.generateDeformationCounts(deformationLimits);
                        divIndex = findMaxIndex(stepCounts);
                        stepCount = stepCounts[divIndex];
                        divCount = 1;
                        System.arraycopy(deformationLimits, 0, checkedDeformations, 0, deformationLimits.length);
                    }

                    divCount *= 2;
                    divCount = Math.min(divCount, stepCounts[divIndex]);
                    newStepCount = (int) Math.ceil(stepCount / (double) divCount);
                    divStep = newStepCount * deformationLimits[divIndex * 3 + 2];
                    checkedDeformations[divIndex * 3 + 1] = deformationLimits[divIndex * 3] + divStep;
                } while (!isMemOk(DeformationUtils.calculateDeformationCount(checkedDeformations), rest, facetSize, defArrayLength));

                sublist = new ArrayList<>(1);
                sublist.add(facets.get(facetIndex));
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

        return new ComputationTask(image1, image2, sublist, checkedDeformations, subDivision);
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
