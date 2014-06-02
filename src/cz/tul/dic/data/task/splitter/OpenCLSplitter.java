/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic.data.task.splitter;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
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
    private static final double COEFF_LIMIT_ADJUST = 1.5;
    private boolean hasNext;
    private int index;

    public OpenCLSplitter(TaskContainer tc, int index1, int index2, List<Facet> facets, double[] deformations, ROI roi) {
        super(tc, index1, index2, facets, deformations, roi);

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
        int defArrayLength;
        try {
            defArrayLength = TaskContainerUtils.getDeformationArrayLength(tc, index1, roi);
        } catch (ComputationException ex) {
            defArrayLength = 36;
            Logger.warn(ex);
        }
        final int fs = tc.getFacetSize(index1, roi);

        int taskSize = rest;
        while (taskSize > 1 && !isMemOk(deformations, taskSize, fs, defArrayLength)) {
            taskSize /= 2;
        }
        
        double[] checkedDeformations = null;
        if (taskSize == 1 && !isMemOk(deformations, taskSize, fs, defArrayLength)) {
            Logger.warn("Too many deformations to try, lowering limits.");
            
            final double[] limits = tc.getDeformationLimits(index1, roi);
            do {
                try {
                    for (int i = 2; i < limits.length; i += 3) {
                        limits[i] *= COEFF_LIMIT_ADJUST;
                    }
                    checkedDeformations = DeformationGenerator.generateDeformations(limits);
                } catch (ComputationException ex) {
                    Logger.error("Failed to generate deformations.");
                    Logger.trace(ex);
                }
            } while (!isMemOk(checkedDeformations, rest, fs, defArrayLength));                        
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

        return new ComputationTask(tc.getImage(index1), tc.getImage(index2), sublist, checkedDeformations);
    }

    private boolean isMemOk(final double[] deformations, final int facetCount, final int facetSize, final int deformationArraySize) {
        final long imageSize = tc.getImage(index1).getHeight() * tc.getImage(index1).getWidth() * SIZE_PIXEL * 2;                
        final long deformationsSize = deformations.length * SIZE_FLOAT;        
        final long reserve = 32 * SIZE_INT;
        final long facetDataSize = facetSize * facetSize * 2 * SIZE_INT * facetCount;
        final long facetCentersSize = 2 * SIZE_FLOAT * facetCount;
        final long resultSize = facetCount * (deformations.length / deformationArraySize) * SIZE_FLOAT;
        final long fullSize = imageSize + deformationsSize + reserve + facetDataSize + facetCentersSize + resultSize;

        final long maxAllocMem = Math.min(DeviceManager.getDevice().getMaxMemAllocSize(), Integer.MAX_VALUE);
        final long maxMem = DeviceManager.getDevice().getGlobalMemSize();
        boolean result = fullSize > 0 && fullSize < maxMem;
        result &= imageSize <= Integer.MAX_VALUE && imageSize > 0 && imageSize < maxAllocMem;
        result &= deformationsSize <= Integer.MAX_VALUE && deformationsSize > 0 && deformationsSize < maxAllocMem;
        result &= resultSize <= Integer.MAX_VALUE && resultSize > 0 && resultSize < maxAllocMem;
        result &= facetDataSize <= Integer.MAX_VALUE && facetDataSize > 0 && facetDataSize < maxAllocMem;
        result &= facetCentersSize <= Integer.MAX_VALUE && facetCentersSize > 0 && facetCentersSize < maxAllocMem;

        return result;
    }

}
