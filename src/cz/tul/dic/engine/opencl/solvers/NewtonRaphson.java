/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.solvers;

import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.ComputationException;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.deformation.DeformationOrder;
import cz.tul.dic.data.deformation.DeformationLimit;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.debug.IGPUResultsReceiver;
import cz.tul.dic.data.task.FullTask;
import cz.tul.dic.engine.Engine;
import cz.tul.pj.journal.Journal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.apache.commons.math3.exception.InsufficientDataException;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularMatrixException;

/**
 *
 * @author Petr Ječmen
 */
public abstract class NewtonRaphson extends AbstractTaskSolver implements IGPUResultsReceiver {

    private static final int COUNT_ZERO_ORDER_LIMITS = 6;
    private static final int LIMITS_ITERATIONS = 10;
    private static final double LIMIT_MIN_IMPROVEMENT = 0.01;
    private static final double LIMIT_Q_DONE = 1 - LIMIT_MIN_IMPROVEMENT;
    private static final double STEP_FIRST = 0.01;
    private static final double STEP_SECOND = 0.001;
    protected static final int STEP_WEIGHT = 1;
    private final Set<AbstractSubset> smallerStep;
    protected DeformationOrder deformationOrder;

    public NewtonRaphson() {
        smallerStep = new HashSet<>();
    }

    @Override
    public List<CorrelationResult> solve() throws ComputationException {
        final int subsetCount = subsetsToCompute.size();
        deformationOrder = DeformationUtils.getOrderFromLimits(fullTask.getDeformationLimits().get(0));

        smallerStep.clear();

        // estimate initial solution by direct search
        prepareInitialResults();
        notifyProgress(subsetCount, subsetCount);

        // prepare data for NR solver        
        prepareDeformations();

        // initial data for NR solver
        registerGPUDataListener(this);
        computeTask(kernel, new ComputationTask(fullTask.getImageA(), fullTask.getImageB(), subsetsToCompute, fullTask.getSubsetWeights(), new ArrayList<>(deformations.values()), deformationOrder, false));

        for (int i = 0; i < LIMITS_ITERATIONS; i++) {
            makeStep(subsetsToCompute, deformationOrder);

            notifyProgress(subsetsToCompute.size(), subsetCount);

            if (subsetsToCompute.isEmpty()) {
                break;
            }
        }

        deregisterGPUDataListener(this);

        return new ArrayList<>(results.values());
    }

    private void prepareInitialResults() throws ComputationException {
        final List<AbstractSubset> subsets = fullTask.getSubsets();
        final int subsetCount = subsets.size();
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(deformationOrder);

        double[] temp;
        List<double[]> zeroOrderLimits = new ArrayList<>(subsetCount);

        // initial pixel step        
        for (double[] dA : fullTask.getDeformationLimits()) {
            temp = new double[COUNT_ZERO_ORDER_LIMITS];
            System.arraycopy(dA, 0, temp, 0, COUNT_ZERO_ORDER_LIMITS);
            temp[DeformationLimit.UMIN] = Math.floor(temp[DeformationLimit.UMIN]);
            temp[DeformationLimit.UMAX] = Math.ceil(temp[DeformationLimit.UMAX]);
            temp[DeformationLimit.VMIN] = Math.floor(temp[DeformationLimit.VMIN]);
            temp[DeformationLimit.VMAX] = Math.ceil(temp[DeformationLimit.VMAX]);
            zeroOrderLimits.add(temp);
        }
        final List<CorrelationResult> localResults = AbstractTaskSolver.initSolver(Solver.COARSE_FINE).solve(
                new FullTask(fullTask.getImageA(), fullTask.getImageB(), fullTask.getSubsets(), fullTask.getSubsetWeights(), zeroOrderLimits));
        CorrelationResult paddedResult, currentResult;
        for (int i = 0; i < subsetCount; i++) {
            // set the length of the result to match correlation degree
            currentResult = localResults.get(i);
            paddedResult = new CorrelationResult(currentResult.getQuality(), Arrays.copyOf(currentResult.getDeformation(), coeffCount));
            results.put(subsets.get(i), paddedResult);
            addSubsetResultInfo(fullTask.getSubsets().get(i), localResults.get(i));
        }
    }

    private void prepareDeformations() {
        final List<AbstractSubset> subsets = fullTask.getSubsets();

        double[] deformation;
        double step;
        for (AbstractSubset subset : subsets) {
            step = smallerStep.contains(subset) ? STEP_SECOND : STEP_FIRST;
            deformation = generateDeformations(results.get(subset).getDeformation(), step);
            deformations.put(subset, deformation);
        }
    }

    /**
     * Make one step using NewtonRaphson solver. Solution is found by solving
     * equation H(x(k)) * [x(k+1) - x(k)] = -G(x(k)). We find solution for
     * [x(k+1) - x(k)] and then add x(k).
     *
     * @param subsetsToCompute
     * @param order
     * @throws ComputationException
     */
    private void makeStep(final List<AbstractSubset> subsetsToCompute, final DeformationOrder order) throws ComputationException {
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(order);

        final ExecutorService exec = Engine.getInstance().getExecutorService();
        final Set<Future<AbstractSubset>> steps = new HashSet<>(subsetsToCompute.size());

        final Iterator<AbstractSubset> it = subsetsToCompute.iterator();
        while (it.hasNext()) {
            final AbstractSubset as = it.next();
            steps.add(exec.submit(new StepMaker(as, coeffCount)));
        }

        for (Future<AbstractSubset> fas : steps) {
            try {
                final AbstractSubset as = fas.get();
                subsetsToCompute.remove(as);
            } catch (InterruptedException | ExecutionException ex) {
                Journal.addDataEntry(ex, "Solver error", "Error retrieving result after computing new step.");
            }
        }

        if (!subsetsToCompute.isEmpty()) {
            final List<double[]> deformationsToCompute = new ArrayList<>(subsetsToCompute.size());
            final List<Integer> weightToCompute = new ArrayList<>(subsetsToCompute.size());
            for (AbstractSubset subset : subsetsToCompute) {
                deformationsToCompute.add(deformations.get(subset));
                weightToCompute.add(weights.get(subset));
            }
            computeTask(kernel, new ComputationTask(fullTask.getImageA(), fullTask.getImageB(), subsetsToCompute, weightToCompute, deformationsToCompute, order, false));
        }
    }

    // UTILS
    protected int getCoeffCount() {
        return DeformationUtils.getDeformationCoeffCount(deformationOrder);
    }

    protected double[] extractDeformation(final AbstractSubset subset) {
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(deformationOrder);
        final double[] result = new double[coeffCount];
        System.arraycopy(deformations.get(subset), 0, result, 0, coeffCount);
        return result;
    }

    // ABSTRACT
    protected abstract RealVector generateNegativeGradient(final AbstractSubset subset, final double step);

    protected abstract RealMatrix generateHessianMatrix(final AbstractSubset subset, final double step);

    protected abstract double[] generateDeformations(final double[] solution, final double step);

    ///// MISC
    @Override
    public void dumpGpuResults(double[] resultData, List<AbstractSubset> subsets, List<double[]> deformationLimits) {
        this.gpuData = resultData;
    }

    @Override
    protected boolean needsBestResult() {
        return false;
    }

    private void notifyProgress(final int subsetToCompute, final int subsetCount) {
        if (subsetCount > 0) {
            setChanged();
            notifyObservers(0.5 + 0.5 * ((subsetCount - subsetToCompute) / subsetCount));
        }
    }

    private class StepMaker implements Callable<AbstractSubset> {

        private final AbstractSubset subset;
        final int coeffCount;

        public StepMaker(AbstractSubset subset, int coeffCount) {
            this.subset = subset;
            this.coeffCount = coeffCount;
        }

        @Override
        public AbstractSubset call() throws Exception {
            try {
                final CorrelationResult oldResult = results.get(subset);

                final int resultIndex = generateResultIndex(subset);
                final double currentCorrelationValue = gpuData[resultIndex];
                final double[] currentDeformation = extractDeformation(subset);
                final CorrelationResult currentResult = new CorrelationResult(currentCorrelationValue, currentDeformation);
                results.put(subset, currentResult);
                if (currentCorrelationValue > LIMIT_Q_DONE) {
                    return subset;
                }

                final double improvement = computeImprovement(oldResult.getDeformation(), currentResult.getDeformation());
                if (improvement > LIMIT_MIN_IMPROVEMENT) {
                    results.put(subset, currentResult);
                    addSubsetResultInfo(subset, currentResult);
                } else if (!smallerStep.contains(subset)) {
                    smallerStep.add(subset);
                } else {
                    addSubsetTerminationInfo(subset, "Low quality increment");
                    return subset;
                }

                // prepare data for computation
                final double step = smallerStep.contains(subset) ? STEP_SECOND : STEP_FIRST;
                final RealVector negativeGradient = generateNegativeGradient(subset, step);
                final RealMatrix hessianMatrix = generateHessianMatrix(subset, step);
                // calculate next step
                final DecompositionSolver solver = new QRDecomposition(hessianMatrix).getSolver();
                final RealVector solutionVec = solver.solve(negativeGradient);
                double[] old = results.get(subset).getDeformation();
                solutionVec.add(new ArrayRealVector(old));
                // prepare data for next step
                final double[] solution = solutionVec.toArray();
                deformations.put(subset, generateDeformations(solution, step));
            } catch (InsufficientDataException ex) {
                addSubsetTerminationInfo(subset, "Cannot create interpolator.");
                return subset;
            } catch (SingularMatrixException ex) {
                addSubsetTerminationInfo(subset, "Singular hessian matrix");
                return subset;
            } catch (Exception ex) {
                if (ex.getStackTrace().length == 0) {
                    Journal.addEntry("Subset computation stopped", "{0} stop, exception occured - {1}, no stack trace...", subset, ex);
                } else {
                    Journal.addDataEntry(ex,"Subset computation stopped",  "{0} stop, exception occured.", subset);
                }
                addSubsetTerminationInfo(subset, "StepMaker exception - " + ex);
                return subset;
            }
            return null;
        }

        private int generateResultIndex(final AbstractSubset subset) {
            return (int) (subsetsToCompute.indexOf(subset) * getDeformationCount());
        }

        private double computeImprovement(final double[] oldResult, final double[] newResult) {
            double sum = 0, impr;
            for (int i = 0; i < oldResult.length; i++) {
                impr = computeImprovement(oldResult[i], newResult[i]);
                sum += impr * impr;
            }
            return Math.sqrt(sum);
        }

        private double computeImprovement(final double oldResult, final double newResult) {
            return (newResult - oldResult) / newResult;
        }

    }
}
