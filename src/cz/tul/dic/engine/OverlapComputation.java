/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.result.Result;
import cz.tul.dic.data.task.Hint;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.engine.displacement.DisplacementCalculator;
import cz.tul.dic.engine.strain.StrainEstimator;
import java.util.concurrent.Callable;

/**
 *
 * @author Lenam s.r.o.
 */
public class OverlapComputation implements Callable<Void> {

    private final TaskContainer task;
    private final int r;
    private final int nextR;
    private final StrainEstimator strain;

    public OverlapComputation(final TaskContainer task, final int r, final int nextR, final StrainEstimator strain) {
        this.task = task;
        this.r = r;
        this.nextR = nextR;
        this.strain = strain;
    }

    @Override
    public Void call() throws ComputationException {
        if (r + 1 != nextR) {
            task.setResult(r, nextR, new Result(DisplacementCalculator.computeCumulativeDisplacement(task, r, nextR)));
        }
        if (!task.getHints().contains(Hint.NO_STRAIN)) {
            strain.estimateStrain(task, r, nextR);
        }
        return null;
    }

}
