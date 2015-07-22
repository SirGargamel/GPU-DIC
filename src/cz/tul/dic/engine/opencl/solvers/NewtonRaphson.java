/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.solvers;

import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.ComputationException;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationLimit;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.debug.IGPUResultsReceiver;
import cz.tul.dic.data.task.FullTask;
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
public class NewtonRaphson extends AbstractTaskSolver implements IGPUResultsReceiver {

    private static final int COUNT_ZERO_ORDER_LIMITS = 6;
    private static final int COUNT_STEP = 5;
    private static final int LIMITS_ROUNDS = 20;
    private static final double LIMIT_MIN_GROWTH = 0.01;
    private static final double STEP_INITIAL = 1;
    private static final double STEP_FIRST = 0.1;
    private static final double STEP_SECOND = 0.01;
    private double[] gpuData;

    @Override
    public List<CorrelationResult> solve(
            final Kernel kernel,
            final FullTask fullTask, DeformationDegree defDegree) throws ComputationException {
        final int subsetCount = fullTask.getDeformationLimits().size();
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(defDegree);

        // estimate initial solution by direct search
        final List<CorrelationResult> coarseResults = performInitialResultEstimation(kernel, fullTask);
        notifyProgress(subsetCount, subsetCount);

        // prepare limits data for NR solver
        double[] temp;
        final List<double[]> limitsList = new ArrayList<>(subsetCount);
        final List<double[]> solutionList = new ArrayList<>(subsetCount);
        List<long[]> countsList;
        double[] newLimits, coarseResult, solution;
        for (int i = 0; i < subsetCount; i++) {
            coarseResult = coarseResults.get(i).getDeformation();
            temp = fullTask.getDeformationLimits().get(i);

            newLimits = new double[coeffCount * 3];
            System.arraycopy(temp, 0, newLimits, 0, Math.min(temp.length, newLimits.length));
            for (int j = 0; j < coeffCount; j++) {
                newLimits[j * 3 + 2] = STEP_FIRST;
            }

            solution = Arrays.copyOf(coarseResult, coeffCount);
            solutionList.add(solution);
            limitsList.add(generateLimits(solution, newLimits));
        }
        countsList = DeformationUtils.generateDeformationCounts(limitsList);

        final List<AbstractSubset> subsetsToCompute = new ArrayList<>(fullTask.getSubsets());
        List<CorrelationResult> results = coarseResults;
        RealVector gradient, solutionVec;
        RealMatrix hessianMatrix;
        DecompositionSolver solver;
        CorrelationResult newResult;
        double[] limits;
        long[] counts;
        double increment;
        Iterator<AbstractSubset> it;
        AbstractSubset f;
        int subsetIndexGlobal, subsetIndexLocal;
        final List<AbstractSubset> finishedFacets = new LinkedList<>();

        Kernel.registerListener(this);

        final StringBuilder sb = new StringBuilder();
        final long time = System.nanoTime();
        int baseIndex, resultIndex, counterFinished;
        for (int i = 0; i < LIMITS_ROUNDS; i++) {
            sb.setLength(0);
            baseIndex = 0;
            subsetIndexLocal = 0;
            counterFinished = 0;
            finishedFacets.clear();

            computeTask(kernel, new FullTask(fullTask.getImageA(), fullTask.getImageB(), subsetsToCompute, limitsList), defDegree);

            it = subsetsToCompute.iterator();
            sb.append("Results for round ").append(i).append(": ");
            while (it.hasNext()) {
                f = it.next();
                subsetIndexGlobal = fullTask.getSubsets().indexOf(f);
                counts = countsList.get(subsetIndexLocal);

                try {
                    // store results with computed quality  
                    resultIndex = baseIndex + generateIndex(counts, prepareIndices(counts));
                    newResult = new CorrelationResult(gpuData[resultIndex], solutionList.get(subsetIndexLocal));
                    increment = computeError(results.get(subsetIndexGlobal).getValue(), newResult.getValue());
                    sb.append(subsetIndexGlobal)
                            .append(" - ")
                            .append(results.get(subsetIndexGlobal));
                    results.set(subsetIndexGlobal, newResult);
                    limits = limitsList.get(subsetIndexLocal);
                    if (increment > LIMIT_MIN_GROWTH) {
                        // prepare data for next step                        
                        gradient = generateGradient(gpuData, subsetIndexLocal, subsetCount, counts, limits);
                        hessianMatrix = generateHessianMatrix(gpuData, subsetIndexLocal, subsetCount, counts, limits);
                        // calculate next step
                        solver = new QRDecomposition(hessianMatrix).getSolver();
                        solutionVec = solver.solve(gradient);
                        // prepare data for next step
                        solution = solutionVec.toArray();
                        solutionList.set(subsetIndexLocal, solution);
                        limitsList.set(subsetIndexLocal, generateLimits(solution, limits));
                    } else if (i > 0) {
                        if (limits[2] > STEP_SECOND) {
                            for (int j = 0; j < coeffCount; j++) {
                                limits[j * 3 + 2] = STEP_SECOND;
                            }
                            limitsList.set(subsetIndexLocal, generateLimits(solutionList.get(subsetIndexLocal), limits));
                            sb.append(", increasing precision to ")
                                    .append(STEP_SECOND);
                        } else {
                            finishedFacets.add(f);
                            counterFinished++;
                            sb.append(", stop - low quality increment");
                        }
                    }
                } catch (SingularMatrixException ex) {
                    finishedFacets.add(f);
                    counterFinished++;
                    sb.append(", stop - singular hessian matrix");
                }

                sb.append("; ");
                subsetIndexLocal++;
                baseIndex += counts[coeffCount];
            }
            for (AbstractSubset subset : finishedFacets) {
                subsetIndexLocal = subsetsToCompute.indexOf(subset);
                subsetsToCompute.remove(subsetIndexLocal);
                limitsList.remove(subsetIndexLocal);
            }

            sb.append("\n Stopped ");
            sb.append(counterFinished);
            sb.append(" subsets.");
            Logger.trace(sb);

            notifyProgress(subsetsToCompute.size(), subsetCount);

            if (subsetsToCompute.isEmpty()) {
                break;
            }

            countsList = DeformationUtils.generateDeformationCounts(limitsList);
        }
        Logger.trace("Round time: " + ((System.nanoTime() - time) / 1_000_000) + "ms.");

        Kernel.deregisterListener(this);

        return results;
    }

    private List<CorrelationResult> performInitialResultEstimation(final Kernel kernel, final FullTask fullTask) throws ComputationException {
        final int subsetCount = fullTask.getSubsets().size();

        double[] temp;
        List<double[]> zeroOrderLimits = new ArrayList<>(subsetCount);
        List<CorrelationResult> results;
        final StringBuilder sb = new StringBuilder();

        // initial pixel step        
        for (double[] dA : fullTask.getDeformationLimits()) {
            temp = new double[COUNT_ZERO_ORDER_LIMITS];
            System.arraycopy(dA, 0, temp, 0, COUNT_ZERO_ORDER_LIMITS);
            temp[DeformationLimit.UMIN] = Math.floor(temp[DeformationLimit.UMIN]);
            temp[DeformationLimit.UMAX] = Math.ceil(temp[DeformationLimit.UMAX]);
            temp[DeformationLimit.USTEP] = STEP_INITIAL;
            temp[DeformationLimit.VMIN] = Math.floor(temp[DeformationLimit.VMIN]);
            temp[DeformationLimit.VMAX] = Math.ceil(temp[DeformationLimit.VMAX]);
            temp[DeformationLimit.VSTEP] = STEP_INITIAL;
            zeroOrderLimits.add(temp);
        }
        results = computeTask(kernel, new FullTask(fullTask.getImageA(), fullTask.getImageB(), fullTask.getSubsets(), zeroOrderLimits), DeformationDegree.ZERO);
        sb.append("Initial results, step [").append(STEP_INITIAL).append("]:");
        for (int i = 0; i < subsetCount; i++) {
            sb.append(i)
                    .append(" - ")
                    .append(results.get(i))
                    .append("; ");
        }

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

    protected static int generateIndex(final long[] counts, final int[] indices) {
        int result = indices[0];
        for (int i = 1; i < indices.length; i++) {
            result *= counts[i];
            result += indices[i];
        }
        return result;
    }

    private double computeError(final double oldResult, final double newResult) {
        return (oldResult - newResult) / oldResult;
    }

    // central difference
    protected RealVector generateGradient(double[] resultData, final int subsetIndex, final int subsetCount, final long[] counts, final double[] deformationLimits) {
        final int coeffCount = counts.length - 1;
        final double[] data = new double[coeffCount];

        final int deformationCount = resultData.length / subsetCount;
        final int resultsBase = subsetIndex * deformationCount;
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

    protected static int[] prepareIndices(final long[] counts) {
        final int[] indices = new int[counts.length - 1];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = (int) (counts[i] / 2);
        }
        return indices;
    }

    protected RealMatrix generateHessianMatrix(double[] resultData, final int subsetIndex, final int subsetCount, final long[] counts, final double[] deformationLimits) {
        final int coeffCount = counts.length - 1;
        final double[][] data = new double[coeffCount][coeffCount];

        final int deformationCount = resultData.length / subsetCount;
        final int resultsBase = subsetIndex * deformationCount;
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
    public void dumpGpuResults(double[] resultData, List<AbstractSubset> subsets, List<double[]> deformationLimits) {
        this.gpuData = resultData;
    }

    @Override
    boolean needsBestResult() {
        return true;
    }

    private void notifyProgress(final int subsetToCompute, final int subsetCount) {
        if (subsetCount > 0) {
            setChanged();
            notifyObservers(0.5 + 0.5 * ((subsetCount - subsetToCompute) / subsetCount));
        }
    }

}
