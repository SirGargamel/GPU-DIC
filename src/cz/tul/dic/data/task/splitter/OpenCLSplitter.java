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
        dataSize += tc.getImage(index1).getHeight() * tc.getImage(index1).getWidth() * SIZE_PIXEL;
        dataSize *= 2;
        dataSize += deformations.length * SIZE_FLOAT;
        // RESERVE
        dataSize += 10 * SIZE_INT;

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
        final long maxMem = DeviceManager.getDevice().getGlobalMemSize();

        final int rest = facets.size() - index;
        int defArrayLength;
        try {
            defArrayLength = TaskContainerUtils.getDeformationArrayLength(tc, index, roi);
        } catch (ComputationException ex) {
            defArrayLength = 36;
            Logger.warn(ex);
        }

        int taskSize = rest;
        long mem = computeMemorySize(rest, tc.getFacetSize(index1, roi), defArrayLength);

        while (mem > maxMem) {
            taskSize /= 2;
            mem = computeMemorySize(rest, tc.getFacetSize(index1, roi), defArrayLength);
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

    private long computeMemorySize(final int facetCount, final int facetSize, final int deformationCount) {
        long result = fixedDataSize;

        // facet data
        result += facetSize * facetSize * 2 * SIZE_INT * facetCount;
        // facet centers
        result += 2 * SIZE_FLOAT * facetCount;
        // results
        result += facetCount * deformationCount * SIZE_FLOAT;

        return result;
    }

}
