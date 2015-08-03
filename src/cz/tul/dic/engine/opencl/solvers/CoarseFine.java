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
import cz.tul.dic.data.task.FullTask;
import cz.tul.dic.engine.opencl.kernels.Kernel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.pmw.tinylog.Logger;

public class CoarseFine extends AbstractTaskSolver {

    private static final int COUNT_ZERO_ORDER_LIMITS = 6;
    private static final double STEP_INITIAL = 1;
    private static final double STEP_MINIMAL = 0.01;

    @Override
    public List<CorrelationResult> solve(
            final Kernel kernel,
            final FullTask fullTask) throws ComputationException {
        if (fullTask.getSubsets().isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        final int subsetCount = fullTask.getSubsets().size();
        final StringBuilder sb = new StringBuilder();
        List<double[]> zeroOrderLimits = new ArrayList<>(subsetCount);
        List<CorrelationResult> results;
        double[] temp;
        double step = STEP_INITIAL;

        double minStep = 1;
        for (double[] dA : fullTask.getDeformationLimits()) {
            minStep = Math.min(minStep, Math.min(dA[DeformationLimit.USTEP], dA[DeformationLimit.VSTEP]));
        }
        int roundCount = 1;
        double tempStep = step;
        do {
            tempStep /= 10.0;
            if (tempStep < minStep) {
                if (Double.compare(tempStep * 10, minStep) == 0) {
                    break;
                } else {
                    tempStep = minStep;
                }
            }
            roundCount++;
        } while (tempStep > STEP_MINIMAL);
        
        final DeformationDegree defDegree = DeformationUtils.getDegreeFromLimits(fullTask.getDeformationLimits().get(0));
        if (defDegree != DeformationDegree.ZERO) {
            roundCount++;
        }

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
                new FullTask(fullTask.getImageA(), fullTask.getImageB(), fullTask.getSubsets(), zeroOrderLimits));
        sb.append("Initial results, step [").append(step).append("]:");
        for (int i = 0; i < subsetCount; i++) {
            sb.append(i)
                    .append(" - ")
                    .append(results.get(i))
                    .append("; ");
        }
        signalizeRoundComplete(++round, roundCount);

        //sub-pixel stepping
        double[] coarseResult, newLimits;
        int l;
        do {
            step /= 10.0;
            if (step < minStep) {
                if (Double.compare(tempStep * 10, minStep) == 0) {
                    break;
                } else {
                    step = minStep;
                }
            }

            zeroOrderLimits.clear();
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
                    new FullTask(fullTask.getImageA(), fullTask.getImageB(), fullTask.getSubsets(), zeroOrderLimits));

            sb.append("Finer results, step [").append(step).append("]:");
            for (int i = 0; i < subsetCount; i++) {
                sb.append(i)
                        .append(" - ")
                        .append(results.get(i))
                        .append("; ");
            }
            signalizeRoundComplete(++round, roundCount);
        } while (step > STEP_MINIMAL);

        //higher order search
        if (defDegree != DeformationDegree.ZERO) {
            final List<double[]> higherOrderLimits = new ArrayList<>(subsetCount);

            for (int i = 0; i < subsetCount; i++) {
                coarseResult = results.get(i).getDeformation();
                temp = fullTask.getDeformationLimits().get(i);
                l = temp.length;

                newLimits = new double[l];
                System.arraycopy(temp, 0, newLimits, 0, l);

                newLimits[DeformationLimit.UMIN] = coarseResult[Coordinates.X];
                newLimits[DeformationLimit.UMAX] = coarseResult[Coordinates.X];
                newLimits[DeformationLimit.USTEP] = 0;
                newLimits[DeformationLimit.VMIN] = coarseResult[Coordinates.Y];
                newLimits[DeformationLimit.VMAX] = coarseResult[Coordinates.Y];
                newLimits[DeformationLimit.VSTEP] = 0;

                higherOrderLimits.add(newLimits);
            }
            results = computeTask(
                    kernel,
                    new FullTask(fullTask.getImageA(), fullTask.getImageB(), fullTask.getSubsets(), higherOrderLimits));

            sb.append("Higher order results: ");
            for (int i = 0; i < subsetCount; i++) {
                sb.append(i)
                        .append(" - ")
                        .append(results.get(i))
                        .append("; ");
            }
            signalizeRoundComplete(++round, roundCount);
        }
        Logger.trace(sb);

        return results;
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
