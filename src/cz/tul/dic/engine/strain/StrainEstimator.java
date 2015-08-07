/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.strain;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.Engine;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.concurrent.ExecutorService;

/**
 *
 * @author Petr Ječmen
 */
public abstract class StrainEstimator extends Observable {

    private static final Map<StrainEstimationMethod, StrainEstimator> data;
    protected final ExecutorService exec;

    static {
        data = new HashMap<>();
        data.put(StrainEstimationMethod.LOCAL_LEAST_SQUARES, new LocalLeastSquare());
    }

    public StrainEstimator() {
        this.exec = Engine.getInstance().getExecutorService();
    }
    
    public static StrainEstimator initStrainEstimator(final StrainEstimationMethod type) {
        if (data.containsKey(type)) {
            return data.get(type);
        } else {
            throw new IllegalArgumentException("Unsupported strain estimation - " + type.toString());
        }
    }    

    public abstract void estimateStrain(final TaskContainer tc, final int roundFrom, int roundTo);

    public abstract void stop();

    public static void computeStrain(final TaskContainer tc) {
        final int roudZero = TaskContainerUtils.getFirstRound(tc);
        final StrainEstimator estimator = StrainEstimator.initStrainEstimator((StrainEstimationMethod) tc.getParameter(TaskParameter.STRAIN_ESTIMATION_METHOD));
        for (Entry<Integer, Integer> e : TaskContainerUtils.getRounds(tc).entrySet()) {
            estimator.estimateStrain(tc, e.getKey(), e.getValue());

            estimator.estimateStrain(tc, roudZero, e.getValue());
        }
    }
}
