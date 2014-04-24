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
    private final long fixedDataSize;
    private boolean hasNext;
    private int index;

    public OpenCLSplitter(TaskContainer tc, int index1, int index2, List<Facet> facets, double[] deformations, ROI roi) {
        super(tc, index1, index2, facets, deformations, roi);

        long dataSize = 0;
        // image size
        dataSize += tc.getImage(index1).getHeight() * tc.getImage(index1).getWidth() * SIZE_PIXEL;
        // two images per round
        dataSize *= 2;
        // deformations
        dataSize += deformations.length * SIZE_FLOAT;
        // RESERVE
        dataSize += 32 * SIZE_INT;

        fixedDataSize = dataSize;

        checkIfHasNext();
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    private void checkIfHasNext() {
        hasNext = index < facets.size() - 1;
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
        while (!isMemOk(taskSize, fs, defArrayLength)) {
            taskSize /= 2;
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

        return new ComputationTask(tc.getImage(index1), tc.getImage(index2), sublist, deformations);
    }

    private boolean isMemOk(final int facetCount, final int facetSize, final int deformationArraySize) {
        final long facetDataSize = facetSize * facetSize * 2 * SIZE_INT * facetCount;
        final long facetCentersSize = 2 * SIZE_FLOAT * facetCount;
        final long resultSize = facetCount * (deformations.length / deformationArraySize) * SIZE_FLOAT;
        final long fullSize = fixedDataSize + facetDataSize + facetCentersSize + resultSize;

        final long maxAllocMem = Math.min(DeviceManager.getDevice().getMaxMemAllocSize(), Integer.MAX_VALUE);
        final long maxMem = DeviceManager.getDevice().getGlobalMemSize();
        boolean result = fullSize > 0 && fullSize < maxMem;
        result &= resultSize > 0 && resultSize < maxAllocMem;
        result &= facetDataSize > 0 && facetDataSize < maxAllocMem;
        result &= facetCentersSize > 0 && facetCentersSize < maxAllocMem;

        return result;
    }

}
