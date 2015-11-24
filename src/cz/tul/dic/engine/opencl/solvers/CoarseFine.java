/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.solvers;

import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationLimit;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.task.FullTask;
import java.util.ArrayList;
import java.util.List;
import org.pmw.tinylog.Logger;

public class CoarseFine extends AbstractTaskSolver {

    private static final int COUNT_ZERO_ORDER_LIMITS = 6;
    private static final double STEP_INITIAL = 1;
    private static final double STEP_MINIMAL = 0.01;

    @Override
    public List<CorrelationResult> solve(
            final FullTask fullTask,
            final boolean usesWeights) throws ComputationException {
        if (fullTask.getSubsets().isEmpty()) {
            return new ArrayList<>(0);
        }

        final List<AbstractSubset> subsets = fullTask.getSubsets();
        final int subsetCount = subsets.size();
        List<double[]> zeroOrderLimits = new ArrayList<>(subsetCount);
        List<CorrelationResult> results;
        double[] temp;
        double step = STEP_INITIAL;

        final int roundCount = coumputeRoundCount(fullTask);                
        final List<Integer> weights = fullTask.getSubsetWeights();        

        // initial pixel step
        int round = 0;
        for (double[] dA : fullTask.getDeformationLimits()) {
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
        results = computeTask(
                kernel,
                new FullTask(fullTask.getImageA(), fullTask.getImageB(), fullTask.getSubsets(), weights, zeroOrderLimits));
        for (int i = 0; i < subsetCount; i++) {
            addSubsetResult(subsets.get(i), results.get(i));
        }
        signalizeRoundComplete(++round, roundCount);

        //sub-pixel stepping
        final double minimalStep = findMinimalStep(fullTask);
        double[] coarseResult;
        do {
            step /= 10.0;
            if (step < minimalStep) {
                if (Double.compare(step * 10, minimalStep) == 0) {
                    break;
                } else {
                    step = minimalStep;
                }
            }

            zeroOrderLimits = new ArrayList<>(subsetCount);

            for (int i = 0; i < subsetCount; i++) {
                coarseResult = results.get(i).getDeformation();
                temp = new double[COUNT_ZERO_ORDER_LIMITS];

                temp[DeformationLimit.UMIN] = coarseResult[Coordinates.X] - (10 * step);
                temp[DeformationLimit.UMAX] = coarseResult[Coordinates.X] + (10 * step);
                temp[DeformationLimit.USTEP] = step;
                temp[DeformationLimit.VMIN] = coarseResult[Coordinates.Y] - (10 * step);
                temp[DeformationLimit.VMAX] = coarseResult[Coordinates.Y] + (10 * step);
                temp[DeformationLimit.VSTEP] = step;

                zeroOrderLimits.add(temp);
            }
            results = computeTask(
                    kernel,
                    new FullTask(fullTask.getImageA(), fullTask.getImageB(), fullTask.getSubsets(), weights, zeroOrderLimits));
            
            for (int i = 0; i < subsetCount; i++) {
                addSubsetResult(subsets.get(i), results.get(i));
            }            
            signalizeRoundComplete(++round, roundCount);
        } while (step > STEP_MINIMAL);

        final DeformationDegree defDegree = DeformationUtils.getDegreeFromLimits(fullTask.getDeformationLimits().get(0));
        if (defDegree != DeformationDegree.ZERO) {
            Logger.debug("CoarseFine solver does support only zero order deformations.");
        }        

        return results;
    }

    private static int coumputeRoundCount(final FullTask fullTask) throws ComputationException {
        final double minimalStep = findMinimalStep(fullTask);

        int roundCount = 1;
        double tempStep = STEP_INITIAL;
        do {
            tempStep /= 10.0;
            if (tempStep < minimalStep && Double.compare(tempStep * 10, minimalStep) == 0) {
                // break only when tempSeto is at least 10 times smaller
                // handles limits such as 0.25 etc.
                break;
            }
            roundCount++;
        } while (tempStep > STEP_MINIMAL);

        final DeformationDegree defDegree = DeformationUtils.getDegreeFromLimits(fullTask.getDeformationLimits().get(0));
        if (defDegree != DeformationDegree.ZERO) {
            roundCount++;
        }

        return roundCount;
    }

    private static double findMinimalStep(final FullTask fullTask) {
        double minimalStep = 1;
        for (double[] dA : fullTask.getDeformationLimits()) {
            minimalStep = Math.min(minimalStep, Math.min(dA[DeformationLimit.USTEP], dA[DeformationLimit.VSTEP]));
        }
        return minimalStep;
    }

    private void signalizeRoundComplete(final int round, final int roundCount) {
        setChanged();
        notifyObservers(1 / (double) roundCount * round);
    }

    @Override
    protected boolean needsBestResult() {
        return true;
    }

}
