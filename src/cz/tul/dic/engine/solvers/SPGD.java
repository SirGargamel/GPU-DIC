/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.solvers;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.deformation.DeformationOrder;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.debug.IGPUResultsReceiver;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.engine.platform.Platform;
import cz.tul.pj.journal.Journal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class SPGD extends AbstractTaskSolver implements IGPUResultsReceiver {

    private static final int LIMIT_ITERATIONS = 200;
    private static final double LIMIT_Q_DONE = 0.999;
    private static final double LIMIT_Q_DIFF = 1 - LIMIT_Q_DONE;
    private static final double PERTUBATION_AMPLITUDE_ZERO = 0.1;
    private static final double PERTUBATION_AMPLITUDE_FIRST = 0.01;
    private static final double PERTUBATION_AMPLITUDE_SECOND = 0.01;
    private static final int PERTUBATION_AMPLITUDE_WEIGHT = 5;
    private static final double GAIN_SYSTEM = 100;
    private Map<AbstractSubset, double[]> deformationsPertubations;
    private Map<AbstractSubset, Integer> weightsPertubations;
    private DeformationOrder defOrder;

    public SPGD(Platform platform) {
        super(platform);
    }

    @Override
    public List<CorrelationResult> solve() throws ComputationException {
        if (fullTask.getSubsets().isEmpty()) {
            return new ArrayList<>(0);
        }
        final List<AbstractSubset> subsets = fullTask.getSubsets();
        final int subsetCount = subsets.size();

        deformationsPertubations = new LinkedHashMap<>(subsetCount);
        weightsPertubations = new LinkedHashMap<>(subsetCount);

        defOrder = DeformationUtils.getOrderFromLimits(fullTask.getDeformationLimits().get(0));

        // estimate initial solution by direct search
        prepareInitialResults();
        notifyProgress(subsetCount, subsetCount);

        // initial data for solver
        registerGPUDataListener(this);
        prepareDeformations();
        prepareWeightsPertubations(usesWeights);
        computeStep();

        for (int i = 0; i < LIMIT_ITERATIONS; i++) {
            makeStep();

            notifyProgress(subsetsToCompute.size(), subsetCount);

            if (subsetsToCompute.isEmpty()) {
                break;
            }
        }

        deregisterGPUDataListener(this);

        return new ArrayList<>(results.values());
    }

    private void computeStep() throws ComputationException {
        final List<double[]> deformationsToCompute = new ArrayList<>(subsetsToCompute.size());
        final List<Integer> weightsToCompute = new ArrayList<>(subsetsToCompute.size());
        if (!usesWeights) {
            for (AbstractSubset subset : subsetsToCompute) {
                deformationsToCompute.add(generateDeformation(deformations.get(subset), deformationsPertubations.get(subset)));
                weightsToCompute.add(weights.get(subset));
            }
            computeTask(kernel, new ComputationTask(fullTask.getImageA(), fullTask.getImageB(), subsetsToCompute, weightsToCompute, deformationsToCompute, defOrder, false));
        } else {
            final List<double[]> gpuDataList = new ArrayList<>(3);

            // result after step
            for (AbstractSubset subset : subsetsToCompute) {
                deformationsToCompute.add(deformations.get(subset));
                weightsToCompute.add(weights.get(subset));
            }
            computeTask(kernel, new ComputationTask(fullTask.getImageA(), fullTask.getImageB(), subsetsToCompute, weightsToCompute, deformationsToCompute, defOrder, false));
            gpuDataList.add(gpuData);

            // result - pertubation
            deformationsToCompute.clear();
            weightsToCompute.clear();
            for (AbstractSubset subset : subsetsToCompute) {
                deformationsToCompute.add(subtract(deformations.get(subset), deformationsPertubations.get(subset)));
                weightsToCompute.add(weights.get(subset) - weightsPertubations.get(subset));
            }
            computeTask(kernel, new ComputationTask(fullTask.getImageA(), fullTask.getImageB(), subsetsToCompute, weightsToCompute, deformationsToCompute, defOrder, false));
            gpuDataList.add(gpuData);

            // result + pertubation
            deformationsToCompute.clear();
            weightsToCompute.clear();
            for (AbstractSubset subset : subsetsToCompute) {
                deformationsToCompute.add(add(deformations.get(subset), deformationsPertubations.get(subset)));
                weightsToCompute.add(weights.get(subset) + weightsPertubations.get(subset));
            }
            computeTask(kernel, new ComputationTask(fullTask.getImageA(), fullTask.getImageB(), subsetsToCompute, weightsToCompute, deformationsToCompute, defOrder, false));
            gpuDataList.add(gpuData);

            // join data together
            final double[] orig = gpuDataList.get(0);
            final double[] minus = gpuDataList.get(1);
            final double[] plus = gpuDataList.get(2);
            final int l = orig.length;

            gpuData = new double[l * 3];
            int base;
            for (int i = 0; i < l; i++) {
                base = i * 3;
                gpuData[base] = orig[i];
                gpuData[base + 1] = minus[i];
                gpuData[base + 2] = plus[i];
            }
        }
    }

    private double[] subtract(final double[] dA1, final double[] dA2) {
        final double[] result = Arrays.copyOf(dA1, dA1.length);
        for (int i = 0; i < dA1.length; i++) {
            result[i] -= dA2[i];
        }
        return result;
    }

    private double[] add(final double[] dA1, final double[] dA2) {
        final double[] result = Arrays.copyOf(dA1, dA1.length);
        for (int i = 0; i < dA1.length; i++) {
            result[i] += dA2[i];
        }
        return result;
    }

    private void prepareInitialResults() throws ComputationException {
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(defOrder);
        final List<AbstractSubset> subsets = fullTask.getSubsets();
        final int subsetCount = subsets.size();
        AbstractSubset subset;
        for (int i = 0; i < subsetCount; i++) {
            subset = subsets.get(i);
            results.put(subset, new CorrelationResult(-Double.MAX_VALUE, new double[coeffCount]));
            addSubsetResultInfo(subset, results.get(subset));
        }
    }

    private void prepareDeformations() {
        for (AbstractSubset subset : subsetsToCompute) {
            deformationsPertubations.put(subset, generateDeformationPertubation(defOrder));
            deformations.put(subset, results.get(subset).getDeformation());
        }
    }

    private double[] generateDeformation(final double[] solution, final double[] pertubation) {
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(defOrder);
        final double[] result = new double[coeffCount * 3];
        for (int i = 0; i < coeffCount; i++) {
            result[i] = solution[i];
            result[coeffCount + i] = solution[i] - pertubation[i];
            result[2 * coeffCount + i] = solution[i] + pertubation[i];
        }
        return result;
    }

    private double[] generateDeformationPertubation(final DeformationOrder degree) {
        int coeffCount = DeformationUtils.getDeformationCoeffCount(degree);
        final Random rnd = new Random();
        double[] result = new double[coeffCount];
        switch (degree) {
            case SECOND:
                result[11] = (rnd.nextDouble() * 2 * PERTUBATION_AMPLITUDE_SECOND) - (PERTUBATION_AMPLITUDE_SECOND);
                result[10] = (rnd.nextDouble() * 2 * PERTUBATION_AMPLITUDE_SECOND) - (PERTUBATION_AMPLITUDE_SECOND);
                result[9] = (rnd.nextDouble() * 2 * PERTUBATION_AMPLITUDE_SECOND) - (PERTUBATION_AMPLITUDE_SECOND);
                result[8] = (rnd.nextDouble() * 2 * PERTUBATION_AMPLITUDE_SECOND) - (PERTUBATION_AMPLITUDE_SECOND);
                result[7] = (rnd.nextDouble() * 2 * PERTUBATION_AMPLITUDE_SECOND) - (PERTUBATION_AMPLITUDE_SECOND);
                result[6] = (rnd.nextDouble() * 2 * PERTUBATION_AMPLITUDE_SECOND) - (PERTUBATION_AMPLITUDE_SECOND);
            case FIRST:
                result[5] = (rnd.nextDouble() * 2 * PERTUBATION_AMPLITUDE_FIRST) - (PERTUBATION_AMPLITUDE_FIRST);
                result[4] = (rnd.nextDouble() * 2 * PERTUBATION_AMPLITUDE_FIRST) - (PERTUBATION_AMPLITUDE_FIRST);
                result[3] = (rnd.nextDouble() * 2 * PERTUBATION_AMPLITUDE_FIRST) - (PERTUBATION_AMPLITUDE_FIRST);
                result[2] = (rnd.nextDouble() * 2 * PERTUBATION_AMPLITUDE_FIRST) - (PERTUBATION_AMPLITUDE_FIRST);
            case ZERO:
                result[1] = (rnd.nextDouble() * 2 * PERTUBATION_AMPLITUDE_ZERO) - (PERTUBATION_AMPLITUDE_ZERO);
                result[0] = (rnd.nextDouble() * 2 * PERTUBATION_AMPLITUDE_ZERO) - (PERTUBATION_AMPLITUDE_ZERO);
        }
        return result;
    }

    private void prepareWeightsPertubations(final boolean usesWeights) {
        for (AbstractSubset subset : subsetsToCompute) {
            weightsPertubations.put(subset, generateWeightPertubation(usesWeights));
        }
    }

    private int generateWeightPertubation(final boolean usesWeights) {
        if (usesWeights) {
            final Random rnd = new Random();
            return rnd.nextInt(2 * PERTUBATION_AMPLITUDE_WEIGHT) - (PERTUBATION_AMPLITUDE_WEIGHT);
        } else {
            return 0;
        }
    }

    /**
     * Make one step using SPGD solver. TODO...
     *
     * @param subsetsToCompute
     * @param defDegree
     * @throws ComputationException
     */
    private void makeStep() throws ComputationException {
        final ExecutorService exec = Engine.getInstance().getExecutorService();
        final Set<Future<AbstractSubset>> steps = new HashSet<>(subsetsToCompute.size());

        final Iterator<AbstractSubset> it = subsetsToCompute.iterator();
        while (it.hasNext()) {
            final AbstractSubset as = it.next();
            steps.add(exec.submit(new StepMaker(as, defOrder, usesWeights)));
        }

        for (Future<AbstractSubset> fas : steps) {
            try {
                subsetsToCompute.remove(fas.get());
            } catch (InterruptedException | ExecutionException ex) {
                Journal.getInstance().addDataEntry(ex, "Solver error", "Error retrieving result after computing new step.");
            }
        }

        if (!subsetsToCompute.isEmpty()) {
            computeStep();
        }
    }

    @Override
    protected boolean needsBestResult() {
        return false;
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

    @Override
    public long getDeformationCount() {
        return 3;
    }

    private class StepMaker implements Callable<AbstractSubset> {

        private final AbstractSubset subset;
        private final DeformationOrder order;
        private final boolean usesWeights;

        public StepMaker(AbstractSubset subset, final DeformationOrder order, boolean usesWeights) {
            this.subset = subset;
            this.order = order;
            this.usesWeights = usesWeights;
        }

        @Override
        public AbstractSubset call() throws Exception {
            try {
                final int subsetIndex = subsetsToCompute.indexOf(subset);
                final int resultsBase = (subsetIndex * 3);
                // check result quality
                final double correlationValue = gpuData[resultsBase];
                final double[] currentDeformation = extractDeformation(subset, order);
                final CorrelationResult newResult = new CorrelationResult(correlationValue, currentDeformation);
                results.put(subset, newResult);
                addSubsetResultInfo(subset, newResult);

                if (newResult.getQuality() >= LIMIT_Q_DONE) {
                    addSubsetTerminationInfo(subset, "Good quality");
                    return subset;
                }

                final double correlationMinus = gpuData[resultsBase + 1];
                final double correlationPlus = gpuData[resultsBase + 2];
                final double dJ = correlationPlus - correlationMinus;

                if (Math.abs(dJ) < LIMIT_Q_DIFF) {
                    addSubsetTerminationInfo(subset, "Low dJ");
                    return subset;
                }

                // prepare data for next step
                final double[] currentDeformationPertubation = deformationsPertubations.get(subset);
                final double[] nextDeformation = new double[currentDeformation.length];
                for (int i = 0; i < nextDeformation.length; i++) {
                    // new = old + Gain * Pertubation * dJ
                    nextDeformation[i] = currentDeformation[i] + (GAIN_SYSTEM * currentDeformationPertubation[i] * dJ);
                }
                final double[] nextDeformationPertubation = generateDeformationPertubation(order);
                deformationsPertubations.put(subset, nextDeformationPertubation);
                deformations.put(subset, nextDeformation);

                final int currentWeight = weights.get(subset);
                final int currentWeightPertuation = weightsPertubations.get(subset);
                final int nextWeight = (int) (currentWeight + Math.round(GAIN_SYSTEM * currentWeightPertuation + dJ));
                final int nextWeightPertubation = generateWeightPertubation(usesWeights);
                weightsPertubations.put(subset, nextWeightPertubation);
                weights.put(subset, nextWeight);
            } catch (Exception ex) {
                if (ex.getStackTrace().length == 0) {
                    Journal.getInstance().addEntry("Subset computation stopped", "{0} stop, exception occured - {1}, no stack trace...", subset, ex);
                } else {
                    Journal.getInstance().addDataEntry(ex, "Subset computation stopped", "{0} stop, exception occured.", subset);
                }
                addSubsetTerminationInfo(subset, "StepMaker exception - " + ex);
                return subset;
            }
            return null;
        }

        private double[] extractDeformation(final AbstractSubset subset, final DeformationOrder order) {
            final int coeffCount = DeformationUtils.getDeformationCoeffCount(order);
            final double[] result = new double[coeffCount];
            System.arraycopy(deformations.get(subset), 0, result, 0, coeffCount);
            return result;
        }
    }

}
