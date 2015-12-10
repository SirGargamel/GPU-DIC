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
import cz.tul.dic.data.task.TaskDefaultValues;
import java.util.List;

public class BruteForce extends AbstractTaskSolver {

    @Override
    public List<CorrelationResult> solve(
            final FullTask fullTask,
            final boolean usesWeights) throws ComputationException {
        final DeformationOrder order = DeformationUtils.getOrderFromLimits(fullTask.getDeformationLimits().get(0));
        final List<CorrelationResult> localResults = computeTask(kernel, new ComputationTask(
                fullTask.getImageA(), fullTask.getImageB(),
                fullTask.getSubsets(), fullTask.getSubsetWeights(),
                fullTask.getDeformationLimits(), order, true));

        final List<AbstractSubset> subsets = fullTask.getSubsets();
        for (int i = 0; i < subsets.size(); i++) {
            addSubsetResultInfo(subsets.get(i), localResults.get(i));
        }

        return localResults;
    }

    @Override
    protected boolean needsBestResult() {
        return true;
    }

    @Override
    public long getDeformationCount() {
        final List<double[]> def = fullTask.getDeformationLimits();
        double[] limits;
        if (!def.isEmpty()) {
            limits = def.get(0);                        
        } else {
            limits = TaskDefaultValues.DEFAULT_DEFORMATION_LIMITS_ZERO;
        }
        final long[] counts = DeformationUtils.generateDeformationCounts(limits);
        return counts[counts.length - 1];
    }

}
