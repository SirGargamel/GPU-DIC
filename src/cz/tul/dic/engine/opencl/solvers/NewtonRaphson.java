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
    private static final int LIMITS_ITERATIONS = 10;
    private static final double LIMIT_MIN_IMPROVEMENT = 0.01;
    private static final double LIMIT_Q_DONE = 1 - LIMIT_MIN_IMPROVEMENT;
    private static final double STEP_INITIAL = 1;
    private static final double STEP_FIRST = 0.001;
    private static final double STEP_SECOND = 0.0001;
    protected static final int STEP_WEIGHT = 1;
    protected FullTask fullTask;
    protected Map<AbstractSubset, CorrelationResult> results;
    protected Map<AbstractSubset, double[]> limits;
    protected Map<AbstractSubset, Integer> weights;
    protected double[] gpuData;
    protected List<AbstractSubset> subsetsToCompute;
    protected boolean usesWeights;
    protected DeformationDegree defDegree;
    protected final int stepCountForOneDimension;

    public NewtonRaphson(int stepCountForOneDImension) {
        this.stepCountForOneDimension = stepCountForOneDImension;
    }

    @Override
    public List<CorrelationResult> solve(            
            final FullTask fullTask,
            final boolean usesWeights) throws ComputationException {
        if (fullTask.getSubsets().isEmpty()) {
            return new ArrayList<>(0);
        }
        this.fullTask = fullTask;
        final List<AbstractSubset> subsets = fullTask.getSubsets();
        final int subsetCount = subsets.size();        

        results = new LinkedHashMap<>(subsetCount);
        limits = new LinkedHashMap<>(subsetCount);
        weights = new LinkedHashMap<>(subsetCount);

        defDegree = DeformationUtils.getDegreeFromLimits(fullTask.getDeformationLimits().get(0));
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(defDegree);

        // estimate initial solution by direct search
        performInitialResultEstimation();
        notifyProgress(subsetCount, subsetCount);

        // prepare data for NR solver        
        prepareInitialLimitsForNR(coeffCount);

        // initial data for NR solver
        Kernel.registerListener(this);
        subsetsToCompute = Collections.synchronizedList(new ArrayList<>(subsets));
        computeAndArrangeData(subsetsToCompute, fullTask.getSubsetWeights(), new ArrayList<>(limits.values()));

        for (int i = 0; i < LIMITS_ITERATIONS; i++) {
            makeStep(subsetsToCompute, defDegree);

            extractResults(subsetsToCompute, coeffCount);

            notifyProgress(subsetsToCompute.size(), subsetCount);

            if (subsetsToCompute.isEmpty()) {
                break;
            }
        }

        Kernel.deregisterListener(this);

        return new ArrayList<>(results.values());
    }

    private void performInitialResultEstimation() throws ComputationException {
        final List<AbstractSubset> subsets = fullTask.getSubsets();
        final int subsetCount = subsets.size();
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(defDegree);

        double[] temp;
        List<double[]> zeroOrderLimits = new ArrayList<>(subsetCount);

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
        final List<CorrelationResult> localResults = AbstractTaskSolver.initSolver(Solver.COARSE_FINE).solve(
                new FullTask(fullTask.getImageA(), fullTask.getImageB(), fullTask.getSubsets(), fullTask.getSubsetWeights(), zeroOrderLimits), subsetSize);
        CorrelationResult paddedResult, currentResult;
        for (int i = 0; i < subsetCount; i++) {
            // set the length of the result to match correlation degree
            currentResult = localResults.get(i);
            paddedResult = new CorrelationResult(currentResult.getQuality(), Arrays.copyOf(currentResult.getDeformation(), coeffCount));
            results.put(subsets.get(i), paddedResult);
            addSubsetResult(fullTask.getSubsets().get(i), localResults.get(i));
        }
    }

    private void prepareInitialLimitsForNR(final int coeffCount) {
        final List<AbstractSubset> subsets = fullTask.getSubsets();

        double[] newLimits;
        for (AbstractSubset subset : subsets) {
            newLimits = generateLimits(results.get(subset).getDeformation(), coeffCount, STEP_FIRST);
            limits.put(subset, newLimits);
        }
    }

    private double[] generateLimits(final double[] solution, final int coeffCount, final double step) {        
        final boolean even = stepCountForOneDimension % 2 == 0;
        final int halfStepCount = stepCountForOneDimension / 2;
        final double[] newLimits = new double[coeffCount * 3];
        for (int i = 0; i < coeffCount; i++) {
            newLimits[i * 3] = solution[i] - (halfStepCount * step);
            if (even) {
                newLimits[i * 3] += step;
            }
            newLimits[i * 3 + 1] = solution[i] + (halfStepCount * step);
            newLimits[i * 3 + 2] = step;
        }
        return newLimits;
    }

    private List<List<Integer>> prepareWeightsForNR(final List<Integer> weightsList) {
        for (int i = 0; i < subsetsToCompute.size(); i++) {
            weights.put(subsetsToCompute.get(i), weightsList.get(i));
        }

        final List<List<Integer>> result;
        final List<Integer> original = new ArrayList<>(weightsList);
        if (usesWeights) {            
            result = new ArrayList<>(stepCountForOneDimension);

            final int halfCountPos = stepCountForOneDimension / 2;
            final int halfCountNeg;
            if (stepCountForOneDimension % 2 == 0) {
                halfCountNeg = halfCountPos - 1;
            } else {
                halfCountNeg = halfCountPos;
            }

            for (int i = halfCountNeg; i > 0; i--) {
                // generate negative W
                result.add(generateWeights(original, -i));
            }
            result.add(original);
            for (int i = 0; i < halfCountPos; i++) {
                // generate positive W
                result.add(generateWeights(original, i + 1));
            }
        } else {
            result = new ArrayList<>(1);
            result.add(original);
        }
        return result;
    }

    private static List<Integer> generateWeights(final List<Integer> original, final int dif) {
        final List<Integer> result = new ArrayList<>(original.size());
        for (int i = 0; i < original.size(); i++) {
            result.add(original.get(i) + dif);
        }
        return result;
    }

    private void computeAndArrangeData(final List<AbstractSubset> subsets, final List<Integer> weights, final List<double[]> limits) throws ComputationException {
        final List<double[]> localLimits = new ArrayList<>(limits);
        final List<List<Integer>> localWeights = prepareWeightsForNR(weights);
        final List<double[]> localGpuData = new ArrayList<>(localWeights.size());
        for (List<Integer> lWeights : localWeights) {
            computeTask(kernel, new FullTask(fullTask.getImageA(), fullTask.getImageB(), subsets, lWeights, localLimits));
            localGpuData.add(gpuData);
        }
        final int l = gpuData.length;
        gpuData = new double[gpuData.length * localGpuData.size()];
        for (int i = 0; i < localGpuData.size(); i++) {
            System.arraycopy(localGpuData.get(i), 0, gpuData, l * i, l);
        }
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
        final Map<AbstractSubset, Integer> localWeights = new ConcurrentHashMap<>();

        final int coeffCount = DeformationUtils.getDeformationCoeffCount(defDegree);

        final ExecutorService exec = Engine.getInstance().getExecutorService();
        final Set<Future<AbstractSubset>> steps = new HashSet<>(subsetsToCompute.size());

        final Iterator<AbstractSubset> it = subsetsToCompute.iterator();
        while (it.hasNext()) {
            final AbstractSubset as = it.next();
            steps.add(exec.submit(new StepMaker(as, localWeights, localLimits, coeffCount, usesWeights)));
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
            final List<Integer> localWeightsList = new ArrayList<>(subsetsToCompute.size());
            for (AbstractSubset subset : subsetsToCompute) {
                localLimitsList.add(localLimits.get(subset));
                localWeightsList.add(localWeights.get(subset));
            }

            computeAndArrangeData(subsetsToCompute, localWeightsList, localLimitsList);
        }
    }

    private void extractResults(final List<AbstractSubset> subsetsToCompute, final int coeffCount) {
        AbstractSubset as;
        double[] currentLimits;
        CorrelationResult currentResult, newResult;
        int resultIndex;
        double newCorrelationValue;
        long[] counts;

        int baseIndex = 0;

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
            checkStep(as, currentResult, newResult, it, currentLimits, coeffCount);
            baseIndex += counts[coeffCount];
        }
    }

    protected static int[] prepareIndices(final long[] counts) {
        final int[] indices = new int[counts.length - 1];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = (int) (counts[i] / 2);
        }
        return indices;
    }

    protected double[] extractSolutionFromLimits(final double[] limits) {
        final double[] result = new double[limits.length / 3];
        for (int i = 0; i < limits.length / 3; i++) {
            result[i] = (limits[i * 3] + limits[i * 3 + 1]) / 2.0;
        }
        return result;
    }

    private void checkStep(
            final AbstractSubset as,
            final CorrelationResult currentResult, final CorrelationResult newResult,
            final Iterator<AbstractSubset> it,
            final double[] currentLimits, final int coeffCount) {
        if (newResult.getQuality() >= LIMIT_Q_DONE) {
            results.put(as, newResult);
            it.remove();
            addSubsetResult(as, newResult);
            addSubsetTerminationInfo(as, "Good quality");
        } else {
            final double improvement = computeImprovement(currentResult.getDeformation(), newResult.getDeformation());
            if (improvement > LIMIT_MIN_IMPROVEMENT) {
                results.put(as, newResult);
                addSubsetResult(as, newResult);
            } else if (currentLimits[2] > STEP_SECOND) {
                double[] newLimits = generateLimits(currentResult.getDeformation(), coeffCount, STEP_SECOND);
                limits.put(as, newLimits);
            } else {
                it.remove();
                addSubsetTerminationInfo(as, "Low quality increment");
            }
        }
    }

    private static double computeImprovement(final double[] oldResult, final double[] newResult) {
        double sum = 0, impr;
        for (int i = 0; i < oldResult.length; i++) {
            impr = computeImprovement(oldResult[i], newResult[i]);
            sum += impr * impr;
        }
        return Math.sqrt(sum);
    }

    private static double computeImprovement(final double oldResult, final double newResult) {
        return (newResult - oldResult) / newResult;
    }

    // ABSTRACT
    protected abstract RealVector generateNegativeGradient(final AbstractSubset subset);

    protected abstract RealMatrix generateHessianMatrix(final AbstractSubset subset);

    // UTILS
    protected int computeDeformationCount(final DeformationDegree defDegree) {
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(defDegree);
        return (int) Math.pow(stepCountForOneDimension, coeffCount);
    }

    protected int generateIndex(final long[] counts, final int[] indices) {
        final int l;
        if (usesWeights) {
            l = indices.length - 1;
        } else {
            l = indices.length;
        }
        int result = indices[0];
        for (int i = 1; i < l; i++) {
            result *= counts[i];
            result += indices[i];
        }
//        if (usesWeights) {
//            // weight coeff shift
//            final int mult = indices[l];
//            result += mult * (gpuData.length / getSetpCountForOneDimension());
//        }
        return result;
    }

    protected int getCoeffCount(final AbstractSubset subset) {
        double[] deformationLimits = limits.get(subset);
        final DeformationDegree defDegree = DeformationUtils.getDegreeFromLimits(deformationLimits);
        int coeffCount = DeformationUtils.getDeformationCoeffCount(defDegree);
        if (usesWeights) {
            coeffCount++;
        }
        return coeffCount;
    }

    protected double[] prepareArrayForSolver(final AbstractSubset subset) {
        double[] result = limits.get(subset);
        if (usesWeights) {            
            result = Arrays.copyOf(result, result.length + 3);
            result[result.length - 1] = STEP_WEIGHT;

            final int halfCountPos = stepCountForOneDimension / 2;
            final int halfCountNeg;
            if (stepCountForOneDimension % 2 == 0) {
                halfCountNeg = halfCountPos - 1;
            } else {
                halfCountNeg = halfCountPos;
            }
            result[result.length - 3] = weights.get(subset) - halfCountNeg;
            result[result.length - 2] = weights.get(subset) - halfCountPos;
        }
        return result;
    }

    ///// MISC
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
        private final Map<AbstractSubset, Integer> localWeights;
        final int coeffCount;
        final boolean usesWeights;

        public StepMaker(AbstractSubset subset, Map<AbstractSubset, Integer> localWeights, Map<AbstractSubset, double[]> localLimits, int coeffCount, boolean usesWeights) {
            this.subset = subset;
            this.localLimits = localLimits;
            this.localWeights = localWeights;
            this.coeffCount = coeffCount;
            this.usesWeights = usesWeights;
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
                double[] old = results.get(subset).getDeformation();
                if (usesWeights) {
                    old = Arrays.copyOf(old, old.length + 1);
                    old[old.length - 1] = weights.get(subset);
                }
                solutionVec.add(new ArrayRealVector(old));
                // prepare data for next step
                final double[] solution = solutionVec.toArray();
                if (usesWeights) {
                    // separate weight from limits
                    final double[] limit = Arrays.copyOf(solution, solution.length - 1);
                    final int newWeight = (int) Math.round(solution[solution.length - 1]);
                    localLimits.put(subset, generateLimits(limit, coeffCount, limits.get(subset)[DeformationLimit.USTEP]));
                    localWeights.put(subset, newWeight);
                } else {
                    localLimits.put(subset, generateLimits(solution, coeffCount, limits.get(subset)[DeformationLimit.USTEP]));
                    localWeights.put(subset, weights.get(subset));
                }
            } catch (SingularMatrixException ex) {
                addSubsetTerminationInfo(subset, "Singular hessian matrix");
                return subset;
            } catch (Exception ex) {
                if (ex.getStackTrace().length == 0) {
                    Logger.warn("{} stop, exception occured - {}, no stack trace...", subset, ex);
                } else {
                    Logger.warn(ex, "{} stop, exception occured.", subset);
                }
                addSubsetTerminationInfo(subset, "StepMaker exception - " + ex);
                return subset;
            }
            return null;
        }
    }
}
