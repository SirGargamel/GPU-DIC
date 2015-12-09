/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.solvers;

import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.ComputationException;
import cz.tul.dic.data.deformation.DeformationOrder;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.FullTask;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BruteForce extends AbstractTaskSolver {

    private static final boolean USE_LIMITS = false;

    @Override
    public List<CorrelationResult> solve(
            final FullTask fullTask,
            final boolean usesWeights) throws ComputationException {
        final List<CorrelationResult> results;
        final DeformationOrder order = DeformationUtils.getOrderFromLimits(fullTask.getDeformationLimits().get(0));
        if (!USE_LIMITS) {
            final List<double[]> deformations = generateDeformationsFromLimits(fullTask.getDeformationLimits(), order);
            results = computeTask(kernel, new ComputationTask(
                    fullTask.getImageA(), fullTask.getImageB(),
                    fullTask.getSubsets(), fullTask.getSubsetWeights(),
                    deformations, order, USE_LIMITS));
        } else {
            results = computeTask(kernel, new ComputationTask(
                    fullTask.getImageA(), fullTask.getImageB(),
                    fullTask.getSubsets(), fullTask.getSubsetWeights(),
                    fullTask.getDeformationLimits(), order, USE_LIMITS));
        }

        final List<AbstractSubset> subsets = fullTask.getSubsets();
        for (int i = 0; i < subsets.size(); i++) {
            addSubsetResultInfo(subsets.get(i), results.get(i));
        }

        return results;
    }

    private List<double[]> generateDeformationsFromLimits(final List<double[]> deformationLimits, final DeformationOrder order) {
        List<long[]> counts = DeformationUtils.generateDeformationCounts(deformationLimits);
        List<double[]> result = new ArrayList<>(deformationLimits.size());
        final int coeffCount = DeformationUtils.getDeformationCoeffCount(order);

        double[] limits, deformation;
        long[] count;
        long counter;
        List<double[]> deformations = new LinkedList<>();
        for (int i = 0; i < deformationLimits.size(); i++) {
            deformations.clear();

            limits = deformationLimits.get(i);
            count = counts.get(i);

            // generate deformations            
            for (int j = 0; j < count[count.length - 1]; j++) {
                deformation = new double[coeffCount];
                counter = j;

                for (int k = 0; k < coeffCount; k++) {
                    deformation[k] = counter % count[k];
                    counter = counter / count[k];
                }
                for (int k = 0; k < coeffCount; k++) {
                    deformation[k] = limits[k * 3] + deformation[k] * limits[k * 3 + 2];
                }
                deformations.add(deformation);
            }

            result.add(condenseDeformations(deformations, coeffCount));
        }

        return result;
    }

    private double[] condenseDeformations(final List<double[]> deformations, final int coeffCount) {
        final double[] result = new double[deformations.size() * coeffCount];
        double[] deformation;
        for (int i = 0; i < deformations.size(); i++) {
            deformation = deformations.get(i);
            System.arraycopy(deformation, 0, result, i * coeffCount, coeffCount);
        }
        return result;
    }

    @Override
    protected boolean needsBestResult() {
        return true;
    }

}
