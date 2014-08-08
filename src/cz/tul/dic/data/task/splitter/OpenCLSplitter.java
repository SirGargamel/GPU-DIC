package cz.tul.dic.data.task.splitter;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.engine.opencl.DeviceManager;
import cz.tul.dic.generators.DeformationGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class OpenCLSplitter extends TaskSplitter {

    private static final int MAX_SIZE_DEFORMATION_A = Integer.MAX_VALUE / 64;
    private static final int SIZE_INT = 4;
    private static final int SIZE_FLOAT = 4;
    private static final int SIZE_PIXEL = 4;
    private static final double COEFF_LIMIT_ADJUST = 0.75;
    private final int defArrayLength, facetSize;
    private boolean hasNext;
    private int facetIndex, divPosition;
    private long defCount, divCount;
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
        double[] checkedDeformations = null;
        if (subDivision) {
            divPosition++;

            sublist = new ArrayList<>(1);
            sublist.add(facets.get(facetIndex));

            final int l = (int) (defCount * defArrayLength);
            try {
                checkedDeformations = DeformationGenerator.generateDeformations(deformationLimits, divPosition * l, (divPosition + 1) * l);
            } catch (ComputationException ex) {
                Logger.error(ex);
                Logger.error("Failed to generate deformations, should not happen here - something is very wrong.");
            }

            if (divPosition == divCount - 1) {
                subDivision = false;
                facetIndex++;
            }
        } else {
            final int rest = facets.size() - facetIndex;

            int taskSize = rest;
            final long l = DeformationGenerator.calculateDeformationArraySize(deformationLimits);
            while (taskSize > 1 && !isMemOk(l, taskSize, facetSize, defArrayLength)) {
                taskSize *= COEFF_LIMIT_ADJUST;
            }

            checkedDeformations = null;
            long newL;
            if (taskSize == 1 && !isMemOk(l, taskSize, facetSize, defArrayLength)) {
                Logger.warn("Too many deformations to try, spliting facet task .");
                subDivision = true;
                divPosition = 0;
                defCount = l / defArrayLength;

                do {
                    defCount /= 2;
                    newL = defCount * defArrayLength;
                } while (newL > MAX_SIZE_DEFORMATION_A && !isMemOk(newL, rest, facetSize, defArrayLength));

                divCount = (l / defArrayLength) / defCount;
                try {
                    checkedDeformations = DeformationGenerator.generateDeformations(deformationLimits, divPosition * newL, (divPosition + 1) * newL);
                } catch (ComputationException ex) {
                    Logger.error(ex);
                    Logger.error("Failed to generate deformations, should not happen here - something is very wrong.");
                }

                sublist = new ArrayList<>(1);
                sublist.add(facets.get(facetIndex));
            } else {
                try {
                    checkedDeformations = DeformationGenerator.generateDeformations(deformationLimits);
                } catch (ComputationException ex) {
                    Logger.error(ex);
                    Logger.error("Failed to generate deformations, should not happen here - something is very wrong.");
                }

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

    private boolean isMemOk(final long deformationCount, final int facetCount, final int facetSize, final int deformationArraySize) {
        final long imageSize = image1.getHeight() * image1.getWidth() * SIZE_PIXEL * 2;
        final long deformationsSize = deformationCount * SIZE_FLOAT;
        final int reserve = 32 * SIZE_INT;
        final long facetDataSize = facetSize * facetSize * 2 * SIZE_INT * facetCount;
        final long facetCentersSize = 2 * SIZE_FLOAT * facetCount;
        final long resultSize = facetCount * (deformationCount / deformationArraySize) * SIZE_FLOAT;
        final long fullSize = imageSize + deformationsSize + reserve + facetDataSize + facetCentersSize + resultSize;

        final long maxAllocMem = Math.min(DeviceManager.getDevice().getMaxMemAllocSize() * 3 / 4, Integer.MAX_VALUE);
        final long maxMem = DeviceManager.getDevice().getGlobalMemSize();
        boolean result = fullSize > 0 && fullSize < maxMem;
        result &= (deformationCount / deformationArraySize) <= (Integer.MAX_VALUE / (facetCount * SIZE_FLOAT));    // int overflow check
        result &= resultSize <= Integer.MAX_VALUE && resultSize > 0 && resultSize < maxAllocMem;
        result &= imageSize <= Integer.MAX_VALUE && imageSize > 0 && imageSize < maxAllocMem;
        result &= deformationsSize <= Integer.MAX_VALUE && deformationsSize > 0 && deformationsSize < maxAllocMem;
        result &= facetDataSize <= Integer.MAX_VALUE && facetDataSize > 0 && facetDataSize < maxAllocMem;
        result &= facetCentersSize <= Integer.MAX_VALUE && facetCentersSize > 0 && facetCentersSize < maxAllocMem;

//        System.out.print(result + " - ");
//        System.out.print(Arrays.toString(new long[]{deformations.length / deformationArraySize, facetCount}));
//        System.out.print(" - ");
//        System.out.println(Arrays.toString(new long[]{deformationsSize, facetDataSize, facetCentersSize, facetCentersSize, resultSize, fullSize, maxAllocMem, maxMem}));
        return result;
    }

}
