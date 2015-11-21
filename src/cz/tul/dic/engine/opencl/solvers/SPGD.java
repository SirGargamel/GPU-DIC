/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.solvers;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationLimit;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.task.FullTask;
import cz.tul.dic.debug.IGPUResultsReceiver;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.engine.opencl.kernels.Kernel;
import cz.tul.dic.engine.opencl.kernels.KernelInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.pmw.tinylog.Logger;

public class SPGD extends AbstractTaskSolver implements IGPUResultsReceiver {

    private static final int COUNT_ZERO_ORDER_LIMITS = 6;
    private static final double STEP_INITIAL = .1;
    private static final int LIMIT_ITERATIONS = 100;
    private static final double LIMIT_Q_DONE = 0.99;
    private static final double LIMIT_Q_DIFF = 0.0001;
    private static final double PERTUBATION_AMPLITUDE_ZERO = 0.25;
    private static final double PERTUBATION_AMPLITUDE_FIRST = 0.025;
    private static final double PERTUBATION_AMPLITUDE_SECOND = 0.0025;
    private static final int PERTUBATION_AMPLITUDE_WEIGHT = 10;
    private static final double GAIN_SYSTEM = 50;
    private FullTask fullTask;
    private Map<AbstractSubset, CorrelationResult> results;
    private Map<AbstractSubset, double[]> limits;
    private Map<AbstractSubset, Integer> weights, pertubations;
    private List<AbstractSubset> subsetsToCompute;
    private double[] gpuData;

    @Override
    public List<CorrelationResult> solve(Kernel kernel, FullTask fullTask) throws ComputationException {
        if (fullTask.getSubsets().isEmpty()) {
            return new ArrayList<>(0);
        }
        this.fullTask = fullTask;
        final List<AbstractSubset> subsets = fullTask.getSubsets();
        final int subsetCount = subsets.size();
        final boolean usesWeights = kernel.getKernelInfo().getCorrelation() == KernelInfo.Correlation.WZNSSD;

        results = new LinkedHashMap<>(subsetCount);
        limits = new ConcurrentHashMap<>(subsetCount);
        weights = new ConcurrentHashMap<>(subsetCount);
        pertubations = new LinkedHashMap<>(subsetCount);

        final DeformationDegree defDegree = DeformationUtils.getDegreeFromLimits(fullTask.getDeformationLimits().get(0));
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(defDegree);

        // estimate initial solution by direct search
        performInitialResultEstimation(coeffCount);
        notifyProgress(subsetCount, subsetCount);

        // initial data for solver
        Kernel.registerListener(this);
        subsetsToCompute = Collections.synchronizedList(new ArrayList<>(subsets));
        prepareLimits(defDegree);
        prepareWeights(usesWeights);
        final List<double[]> localLimits = new ArrayList<>(limits.values());
        final List<Integer> localWeights = new ArrayList<>(weights.values());
        computeTask(kernel, new FullTask(fullTask.getImageA(), fullTask.getImageB(), subsetsToCompute, localWeights, localLimits));

        for (int i = 0; i < LIMIT_ITERATIONS; i++) {
            prepareLimits(defDegree);
            prepareWeights(usesWeights);

            makeStep(subsetsToCompute, defDegree, usesWeights);

            extractResults(subsetsToCompute, coeffCount);

            notifyProgress(subsetsToCompute.size(), subsetCount);

            if (subsetsToCompute.isEmpty()) {
                break;
            }
        }

        Kernel.deregisterListener(this);

        return new ArrayList<>(results.values());
    }

    private void performInitialResultEstimation(final int coeffCount) throws ComputationException {
        final List<AbstractSubset> subsets = fullTask.getSubsets();
        final int subsetCount = subsets.size();

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
        final List<Integer> taskWeights = fullTask.getSubsetWeights();
        for (int i = 0; i < subsetCount; i++) {
            weights.put(subsets.get(i), taskWeights.get(i));
        }
        final List<CorrelationResult> localResults = AbstractTaskSolver.initSolver(Solver.BRUTE_FORCE).solve(
                new FullTask(fullTask.getImageA(), fullTask.getImageB(), fullTask.getSubsets(), new ArrayList<>(weights.values()), zeroOrderLimits), subsetSize);
        CorrelationResult paddedResult, currentResult;
        for (int i = 0; i < subsetCount; i++) {
            // set the length of the result to match correlation degree
            currentResult = localResults.get(i);
            paddedResult = new CorrelationResult(currentResult.getQuality(), Arrays.copyOf(currentResult.getDeformation(), coeffCount));
            results.put(subsets.get(i), paddedResult);
            addSubsetResult(fullTask.getSubsets().get(i), localResults.get(i));
        }
    }

    private void prepareLimits(final DeformationDegree degree) {
        double[] newLimits;
        for (AbstractSubset subset : subsetsToCompute) {
            newLimits = generateLimit(results.get(subset).getDeformation(), generatePertubation(degree));
            limits.put(subset, newLimits);
        }
    }
    
    private double[] generateLimit(final double[] solution, final double[] pertubation) {
        final int coeffCount = solution.length;
        final double[] newLimits = new double[coeffCount * 3];
        for (int i = 0; i < coeffCount; i++) {
            newLimits[i * 3] = solution[i] - pertubation[i];
            newLimits[i * 3 + 1] = solution[i] + pertubation[i];
            newLimits[i * 3 + 2] = pertubation[i];
        }
        return newLimits;
    }

    private double[] generatePertubation(final DeformationDegree degree) {
        int coeffCount = DeformationUtils.getDeformationCoeffCount(degree);
        final Random rnd = new Random();
        double[] result = new double[coeffCount];
        switch (degree) {
            case SECOND:
                result[11] = rnd.nextDouble() * PERTUBATION_AMPLITUDE_SECOND;
                result[10] = rnd.nextDouble() * PERTUBATION_AMPLITUDE_SECOND;
                result[9] = rnd.nextDouble() * PERTUBATION_AMPLITUDE_SECOND;
                result[8] = rnd.nextDouble() * PERTUBATION_AMPLITUDE_SECOND;
                result[7] = rnd.nextDouble() * PERTUBATION_AMPLITUDE_SECOND;
                result[6] = rnd.nextDouble() * PERTUBATION_AMPLITUDE_SECOND;
            case FIRST:
                result[5] = rnd.nextDouble() * PERTUBATION_AMPLITUDE_FIRST;
                result[4] = rnd.nextDouble() * PERTUBATION_AMPLITUDE_FIRST;
                result[3] = rnd.nextDouble() * PERTUBATION_AMPLITUDE_FIRST;
                result[2] = rnd.nextDouble() * PERTUBATION_AMPLITUDE_FIRST;
            case ZERO:
                result[1] = rnd.nextDouble() * PERTUBATION_AMPLITUDE_ZERO;
                result[0] = rnd.nextDouble() * PERTUBATION_AMPLITUDE_ZERO;
        }
        return result;
    }

    private void prepareWeights(final boolean usesWeights) {
        if (usesWeights) {
            final Random rnd = new Random();
            int weight, pertubation;
            for (Entry<AbstractSubset, Integer> e : weights.entrySet()) {
                pertubation = rnd.nextInt(PERTUBATION_AMPLITUDE_WEIGHT) - (PERTUBATION_AMPLITUDE_WEIGHT / 2);
                pertubations.put(e.getKey(), pertubation);
                weight = e.getValue() + pertubation;
                weights.put(e.getKey(), weight);
            }
        }
    }    

    /**
     * Make one step using SPGD solver. TODO...
     *
     * @param subsetsToCompute
     * @param defDegree
     * @throws ComputationException
     */
    private void makeStep(final List<AbstractSubset> subsetsToCompute, final DeformationDegree defDegree, final boolean usesWeights) throws ComputationException {
        final Map<AbstractSubset, double[]> localLimits = new ConcurrentHashMap<>();
        final Map<AbstractSubset, Integer> localWeights = new ConcurrentHashMap<>();
        
        final ExecutorService exec = Engine.getInstance().getExecutorService();
        final Set<Future<AbstractSubset>> steps = new HashSet<>(subsetsToCompute.size());

        final Iterator<AbstractSubset> it = subsetsToCompute.iterator();
        while (it.hasNext()) {
            final AbstractSubset as = it.next();
            steps.add(exec.submit(new StepMaker(as, localWeights, localLimits, defDegree, usesWeights)));
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
            final List<double[]> limitsToCompute = new ArrayList<>(subsetsToCompute.size());
            final List<Integer> weightToCompute = new ArrayList<>(subsetsToCompute.size());
            for (AbstractSubset subset : subsetsToCompute) {
                limitsToCompute.add(limits.get(subset));
                weightToCompute.add(weights.get(subset));
            }
            computeTask(kernel, new FullTask(fullTask.getImageA(), fullTask.getImageB(), subsetsToCompute, weightToCompute, limitsToCompute));
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
            currentLimits = limits.get(as);
            counts = DeformationUtils.generateDeformationCounts(currentLimits);
            // check quality of new result
            resultIndex = baseIndex + generateIndex(counts, prepareIndices(counts));
            newCorrelationValue = gpuData[resultIndex];
            newResult = new CorrelationResult(newCorrelationValue, extractSolutionFromLimits(currentLimits));
            checkStep(as, newResult, it);
            currentResult = results.get(as);
            if (newResult.getQuality() > currentResult.getQuality()) {

            }

            baseIndex += counts[coeffCount];
        }
    }

    private static int generateIndex(final long[] counts, final int[] indices) {
        int result = indices[0];
        for (int i = 1; i < indices.length; i++) {
            result *= counts[i];
            result += indices[i];
        }
        return result;
    }

    private static int[] prepareIndices(final long[] counts) {
        final int[] indices = new int[counts.length - 1];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = (int) (counts[i] / 2);
        }
        return indices;
    }

    private static double[] extractSolutionFromLimits(final double[] limits) {
        final double[] result = new double[limits.length / 3];
        for (int i = 0; i < limits.length / 3; i++) {
            result[i] = (limits[i * 3] + limits[i * 3 + 1]) / 2.0;
        }
        return result;
    }

    private void checkStep(
            final AbstractSubset as,
            final CorrelationResult newResult,
            final Iterator<AbstractSubset> it) {
        results.put(as, newResult);
        addSubsetResult(as, newResult);
        if (newResult.getQuality() >= LIMIT_Q_DONE) {
            it.remove();
            addSubsetTerminationInfo(as, "Good quality");
        }
    }

    @Override
    protected boolean needsBestResult() {
        return true;
    }

    private void notifyProgress(final int subsetToCompute, final int subsetCount) {
        // TODO iteration based notify

        if (subsetCount > 0) {
            setChanged();
            notifyObservers(0.5 + 0.5 * ((subsetCount - subsetToCompute) / subsetCount));
        }
    }

    @Override
    public void dumpGpuResults(double[] resultData, List<AbstractSubset> subsets, List<double[]> deformationLimits) {
        this.gpuData = resultData;
    }

    private class StepMaker implements Callable<AbstractSubset> {

        private final AbstractSubset subset;
        private final DeformationDegree degree;
        private final Map<AbstractSubset, double[]> localLimits;
        private final Map<AbstractSubset, Integer> localWeights;
        private final boolean usesWeights;

        public StepMaker(AbstractSubset subset, Map<AbstractSubset, Integer> localWeights, Map<AbstractSubset, double[]> localLimits, final DeformationDegree degree, boolean usesWeights) {
            this.subset = subset;
            this.localLimits = localLimits;
            this.localWeights = localWeights;            
            this.degree = degree;
            this.usesWeights = usesWeights;
        }

        @Override
        public AbstractSubset call() throws Exception {
            try {
                final int deformationCount = computeDeformationCount(degree);
                final int resultsBase = (subsetsToCompute.indexOf(subset) * deformationCount);
                // prepare data for computation
                final double correlationPlus = gpuData[resultsBase];
                final double correlationMinus = gpuData[resultsBase + 1];
                final double dJ = correlationMinus - correlationPlus;

                if (Math.abs(dJ) < LIMIT_Q_DIFF) {
                    addSubsetTerminationInfo(subset, "Low correlation diff");
                    return subset;
                }

                final double[] oldResult = results.get(subset).getDeformation();
                final double[] oldLimits = limits.get(subset);
                final double[] solution = new double[oldResult.length];
                for (int i = 0; i < solution.length; i++) {
                    // new = old + Gain * Pertubation * dJ
                    solution[i] = oldResult[i] + (GAIN_SYSTEM * oldLimits[i * 3 + 2] * dJ);
                }

                // prepare data for next step
                localLimits.put(subset, generateLimit(solution, generatePertubation(degree)));

                final int oldWeight = weights.get(subset);
                if (usesWeights) {
                    final int newWeight = oldWeight + (int) Math.round(GAIN_SYSTEM * pertubations.get(subset) * dJ);
                    localWeights.put(subset, newWeight);
                } else {
                    localWeights.put(subset, oldWeight);
                }                
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

        private int computeDeformationCount(final DeformationDegree defDegree) {
            final int coeffCount = DeformationUtils.getDeformationCoeffCount(defDegree);
            return (int) Math.pow(2, coeffCount);
        }
    }

}
