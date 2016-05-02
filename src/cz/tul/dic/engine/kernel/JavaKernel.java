/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.kernel;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Interpolation;
import cz.tul.dic.data.deformation.DeformationOrder;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.engine.memory.MemoryManager;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.pmw.tinylog.Logger;

public class JavaKernel extends AbstractKernel<MemoryManager> {

    private static final int COUNT_THREADS = Runtime.getRuntime().availableProcessors();
    private ExecutorService exec;

    public JavaKernel(final KernelInfo kernelInfo, final MemoryManager memManager, final WorkSizeManager wsm) {
        super(kernelInfo, memManager, wsm);
    }

    @Override
    public List<CorrelationResult> computeFindBestInner(ComputationTask task) throws ComputationException {
        final double[] results = computeRawInner(task);
        // create results
        final int subsetCount = task.getSubsets().size();
        final int defArrayLength = DeformationUtils.getDeformationCoeffCount(task.getOrder());
        final List<long[]> deformationCounts = DeformationUtils.generateDeformationCounts(task.getDeformations());
        final List<CorrelationResult> result = new ArrayList<>(subsetCount);

        int base = 0;
        double max;
        int maxIndex;
        long defCount;
        double[] resultDeformation;
        for (int i = 0; i < subsetCount; i++) {
            max = -Double.MAX_VALUE;
            maxIndex = -1;

            defCount = deformationCounts.get(i)[defArrayLength];
            for (int j = 0; j < defCount; j++) {
                if (results[base + j] > max) {
                    max = results[base + j];
                    maxIndex = j;
                }
            }
            base += defCount;

            result.add(new CorrelationResult(
                    max,
                    DeformationUtils.extractDeformationFromLimits(maxIndex, task.getDeformations().get(i), deformationCounts.get(i))));
        }

        return result;
    }

    @Override
    public double[] computeRawInner(ComputationTask task) throws ComputationException {
        return compute(
                task.getImageA().toBWArray(), task.getImageB().toBWArray(),
                task.getSubsets(),
                task.getDeformations(),
                task.getImageA().getWidth(), task.getOrder());
    }

    @Override
    public void stopComputation() {
        exec.shutdownNow();
    }

    @Override
    public boolean usesGPU() {
        return false;
    }

    @Override
    public void prepareKernel(int subsetSize, DeformationOrder deg, boolean usesLimits, Interpolation interpolation) throws ComputationException {
        // no preparation needed
    }

    public double[] compute(
            final byte[] imageA, final byte[] imageB,
            final List<AbstractSubset> subsets, final List<double[]> deformations,
            final int imageWidth, final DeformationOrder defOrder) {
        // preparation
        exec = Executors.newWorkStealingPool();
        final List<long[]> counts = DeformationUtils.generateDeformationCounts(deformations);
        final int subsetCount = subsets.size();
        final int deformationCount = (int) counts.get(0)[counts.get(0).length - 1];
        final double[] results = new double[subsetCount * deformationCount];
        // execution        
        final List<Worker> workers = new ArrayList<>(COUNT_THREADS);

        final List<int[]> workBounds = generateBounds(COUNT_THREADS + 1, subsetCount);

        final int subsetDataArrayLength = subsets.get(0).getData().length;
        final int[] subsetData = new int[subsetCount * subsetDataArrayLength];
        final double[] subsetCenters = new double[subsetCount * 2];
        for (int i = 0; i < subsetCount; i++) {
            System.arraycopy(subsets.get(i).getData(), 0, subsetData, i * subsetDataArrayLength, subsetDataArrayLength);
            System.arraycopy(subsets.get(i).getCenter(), 0, subsetCenters, i * 2, 2);
        }

        workBounds.stream().forEach((bound) -> {
            workers.add(new Worker(
                    bound[0], bound[1],
                    imageA, imageB,
                    imageWidth,
                    subsetData, subsetCenters,
                    subsets.get(0).getSize(),
                    deformations,
                    counts,
                    defOrder,
                    results));
        });
        try {
            workers.stream().forEach((w) -> {
                exec.execute(w);
            });
            exec.shutdown();
            exec.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException ex) {
            Logger.warn(ex);
        }

        return results;
    }

    private static List<int[]> generateBounds(final int count, final int size) {
        final List<int[]> result = new LinkedList<>();
        int part = Math.max(size / count, 1);
        int c = 0, prevC = 0;
        for (int i = 0; i < count; i++) {
            c += part;
            result.add(new int[]{prevC, c});
            prevC = c;
            if (c >= size) {
                break;
            }
        }

        return result;
    }

    private static class Worker implements Runnable {

        private final int startIndex, endIndex;
        private final byte[] imageA, imageB;
        private final int imageWidth;
        private final int[] subsetData;
        private final double[] subsetCenters;
        private final int subsetSize;
        private final List<double[]> deformations;
        private final List<long[]> counts;
        private final DeformationOrder defOrder;
        private final double[] results;

        public Worker(int startIndex, int endIndex, byte[] imageA, byte[] imageB, int imageWidth, int[] subsetData, double[] subsetCenters, int subsetSize, List<double[]> deformations, List<long[]> counts, DeformationOrder defOrder, double[] results) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.imageA = imageA;
            this.imageB = imageB;
            this.imageWidth = imageWidth;
            this.subsetData = subsetData;
            this.deformations = deformations;
            this.counts = counts;
            this.subsetCenters = subsetCenters;
            this.subsetSize = subsetSize;
            this.results = results;
            this.defOrder = defOrder;
        }

        @Override
        public void run() {
            if (startIndex >= endIndex) {
                throw new IllegalArgumentException("Subset start index must be smaller than end index.");
            }

            final int subsetArraySize = calculateFacetArraySize(subsetSize);
            final int subsetArea = calculateFacetArea(subsetSize);

            final double[] deformedSubset = new double[subsetArraySize];
            final int[] deformeSubsetI = new int[subsetArea];
            final int[] subsetI = new int[subsetArea];

            final int defArrayLength = DeformationUtils.getDeformationCoeffCount(defOrder);

            final int imageHeight = imageA.length / imageWidth;

            final double[] deformation = new double[defArrayLength];
            double[] localDefLimits;
            long[] localCounts;
            int deformationCount;
            for (int si = startIndex; si < endIndex; si++) {
                interpolate(subsetData, si, subsetI, imageA, imageWidth, imageHeight);
                localDefLimits = deformations.get(si);
                localCounts = counts.get(si);
                deformationCount = (int) localCounts[localCounts.length - 1];

                for (int di = 0; di < deformationCount; di++) {
                    generateDeformation(localDefLimits, localCounts, di, deformation, defArrayLength);

                    switch (defOrder) {
                        case ZERO:
                            deform0(subsetData, subsetSize, si, deformedSubset, deformation);
                            break;
                        case FIRST:
                            deform1(subsetData, subsetSize, subsetCenters, si, deformedSubset, deformation);
                            break;
                        case SECOND:
                            deform2(subsetData, subsetSize, subsetCenters, si, deformedSubset, deformation);
                            break;
                        default:
                            throw new UnsupportedOperationException("Second order deformation not support yet.");
                    }

                    interpolate(deformedSubset, deformeSubsetI, imageB, imageWidth, imageHeight);
                    results[si * deformationCount + di] = correlate(subsetI, deformeSubsetI);
                }
            }
        }
    }

    private static void generateDeformation(final double[] limits, final long[] counts, final int deformationIndex, final double[] deformation, final int defArrayLength) {
        if (deformationIndex >= counts[defArrayLength]) {
            return;
        }

        long counter = deformationIndex;
        deformation[0] = counter % counts[0];
        counter = counter / counts[0];
        deformation[1] = counter % counts[1];
        if (defArrayLength > 2) {
            counter = counter / counts[1];
            deformation[2] = counter % counts[2];
            counter = counter / counts[2];
            deformation[3] = counter % counts[3];
            counter = counter / counts[3];
            deformation[4] = counter % counts[4];
            counter = counter / counts[4];
            deformation[5] = counter % counts[5];
            if (defArrayLength > 6) {
                counter = counter / counts[5];
                deformation[6] = counter % counts[6];
                counter = counter / counts[6];
                deformation[7] = counter % counts[7];
                counter = counter / counts[7];
                deformation[8] = counter % counts[8];
                counter = counter / counts[8];
                deformation[9] = counter % counts[9];
                counter = counter / counts[9];
                deformation[10] = counter % counts[10];
                counter = counter / counts[10];
                deformation[11] = counter % counts[11];
            }
        }

        deformation[0] = limits[0] + deformation[0] * limits[2];
        deformation[1] = limits[3] + deformation[1] * limits[5];
        if (defArrayLength > 2) {
            deformation[2] = limits[6] + deformation[2] * limits[8];
            deformation[3] = limits[9] + deformation[3] * limits[11];
            deformation[4] = limits[12] + deformation[4] * limits[14];
            deformation[5] = limits[15] + deformation[5] * limits[17];
            if (defArrayLength > 6) {
                deformation[6] = limits[18] + deformation[6] * limits[20];
                deformation[7] = limits[21] + deformation[7] * limits[23];
                deformation[8] = limits[24] + deformation[8] * limits[26];
                deformation[9] = limits[27] + deformation[9] * limits[29];
                deformation[10] = limits[30] + deformation[10] * limits[32];
                deformation[11] = limits[33] + deformation[11] * limits[35];
            }
        }
    }

    private static void deform0(
            final int[] subsets, final int subsetSize, final int subsetIndex,
            final double[] deformedFacet, final double[] deformation) {
        final int facetBase = subsetIndex * calculateFacetArraySize(subsetSize);
        final int facetArea = calculateFacetArea(subsetSize);

        int x, y;
        int baseIndex;
        double val;
        for (int i = 0; i < facetArea; i++) {
            baseIndex = facetBase + i * 2;

            x = subsets[baseIndex];
            y = subsets[baseIndex + 1];

            val = x + deformation[0];
            if (val < 0) {
                val = 0;
            }
            deformedFacet[i * 2] = val;

            val = y + deformation[1];
            if (val < 0) {
                val = 0;
            }
            deformedFacet[i * 2 + 1] = val;
        }
    }

    private static void deform1(
            final int[] subsets, final int subsetSize, final double[] subsetCenters, final int subsetIndex,
            final double[] deformedSubset, final double[] deformation) {
        final int facetBase = subsetIndex * calculateFacetArraySize(subsetSize);
        final int facetArea = calculateFacetArea(subsetSize);

        final double cx = subsetCenters[subsetIndex * 2];
        final double cy = subsetCenters[subsetIndex * 2 + 1];

        int x, y;
        double dx, dy;
        int baseIndex;
        double val;
        for (int i = 0; i < facetArea; i++) {
            baseIndex = facetBase + i * 2;

            x = subsets[baseIndex];
            y = subsets[baseIndex + 1];

            dx = x - cx;
            dy = y - cy;

            val = x + deformation[0] + deformation[2] * dx + deformation[3] * dy;
            if (val < 0) {
                val = 0;
            }
            deformedSubset[i * 2] = val;

            val = y + deformation[1] + deformation[4] * dx + deformation[5] * dy;
            if (val < 0) {
                val = 0;
            }
            deformedSubset[i * 2 + 1] = val;
        }
    }

    private static void deform2(
            final int[] subsets, final int subsetSize, final double[] subsetCenters, final int subsetIndex,
            final double[] deformedSubset, final double[] deformation) {
        final int facetBase = subsetIndex * calculateFacetArraySize(subsetSize);
        final int facetArea = calculateFacetArea(subsetSize);

        final double cx = subsetCenters[subsetIndex * 2];
        final double cy = subsetCenters[subsetIndex * 2 + 1];

        int x, y;
        double dx, dy;
        int baseIndex;
        double val;
        for (int i = 0; i < facetArea; i++) {
            baseIndex = facetBase + i * 2;

            x = subsets[baseIndex];
            y = subsets[baseIndex + 1];

            dx = x - cx;
            dy = y - cy;

            val = x + deformation[0] + deformation[2] * dx + deformation[3] * dy
                    + 0.5 * deformation[6] * dx * dx + 0.5 * deformation[7] * dy * dy + deformation[8] * dx * dy;
            if (val < 0) {
                val = 0;
            }
            deformedSubset[i * 2] = val;

            val = y + deformation[1] + deformation[4] * dx + deformation[5] * dy
                    + 0.5 * deformation[9] * dx * dx + 0.5 * deformation[10] * dy * dy + deformation[11] * dx * dy;
            if (val < 0) {
                val = 0;
            }
            deformedSubset[i * 2 + 1] = val;
        }
    }

    private static void interpolate(final double[] deformedFacet, final int[] intensities, final byte[] image, final int imageWidth, final int imageHeight) {
        int x, y, intensity, i2;
        double dx, dy, val;
        for (int i = 0; i < intensities.length; i++) {
            i2 = i * 2;

            val = deformedFacet[i2];
            x = Math.min((int) Math.floor(val), imageWidth - 2);
            dx = val - x;

            val = deformedFacet[i2 + 1];
            y = Math.min((int) Math.floor(val), imageHeight - 2);
            dy = val - y;

            intensity = 0;
            intensity += image[compute1DIndex(x, y, imageWidth)] * (1 - dx) * (1 - dy);
            intensity += image[compute1DIndex(x + 1, y, imageWidth)] * dx * (1 - dy);
            intensity += image[compute1DIndex(x, y + 1, imageWidth)] * (1 - dx) * dy;
            intensity += image[compute1DIndex(x + 1, y + 1, imageWidth)] * dx * dy;

            intensities[i] = intensity;
        }
    }

    private static void interpolate(final int[] facets, final int facetIndex, final int[] intensities, final byte[] image, final int imageWidth, final int imageHeight) {
        final int facetBase = facetIndex * 2 * intensities.length;

        int x, y, intensity, i2;
        double dx, dy, val;
        for (int i = 0; i < intensities.length; i++) {
            i2 = i * 2;

            val = facets[facetBase + i2];
            x = (int) Math.floor(val);
            dx = val - x;

            val = facets[facetBase + i2 + 1];
            y = (int) Math.floor(val);
            dy = val - y;

            intensity = 0;
            intensity += image[compute1DIndex(x, y, imageWidth)] * (1 - dx) * (1 - dy);
            intensity += image[compute1DIndex(Math.min(x + 1, imageWidth - 1), y, imageWidth)] * dx * (1 - dy);
            intensity += image[compute1DIndex(x, Math.min(y + 1, imageHeight - 1), imageWidth)] * (1 - dx) * dy;
            intensity += image[compute1DIndex(Math.min(x + 1, imageWidth - 1), Math.min(y + 1, imageHeight - 1), imageWidth)] * dx * dy;

            intensities[i] = intensity;
        }
    }

    private static float correlate(final int[] a, final int[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Array size mismatch.");
        }

        final float meanA = mean(a);
        final float deltaA = delta(a, meanA);

        final float meanB = mean(b);
        final float deltaB = delta(b, meanB);

        float result = 0;
        for (int i = 0; i < a.length; i++) {
            result += (a[i] - meanA) * (b[i] - meanB);
        }
        if (deltaA != 0 && deltaB != 0) {
            result /= deltaA * deltaB;
        }

        return result;
    }

    private static float mean(int[] l) {
        float result = 0;
        for (int i : l) {
            result += i;
        }

        return result / (float) l.length;
    }

    private static float delta(int[] l, final float mean) {
        float result = 0;

        float tmp;
        for (int i : l) {
            tmp = i - mean;
            result += tmp * tmp;
        }

        return (float) Math.sqrt(result);
    }

    private static int calculateFacetArraySize(final int subsetSize) {
        return (subsetSize * subsetSize) * 2;
    }

    private static int calculateFacetArea(final int subsetSize) {
        return subsetSize * subsetSize;
    }

    private static int compute1DIndex(final int x, final int y, final int width) {
        return (y * width) + x;
    }

}
