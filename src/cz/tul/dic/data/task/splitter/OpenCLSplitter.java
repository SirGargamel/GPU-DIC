/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class OpenCLSplitter extends TaskSplitter {

    private static final int SIZE_INT = 4;
    private static final int SIZE_FLOAT = 4;
    private static final int SIZE_PIXEL = 4;
    private static final double COEFF_LIMIT_ADJUST = 0.75;
    private final int defArrayLength, facetSize;
    private final double[] defLimits;
    private boolean hasNext;
    private int index;

    public OpenCLSplitter(Image image1, Image image2, List<Facet> facets, double[] deformations, ROI roi, final Object taskSplitValue) throws ComputationException {
        super(image1, image2, facets, deformations, roi);

        if (taskSplitValue instanceof Object[]) {
            final Object[] vals = (Object[]) taskSplitValue;

            defArrayLength = (int) vals[0];
            facetSize = (int) vals[1];
            defLimits = (double[]) vals[2];
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
        hasNext = index < facets.size();
    }

    @Override
    public ComputationTask next() {
        final int rest = facets.size() - index;

        int taskSize = rest;
        while (taskSize > 1 && !isMemOk(deformations, taskSize, facetSize, defArrayLength)) {
            taskSize *= COEFF_LIMIT_ADJUST;
        }

        double[] checkedDeformations = null;
        if (taskSize == 1 && !isMemOk(deformations, taskSize, facetSize, defArrayLength)) {
            Logger.warn("Too many deformations to try, lowering limits.");

            do {
                try {
                    for (int i = 2; i < defLimits.length; i += 3) {
                        defLimits[i] *= COEFF_LIMIT_ADJUST;
                    }
                    checkedDeformations = DeformationGenerator.generateDeformations(defLimits);
                } catch (ComputationException ex) {
                    Logger.error("Failed to generate deformations.");
                    Logger.trace(ex);
                }
            } while (!isMemOk(checkedDeformations, rest, facetSize, defArrayLength));
        } else {
            checkedDeformations = deformations;
        }

        final List<Facet> sublist = new ArrayList<>(taskSize);
        final int facetCount = facets.size();

        int count = 0;
        while (count < taskSize && index < facetCount) {
            sublist.add(facets.get(index));

            count++;
            index++;
        }

        checkIfHasNext();

        return new ComputationTask(image1, image2, sublist, checkedDeformations);
    }

    private boolean isMemOk(final double[] deformations, final int facetCount, final int facetSize, final int deformationArraySize) {
        final int imageSize = image1.getHeight() * image1.getWidth() * SIZE_PIXEL * 2;
        final int deformationsSize = deformations.length * SIZE_FLOAT;
        final int reserve = 32 * SIZE_INT;
        final int facetDataSize = facetSize * facetSize * 2 * SIZE_INT * facetCount;
        final int facetCentersSize = 2 * SIZE_FLOAT * facetCount;
        final int resultSize = facetCount * (deformations.length / deformationArraySize) * SIZE_FLOAT;
        final int fullSize = imageSize + deformationsSize + reserve + facetDataSize + facetCentersSize + resultSize;

        final long maxAllocMem = Math.min(DeviceManager.getDevice().getMaxMemAllocSize() * 3 / 4, Integer.MAX_VALUE);
        final long maxMem = DeviceManager.getDevice().getGlobalMemSize();
        boolean result = fullSize > 0 && fullSize < maxMem;
        result &= (deformations.length / deformationArraySize) <= (Integer.MAX_VALUE / (facetCount * SIZE_FLOAT));    // int overflow check
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
