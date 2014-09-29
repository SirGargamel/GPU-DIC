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
    private final boolean subSplitter;
    private boolean hasNext;
    private int facetIndex;

    public OpenCLSplitter(Image image1, Image image2, List<Facet> facets, double[] deformationLimits) {
        this(image1, image2, facets, deformationLimits, false);
    }

    private OpenCLSplitter(Image image1, Image image2, List<Facet> facets, double[] deformationLimits, boolean subSplitter) {
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

        ID = (int) (Integer.MAX_VALUE * Math.random());
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
        final int deformationLimitsArraySize = deformationLimits.length;
        List<Facet> sublist = null;
        double[] checkedDeformations = null;
        ComputationTask ct = null;
        if (!subSplitters.isEmpty()) {
            ct = subSplitters.get(0).next();
        } else {
            final int rest = facets.size() - facetIndex;

            int taskSize = rest;
            final long l = DeformationUtils.calculateDeformationCount(deformationLimits);
            while (taskSize > 1 && !isMemOk(l, taskSize, facetSize, deformationLimitsArraySize)) {
                taskSize *= COEFF_LIMIT_ADJUST;
            }

            if (taskSize == 1 && !isMemOk(l, taskSize, facetSize, deformationLimitsArraySize)) {
                Logger.warn("Too many deformations, {0} generating subsplitters.", ID);
                checkedDeformations = new double[deformationLimitsArraySize];
                System.arraycopy(deformationLimits, 0, checkedDeformations, 0, deformationLimitsArraySize);

                sublist = new ArrayList<>(1);
                sublist.add(facets.get(facetIndex));

                final int[] stepCounts = DeformationUtils.generateDeformationCounts(deformationLimits);
                final int minIndex = findMinIndexBiggerThanZero(stepCounts);
                final int newStepCount = stepCounts[minIndex] / 2 - 1;
                final double midPoint = deformationLimits[minIndex * 3] + newStepCount * deformationLimits[minIndex * 3 + 2];
                checkedDeformations = new double[deformationLimitsArraySize];
                System.arraycopy(deformationLimits, 0, checkedDeformations, 0, deformationLimitsArraySize);
                checkedDeformations[minIndex * 3 + 1] = midPoint;
                Logger.trace(Arrays.toString(deformationLimits));
                Logger.trace(Arrays.toString(checkedDeformations));
                subSplitters.add(new OpenCLSplitter(image1, image2, sublist, checkedDeformations, true));
                checkedDeformations = new double[deformationLimitsArraySize];
                System.arraycopy(deformationLimits, 0, checkedDeformations, 0, deformationLimitsArraySize);
                checkedDeformations[minIndex * 3] = midPoint + deformationLimits[minIndex * 3 + 2];
                Logger.trace(Arrays.toString(checkedDeformations));
                subSplitters.add(new OpenCLSplitter(image1, image2, sublist, checkedDeformations, true));
                Logger.trace("--- {0} splits into {1}; {2}.", ID, subSplitters.get(0).ID, +subSplitters.get(1).ID);

                ct = subSplitters.get(0).next();
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
            ct = new ComputationTask(image1, image2, sublist, checkedDeformations, subSplitter);
            Logger.trace("{0} computing {1}", ID, Arrays.toString(ct.getDeformationLimits()));
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
