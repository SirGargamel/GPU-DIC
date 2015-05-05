/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.solvers;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationLimit;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.debug.IGPUResultsReceiver;
import cz.tul.dic.engine.opencl.kernels.Kernel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class NewtonRaphson extends TaskSolver implements IGPUResultsReceiver {

    private static final int COUNT_ZERO_ORDER_LIMITS = 6;
    private static final int COUNT_STEP = 5;
    private static final int LIMITS_ROUNDS = 20;
    private static final double LIMIT_MIN_GROWTH = 0.01;
    private static final double STEP_INITIAL = 1;
    private static final double STEP_MINIMAL = 0.01;
    private float[] gpuData;

    @Override
    List<CorrelationResult> solve(
            final Image image1, final Image image2,
            final Kernel kernel, final List<Facet> facets,
            final List<double[]> deformationLimits, final DeformationDegree defDegree) throws ComputationException {
        final int facetCount = deformationLimits.size();
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(defDegree);

        final List<CorrelationResult> coarseResults = performInitialResultEstimation(image1, image2, kernel, facets, deformationLimits);
        notifyProgress(facetCount, facetCount);

        double[] temp;
        final List<double[]> limitsList = new ArrayList<>(facetCount);
        final List<double[]> solutionList = new ArrayList<>(facetCount);
        List<int[]> countsList;
        double[] newLimits, coarseResult, solution;
        for (int i = 0; i < facetCount; i++) {
            coarseResult = coarseResults.get(i).getDeformation();
            temp = deformationLimits.get(i);

            newLimits = new double[coeffCount * 3];
            System.arraycopy(temp, 0, newLimits, 0, Math.min(temp.length, newLimits.length));
            for (int j = 0; j < coeffCount; j++) {
                newLimits[j * 3 + 2] /= 10.0;
            }

            solution = Arrays.copyOf(coarseResult, coeffCount);
            solutionList.add(solution);
            limitsList.add(generateLimits(solution, newLimits));
        }
        countsList = DeformationUtils.generateDeformationCounts(limitsList);

        final List<Facet> facetsToCompute = new ArrayList<>(facets);
        List<CorrelationResult> results = coarseResults;
        RealVector gradient, solutionVec;
        RealMatrix hessianMatrix;
        DecompositionSolver solver;
        CorrelationResult newResult;
        double[] limits;
        int[] counts;
        float increment;
        Iterator<Facet> it;
        Facet f;
        int facetIndexGlobal, facetIndexLocal;
        final List<Facet> finishedFacets = new LinkedList<>();

        Kernel.registerListener(this);

        final StringBuilder sb = new StringBuilder();
        final long time = System.nanoTime();
        int baseIndex, resultIndex = 0, counterFinished;
        for (int i = 0; i < LIMITS_ROUNDS; i++) {
            sb.setLength(0);
            baseIndex = 0;
            facetIndexLocal = 0;
            counterFinished = 0;
            finishedFacets.clear();

            computeTask(image1, image2, kernel, facetsToCompute, limitsList, defDegree);

            it = facetsToCompute.iterator();
            sb.append("Results for round ").append(i).append(": ");
            while (it.hasNext()) {
                f = it.next();
                facetIndexGlobal = facets.indexOf(f);
                counts = countsList.get(facetIndexLocal);

                try {
                    // store results with computed quality  
                    resultIndex = baseIndex + generateIndex(counts, prepareIndices(counts));
                    newResult = new CorrelationResult(gpuData[resultIndex], solutionList.get(facetIndexLocal));
                    increment = newResult.getValue() - results.get(facetIndexGlobal).getValue();
                    sb.append(facetIndexGlobal)
                            .append(" - ")
                            .append(results.get(facetIndexGlobal));
                    results.set(facetIndexGlobal, newResult);
                    if (increment > LIMIT_MIN_GROWTH) {
                        // prepare data for next step
                        limits = limitsList.get(facetIndexLocal);
                        gradient = generateGradient(gpuData, facetIndexLocal, facetCount, counts, limits);
                        hessianMatrix = generateHessianMatrix(gpuData, facetIndexLocal, facetCount, counts, limits);
                        // calculate next step
                        solver = new QRDecomposition(hessianMatrix).getSolver();
                        solutionVec = solver.solve(gradient);
                        // prepare data for next step
                        solution = solutionVec.toArray();
                        solutionList.set(facetIndexLocal, solution);
                        limitsList.set(facetIndexLocal, generateLimits(solution, limits));
                    } else if (i > 0) {
                        sb.append(", stop - low quality increment");
                        finishedFacets.add(f);
                        counterFinished++;
                    }
                } catch (SingularMatrixException ex) {
                    sb.append(", stop - singular hessian matrix");
                    finishedFacets.add(f);
                    counterFinished++;
                }

                sb.append("; ");
                facetIndexLocal++;
                baseIndex += counts[coeffCount];
            }
            for (Facet facet : finishedFacets) {
                facetIndexLocal = facetsToCompute.indexOf(facet);
                facetsToCompute.remove(facetIndexLocal);
                limitsList.remove(facetIndexLocal);
            }

            sb.append("\n Stopped ");
            sb.append(counterFinished);
            sb.append(" facets.");
            Logger.trace(sb);
            
            notifyProgress(facetsToCompute.size(), facetCount);

            if (facetsToCompute.isEmpty()) {
                break;
            }

            countsList = DeformationUtils.generateDeformationCounts(limitsList);
        }
        Logger.trace("Round time: " + ((System.nanoTime() - time) / 1_000_000) + "ms.");

        Kernel.deregisterListener(this);

        return results;
    }

    List<CorrelationResult> performInitialResultEstimation(Image image1, Image image2, Kernel kernel, List<Facet> facets, List<double[]> deformationLimits) throws ComputationException {
        final int facetCount = deformationLimits.size();

        double[] temp;
        List<double[]> zeroOrderLimits = new ArrayList<>(facetCount);
        List<CorrelationResult> results;
        final StringBuilder sb = new StringBuilder();

        // initial pixel step
        double step = STEP_INITIAL;
        for (double[] dA : deformationLimits) {
            temp = new double[COUNT_ZERO_ORDER_LIMITS];
            System.arraycopy(dA, 0, temp, 0, COUNT_ZERO_ORDER_LIMITS);
            temp[DeformationLimit.UMIN] = Math.floor(temp[DeformationLimit.UMIN]);
            temp[DeformationLimit.UMAX] = Math.ceil(temp[DeformationLimit.UMAX]);
            temp[DeformationLimit.USTEP] = step;
            temp[DeformationLimit.VMIN] = Math.floor(temp[DeformationLimit.VMIN]);
            temp[DeformationLimit.VMAX] = Math.ceil(temp[DeformationLimit.VMAX]);
            temp[DeformationLimit.VSTEP] = step;
            zeroOrderLimits.add(temp);
        }
        results = computeTask(image1, image2, kernel, facets, zeroOrderLimits, DeformationDegree.ZERO);
        sb.append("Initial results, step [").append(step).append("]:");
        for (int i = 0; i < facetCount; i++) {
            sb.append(i)
                    .append(" - ")
                    .append(results.get(i))
                    .append("; ");
        }

        double minStep = 1;
        for (double[] dA : deformationLimits) {
            minStep = Math.min(minStep, Math.min(dA[DeformationLimit.USTEP], dA[DeformationLimit.VSTEP]));
        }

        //sub-pixel stepping
        double[] coarseResult, defLimits;
        int l;
        do {
            step /= 10.0;
            if (step < minStep) {
                if (step * 10 == minStep) {
                    break;
                } else {
                    step = minStep;
                }
            }

            zeroOrderLimits.clear();
            zeroOrderLimits = new ArrayList<>(facetCount);

            for (int i = 0; i < facetCount; i++) {
                coarseResult = results.get(i).getDeformation();
                defLimits = deformationLimits.get(i);
                temp = new double[COUNT_ZERO_ORDER_LIMITS];

                temp[DeformationLimit.UMIN] = Math.max(coarseResult[Coordinates.X] - (10 * step), defLimits[DeformationLimit.UMIN]);
                temp[DeformationLimit.UMAX] = Math.min(coarseResult[Coordinates.X] + (10 * step), defLimits[DeformationLimit.UMAX]);
                temp[DeformationLimit.USTEP] = step;
                temp[DeformationLimit.VMIN] = Math.max(coarseResult[Coordinates.Y] - (10 * step), defLimits[DeformationLimit.VMIN]);
                temp[DeformationLimit.VMAX] = Math.min(coarseResult[Coordinates.Y] + (10 * step), defLimits[DeformationLimit.VMAX]);
                temp[DeformationLimit.VSTEP] = step;

                zeroOrderLimits.add(temp);
            }
            results = computeTask(image1, image2, kernel, facets, zeroOrderLimits, DeformationDegree.ZERO);

            sb.append("Finer results, step [").append(step).append("]:");
            for (int i = 0; i < facetCount; i++) {
                sb.append(i)
                        .append(" - ")
                        .append(results.get(i))
                        .append("; ");
            }

        } while (step > STEP_MINIMAL);

        return results;
    }

    protected static double[] generateLimits(final double[] solution, final double[] oldLimits) {
        final int halfStep = COUNT_STEP / 2;
        final double[] newLimits = Arrays.copyOf(oldLimits, oldLimits.length);
        double step;
        for (int i = 0; i < solution.length; i++) {
            step = oldLimits[i * 3 + 2];
            newLimits[i * 3] = solution[i] - halfStep * step;
            newLimits[i * 3 + 1] = solution[i] + halfStep * step;
        }
        return newLimits;
    }

    protected static int generateIndex(final int[] counts, final int[] indices) {
        int result = indices[0];
        for (int i = 1; i < indices.length; i++) {
            result *= counts[i];
            result += indices[i];
        }
        return result;
    }

    // central difference
    protected RealVector generateGradient(float[] resultData, final int facetIndex, final int facetCount, final int[] counts, final double[] deformationLimits) {
        final int coeffCount = counts.length - 1;
        final double[] data = new double[coeffCount];

        final int deformationCount = resultData.length / facetCount;
        final int resultsBase = facetIndex * deformationCount;
        final int[] indices = prepareIndices(counts);
        for (int i = 0; i < coeffCount; i++) {
            // right index
            indices[i]++;
            data[i] = resultData[resultsBase + generateIndex(counts, indices)];
            // left index
            indices[i] -= 2;
            data[i] -= resultData[resultsBase + generateIndex(counts, indices)];
            data[i] /= 2 * deformationLimits[i * 3 + 2];
            indices[i]++;
        }
        return new ArrayRealVector(data);
    }

    protected static int[] prepareIndices(final int[] counts) {
        final int[] indices = new int[counts.length - 1];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = counts[i] / 2;
        }
        return indices;
    }

    protected RealMatrix generateHessianMatrix(float[] resultData, final int facetIndex, final int facetCount, final int[] counts, final double[] deformationLimits) {
        final int coeffCount = counts.length - 1;
        final double[][] data = new double[coeffCount][coeffCount];

        final int deformationCount = resultData.length / facetCount;
        final int resultsBase = facetIndex * deformationCount;
        final int[] indices = prepareIndices(counts);

        // upper triangle approach
        double step;
        for (int i = 0; i < coeffCount; i++) {
            step = deformationLimits[i * 3 + 2];

            indices[i]++;
            data[i][i] = resultData[resultsBase + generateIndex(counts, indices)];
            indices[i] -= 2;
            data[i][i] += resultData[resultsBase + generateIndex(counts, indices)];
            indices[i]++;
            data[i][i] -= 2 * resultData[resultsBase + generateIndex(counts, indices)];
            data[i][i] /= step * step * 4;
        }
        for (int i = 0; i < coeffCount; i++) {
            for (int j = i + 1; j < coeffCount; j++) {
                indices[i]++;
                indices[j]++;
                data[i][j] = resultData[resultsBase + generateIndex(counts, indices)];
                indices[i] -= 2;
                indices[j] -= 2;
                data[i][j] += resultData[resultsBase + generateIndex(counts, indices)];
                indices[j] += 2;
                data[i][j] -= resultData[resultsBase + generateIndex(counts, indices)];
                indices[i] += 2;
                indices[j] -= 2;
                data[i][j] -= resultData[resultsBase + generateIndex(counts, indices)];
                indices[i]--;
                indices[j]++;

                data[i][j] /= (deformationLimits[i * 3 + 2] + deformationLimits[j * 3 + 2]) * (deformationLimits[i * 3 + 2] + deformationLimits[j * 3 + 2]);
                data[i][j] /= 4.0;
                data[j][i] = data[i][j];
            }
        }

        return new Array2DRowRealMatrix(data, false);
    }

    @Override
    public void dumpGpuResults(float[] resultData, List<Facet> facets, List<double[]> deformationLimits) {
        this.gpuData = resultData;
    }

    @Override
    boolean needsBestResult() {
        return true;
    }
    
    private void notifyProgress(final int facetsToCompute, final int facetCount) {
        setChanged();
        notifyObservers(0.5 + 0.5 * ((facetCount - facetsToCompute) / facetCount));
    }

}
