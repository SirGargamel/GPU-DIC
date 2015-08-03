/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.solvers;

import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.FullTask;
import cz.tul.dic.engine.opencl.kernels.Kernel;
import java.util.List;

public class BruteForce extends AbstractTaskSolver {

    @Override
    public List<CorrelationResult> solve(
            final Kernel kernel, 
            final FullTask fullTask) throws ComputationException {
        return computeTask(kernel, fullTask);
    }
    
    @Override
    protected boolean needsBestResult() {
        return true;
    }

}
