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
import cz.tul.dic.engine.Engine;
import cz.tul.dic.engine.opencl.kernels.Kernel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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
public abstract class NewtonRaphson extends AbstractTaskSolver implements IGPUResultsReceiver {

    private static final int COUNT_ZERO_ORDER_LIMITS = 6;
    private static final int LIMITS_ROUNDS = 20;
    private static final double LIMIT_MIN_GROWTH = 0.001;
    private static final double LIMIT_DONE = 1 - LIMIT_MIN_GROWTH;
    private static final double STEP_INITIAL = 0.1;
    private static final double STEP_FIRST = 0.001;
    private static final double STEP_SECOND = 0.0001;
    protected FullTask fullTask;
    protected Map<AbstractSubset, CorrelationResult> results;
    protected Map<AbstractSubset, double[]> limits;
    protected double[] gpuData;

    @Override
    public List<CorrelationResult> solve(
            final Kernel kernel,
            final FullTask fullTask) throws ComputationException {
        if (fullTask.getSubsets().isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        this.fullTask = fullTask;
        final List<AbstractSubset> subsets = fullTask.getSubsets();
        final int subsetCount = subsets.size();

        results = new LinkedHashMap<>(subsetCount);
        limits = new LinkedHashMap<>(subsetCount);

        final DeformationDegree defDegree = DeformationUtils.getDegreeFromLimits(fullTask.getDeformationLimits().get(0));
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(defDegree);

        // estimate initial solution by direct search
        performInitialResultEstimation(coeffCount);
        notifyProgress(subsetCount, subsetCount);

        // prepare data for NR solver        
        prepareLimitsForNR(coeffCount);

        // initial data for NR solver
        Kernel.registerListener(this);
        final List<AbstractSubset> subsetsToCompute = Collections.synchronizedList(new ArrayList<>(subsets));
        final List<double[]> localLimits = new ArrayList<>(limits.values());
        computeTask(kernel, new FullTask(fullTask.getImageA(), fullTask.getImageB(), subsetsToCompute, localLimits));

        for (int i = 0; i < LIMITS_ROUNDS; i++) {
            makeStep(subsetsToCompute, defDegree);

            Logger.debug("Results for round {0}:", i);
            extractResults(subsetsToCompute, coeffCount);

            notifyProgress(subsetsToCompute.size(), subsetCount);

            if (subsetsToCompute.isEmpty()) {
                break;
            }
        }

        Kernel.deregisterListener(this);

        return new ArrayList<>(results.values());
    }

    protected abstract int getSetpCountForOneDimension();

    protected abstract RealVector generateNegativeGradient(final AbstractSubset subset);

    protected abstract RealMatrix generateHessianMatrix(final AbstractSubset subset);

    private void performInitialResultEstimation(final int coeffCount) throws ComputationException {
        final List<AbstractSubset> subsets = fullTask.getSubsets();
        final int subsetCount = subsets.size();

        double[] temp;
        List<double[]> zeroOrderLimits = new ArrayList<>(subsetCount);
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
        final List<CorrelationResult> localResults = AbstractTaskSolver.initSolver(Solver.COARSE_FINE).solve(new FullTask(fullTask.getImageA(), fullTask.getImageB(), fullTask.getSubsets(), zeroOrderLimits), subsetSize);
        sb.append("Initial results, step [").append(STEP_INITIAL).append("]:");
        CorrelationResult paddedResult;
        for (int i = 0; i < subsetCount; i++) {
            paddedResult = new CorrelationResult(localResults.get(i).getQuality(), Arrays.copyOf(localResults.get(i).getDeformation(), coeffCount));
            results.put(subsets.get(i), paddedResult);

            sb.append(i)
                    .append(" - ")
                    .append(localResults.get(i))
                    .append("; ");
        }
        Logger.debug(sb);
    }

    private void prepareLimitsForNR(final int coeffCount) {
        final List<AbstractSubset> subsets = fullTask.getSubsets();

        double[] newLimits;
        for (AbstractSubset subset : subsets) {
            newLimits = generateLimits(results.get(subset).getDeformation(), coeffCount, STEP_FIRST);
            limits.put(subset, newLimits);
        }
    }

    private double[] generateLimits(final double[] solution, final int coeffCount, final double step) {
        final int halfStepCount = getSetpCountForOneDimension() / 2;
        final double[] newLimits = new double[coeffCount * 3];
        for (int i = 0; i < coeffCount; i++) {
            newLimits[i * 3] = solution[i] - halfStepCount * step;
            newLimits[i * 3 + 1] = solution[i] + halfStepCount * step;
            newLimits[i * 3 + 2] = step;
        }
        return newLimits;
    }

    /**
     * Make one step using NewtonRaphson solver. Solution is found by solving
     * equation H(x(k)) * [x(k+1) - x(k)] = -G(x(k)). We find solution for
     * [x(k+1) - x(k)] and then add x(k).
     *
     * @param subsetsToCompute
     * @param defDegree
     * @throws ComputationException
     */
    private void makeStep(final List<AbstractSubset> subsetsToCompute, final DeformationDegree defDegree) throws ComputationException {        
        final Map<AbstractSubset, double[]> localLimits = new ConcurrentHashMap<>();
                
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(defDegree);

        final ExecutorService exec = Engine.getInstance().getExecutorService();
        final Set<Future<AbstractSubset>> steps = new HashSet<>(subsetsToCompute.size());

        final Iterator<AbstractSubset> it = subsetsToCompute.iterator();
        while (it.hasNext()) {
            final AbstractSubset as = it.next();
            steps.add(exec.submit(new StepMaker(as, localLimits, coeffCount)));
        }

        for (Future<AbstractSubset> fas : steps) {
            try {
                final AbstractSubset as = fas.get();
                subsetsToCompute.remove(as);
            } catch (InterruptedException | ExecutionException ex) {
                Logger.warn(ex, "Error retrieving result after computing new step.");
            }
        }

        if (!subsetsToCompute.isEmpty()) {
            final List<double[]> localLimitsList = new ArrayList<>(subsetsToCompute.size());
            for (AbstractSubset subset : subsetsToCompute) {
                localLimitsList.add(localLimits.get(subset));
            }
            computeTask(kernel, new FullTask(fullTask.getImageA(), fullTask.getImageB(), subsetsToCompute, localLimitsList));
        }
    }

    protected int computeDeformationCount(final DeformationDegree defDegree) {
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(defDegree);
        return (int) Math.pow(getSetpCountForOneDimension(), coeffCount);
    }

    private void extractResults(final List<AbstractSubset> subsetsToCompute, final int coeffCount) {
        AbstractSubset as;
        double[] currentLimits, newLimits;
        CorrelationResult currentResult, newResult;
        int resultIndex;
        double newCorrelationValue, improvement;
        long[] counts;

        int baseIndex = 0;

        final StringBuilder sb = new StringBuilder();
        final Iterator<AbstractSubset> it = subsetsToCompute.iterator();
        while (it.hasNext()) {
            as = it.next();
            currentResult = results.get(as);
            currentLimits = limits.get(as);
            counts = DeformationUtils.generateDeformationCounts(currentLimits);
            // check quality of new result
            resultIndex = baseIndex + generateIndex(counts, prepareIndices(counts));
            newCorrelationValue = gpuData[resultIndex];
            newResult = new CorrelationResult(newCorrelationValue, extractSolutionFromLimits(currentLimits));
            improvement = computeImprovement(currentResult.getQuality(), newCorrelationValue);
            sb.append(as)
                    .append(" -  from ")
                    .append(currentResult)
                    .append(" to ")
                    .append(newResult);
            if (improvement > LIMIT_MIN_GROWTH) {
                results.put(as, newResult);
                if (newCorrelationValue >= LIMIT_DONE) {
                    it.remove();
                    sb.append(", stop - quality good enough");
                }
            } else {
                if (currentLimits[2] > STEP_SECOND) {
                    newLimits = generateLimits(currentResult.getDeformation(), coeffCount, STEP_SECOND);
                    limits.put(as, newLimits);
                    sb.append(", increasing precision to ")
                            .append(STEP_SECOND);
                } else {
                    it.remove();
                    sb.append(", stop - low quality increment");
                }
            }

            sb.append("; ");
            baseIndex += counts[coeffCount];
        }

        Logger.debug(sb);
    }

    protected static double[] extractSolutionFromLimits(final double[] limits) {
        final double[] result = new double[limits.length / 3];
        for (int i = 0; i < limits.length / 3; i++) {
            result[i] = (limits[i * 3] + limits[i * 3 + 1]) / 2.0;
        }
        return result;
    }

    protected static int generateIndex(final long[] counts, final int[] indices) {
        int result = indices[0];
        for (int i = 1; i < indices.length; i++) {
            result *= counts[i];
            result += indices[i];
        }
        return result;
    }

    private double computeImprovement(final double oldResult, final double newResult) {
        return newResult - oldResult;
    }

    protected static int[] prepareIndices(final long[] counts) {
        final int[] indices = new int[counts.length - 1];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = (int) (counts[i] / 2);
        }
        return indices;
    }

    @Override
    public void dumpGpuResults(double[] resultData, List<AbstractSubset> subsets, List<double[]> deformationLimits) {
        this.gpuData = resultData;
    }

    @Override
    protected boolean needsBestResult() {
        return true;
    }

    private void notifyProgress(final int subsetToCompute, final int subsetCount) {
        if (subsetCount > 0) {
            setChanged();
            notifyObservers(0.5 + 0.5 * ((subsetCount - subsetToCompute) / subsetCount));
        }
    }

    private class StepMaker implements Callable<AbstractSubset> {

        private final AbstractSubset subset;
        private final Map<AbstractSubset, double[]> localLimits;
        final int coeffCount;

        public StepMaker(AbstractSubset subset, Map<AbstractSubset, double[]> localLimits, int coeffCount) {
            this.subset = subset;
            this.localLimits = localLimits;
            this.coeffCount = coeffCount;
        }

        @Override
        public AbstractSubset call() throws Exception {
            try {
                // prepare data for computation
                final RealVector negativeGradient = generateNegativeGradient(subset);
                final RealMatrix hessianMatrix = generateHessianMatrix(subset);
                // calculate next step
                final DecompositionSolver solver = new QRDecomposition(hessianMatrix).getSolver();
                final RealVector solutionVec = solver.solve(negativeGradient);
                solutionVec.add(new ArrayRealVector(results.get(subset).getDeformation()));
                // prepare data for next step
                final double[] solution = solutionVec.toArray();
                localLimits.put(subset, generateLimits(solution, coeffCount, limits.get(subset)[DeformationLimit.USTEP]));
            } catch (SingularMatrixException ex) {
                Logger.debug("{0} stop - singular hessian matrix.", subset);
                return subset;
            } catch (Exception ex) {
                Logger.warn(ex, "{0} stop, exception occured.", subset);
                return subset;
            }
            return null;
        }
    }
}
