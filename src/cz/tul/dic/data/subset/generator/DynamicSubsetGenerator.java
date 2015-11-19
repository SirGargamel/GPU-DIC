/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.subset.generator;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.result.Result;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.roi.AbstractROI;
import cz.tul.dic.data.subset.SquareSubset2D;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class DynamicSubsetGenerator extends AbstractSubsetGenerator {

    private static final double DIF_TRESHOLD = 0.25;
    private static final double DIF_TRESHOLD_2 = DIF_TRESHOLD * DIF_TRESHOLD;
    private static final double DIST_MIN = 2;
    private static final double DIST_MIN_2 = DIST_MIN * DIST_MIN;
    private static final double DELTA = 1 / 4.0;

    @Override
    public HashMap<AbstractROI, List<AbstractSubset>> generateSubsets(TaskContainer tc, int round) throws ComputationException {
        final Object o = tc.getParameter(TaskParameter.SUBSET_GENERATOR_PARAM);
        if (o == null) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "No subset generator spacing.");
        }

        final int spacing = (int) o;
        if (spacing <= 0) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Illegal subset generator spacing " + spacing);
        }

        final AbstractSubsetGenerator equalGenerator = new EqualSpacingSubsetGenerator();
        final Result previousResult = tc.getResult(round - 1, round);
        if (previousResult == null) {
            return equalGenerator.generateSubsets(tc, round);
        }
        final double[][][] previousDisplacement = previousResult.getDisplacementResult().getDisplacement();

        final Set<AbstractROI> rois = tc.getRois(round);
        final HashMap<AbstractROI, List<AbstractSubset>> result = equalGenerator.generateSubsets(tc, round);

        final Queue<Triangle> processQueue = new LinkedList<>();
        Triangle triangle;
        AbstractSubset subA, subB, subC, subNewAB, subNewBC, subNewAC;
        List<AbstractSubset> subsets;
        int wCount, hCount;
        double iA, iB, iC, iRatio;
        double xA, xB, xC, yA, yB, yC;
        double xNew, yNew;
        int subsetSize;
        for (AbstractROI roi : rois) {
            subsetSize = tc.getSubsetSize(round, roi);
            subsets = result.get(roi);

            wCount = -1;
            for (int i = 0; i < subsets.size() - 1; i++) {
                if (subsets.get(i).getCenter()[1] != subsets.get(i + 1).getCenter()[1]) {
                    wCount = i + 1;
                    break;
                }
            }
            if (wCount == -1) {
                throw new IllegalArgumentException("Unable to find subset count in one line.");
            }
            hCount = subsets.size() / wCount;

            for (int y = 0; y < hCount - 1; y++) {
                for (int x = 0; x < wCount - 1; x++) {
                    processQueue.add(new Triangle(
                            subsets.get(y * wCount + x),
                            subsets.get(y * wCount + x + 1),
                            subsets.get((y + 1) * wCount + x)));
                    processQueue.add(new Triangle(
                            subsets.get(y * wCount + x + 1),
                            subsets.get((y + 1) * wCount + x),
                            subsets.get((y + 1) * wCount + x + 1)));
                }
            }

            while (!processQueue.isEmpty()) {
                triangle = processQueue.poll();
                subA = triangle.getSubset1();
                subB = triangle.getSubset2();
                subC = triangle.getSubset3();
                if (difExceedsTreshold(subA, subB, previousDisplacement)
                        || difExceedsTreshold(subA, subC, previousDisplacement)
                        || difExceedsTreshold(subB, subC, previousDisplacement)) {
                    subNewAB = null;
                    subNewBC = null;
                    subNewAC = null;

                    xA = subA.getCenter()[0];
                    yA = subA.getCenter()[1];
                    xB = subB.getCenter()[0];
                    yB = subB.getCenter()[1];
                    xC = subC.getCenter()[0];
                    yC = subC.getCenter()[1];

                    iA = calculateInvariant(subA, previousDisplacement);
                    iB = calculateInvariant(subB, previousDisplacement);
                    iC = calculateInvariant(subC, previousDisplacement);

                    // generate new subset centers
                    // check if subsets centers are far enough
                    if (calculateDist2(subA.getCenter(), subB.getCenter()) >= DIST_MIN_2) {
                        iRatio = iA / (iA + iB);
                        if (iRatio >= DELTA && iRatio <= (1 - DELTA)) {
                            xNew = (xA * iA + xB * iB) / (iA + iB);
                            yNew = (yA * iA + yB * iB) / (iA + iB);
                        } else {
                            xNew = xA * DELTA + xB * (1 - DELTA);
                            yNew = yA * DELTA + yB * (1 - DELTA);
                        }
                        subNewAB = new SquareSubset2D(subsetSize, xNew, yNew);
                        subsets.add(subNewAB);
                    }
                    if (calculateDist2(subB.getCenter(), subC.getCenter()) >= DIST_MIN_2) {
                        iRatio = iB / (iB + iC);
                        if (iRatio >= DELTA && iRatio <= (1 - DELTA)) {
                            xNew = (xB * iB + xC * iC) / (iB + iC);
                            yNew = (yB * iB + yC * iC) / (iB + iC);
                        } else {
                            xNew = xB * DELTA + xC * (1 - DELTA);
                            yNew = yB * DELTA + yC * (1 - DELTA);
                        }
                        subNewBC = new SquareSubset2D(subsetSize, xNew, yNew);
                        subsets.add(subNewBC);
                    }
                    if (calculateDist2(subA.getCenter(), subC.getCenter()) >= DIST_MIN_2) {
                        iRatio = iA / (iA + iC);
                        if (iRatio >= DELTA && iRatio <= (1 - DELTA)) {
                            xNew = (xA * iA + xC * iC) / (iA + iC);
                            yNew = (yA * iA + yC * iC) / (iA + iC);
                        } else {
                            xNew = xA * DELTA + xC * (1 - DELTA);
                            yNew = yA * DELTA + yC * (1 - DELTA);
                        }
                        subNewAC = new SquareSubset2D(subsetSize, xNew, yNew);
                        subsets.add(subNewAC);
                    }

                    // generate new triangles
                    if (subNewAB != null && subNewBC != null && subNewAC != null) {
                        processQueue.add(new Triangle(subA, subNewAB, subNewAC));
                        processQueue.add(new Triangle(subB, subNewAB, subNewBC));
                        processQueue.add(new Triangle(subC, subNewAC, subNewBC));
                        processQueue.add(new Triangle(subNewAB, subNewBC, subNewAC));
                    } else if (subNewAB != null && subNewAC != null) {
                        processQueue.add(new Triangle(subA, subNewAB, subNewAC));
                    } else if (subNewAB != null && subNewBC != null) {
                        processQueue.add(new Triangle(subB, subNewAB, subNewBC));
                    } else if (subNewBC != null && subNewAC != null) {
                        processQueue.add(new Triangle(subC, subNewBC, subNewAC));
                    } else if (subNewAB != null) {
                        processQueue.add(new Triangle(subC, subA, subNewAB));
                        processQueue.add(new Triangle(subC, subB, subNewAB));
                    } else if (subNewAC != null) {
                        processQueue.add(new Triangle(subB, subA, subNewAC));
                        processQueue.add(new Triangle(subB, subC, subNewAC));
                    } else if (subNewBC != null) {
                        processQueue.add(new Triangle(subA, subB, subNewBC));
                        processQueue.add(new Triangle(subA, subC, subNewBC));
                    }
                }
            }

            result.put(roi, subsets);
        }

        return result;
    }

    private static double calculateDist2(final double[] arr1, final double[] arr2) {
        double result = 0;
        double temp;
        for (int i = 0; i < arr1.length; i++) {
            temp = arr2[i] - arr1[i];
            result += temp * temp;
        }
        return result;
    }

    private static boolean difExceedsTreshold(final AbstractSubset subsetA, final AbstractSubset subsetB, final double[][][] displacementResults) {
        final double[] centerA = subsetA.getCenter();
        final double[] centerB = subsetB.getCenter();
        final double dif2 = calculateDist2(
                displacementResults[(int) Math.round(centerA[0])][(int) Math.round(centerA[1])],
                displacementResults[(int) Math.round(centerB[0])][(int) Math.round(centerB[1])]);        
        return dif2 > DIF_TRESHOLD_2;
    }

    private static double calculateInvariant(final AbstractSubset subset, final double[][][] displacementResults) {
        final double[] center = subset.getCenter();
        final int x = (int) Math.round(center[0]);
        final int y = (int) Math.round(center[1]);
        double result = Math.abs(displacementResults[x][y][0] - displacementResults[x + 1][y][0]);
        result += Math.abs(displacementResults[x][y][1] - displacementResults[x][y + 1][1]);
        return result;
    }

    private static class Triangle {

        private final AbstractSubset subset1, subset2, subset3;

        public Triangle(AbstractSubset subset1, AbstractSubset subset2, AbstractSubset subset3) {
            this.subset1 = subset1;
            this.subset2 = subset2;
            this.subset3 = subset3;
        }

        public AbstractSubset getSubset1() {
            return subset1;
        }

        public AbstractSubset getSubset2() {
            return subset2;
        }

        public AbstractSubset getSubset3() {
            return subset3;
        }
    }

}
