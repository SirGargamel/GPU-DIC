/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.solvers;

import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.ComputationException;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.task.FullTask;
import java.util.List;

public class BruteForce extends AbstractTaskSolver {

    @Override
    public List<CorrelationResult> solve(
            final FullTask fullTask,
            final boolean usesWeights) throws ComputationException {
        final List<CorrelationResult> results = computeTask(kernel, fullTask);

        final List<AbstractSubset> subsets = fullTask.getSubsets();
        for (int i = 0; i < subsets.size(); i++) {
            addSubsetResult(subsets.get(i), results.get(i));
        }

        return results;
    }

    @Override
    protected boolean needsBestResult() {
        return true;
    }

}
