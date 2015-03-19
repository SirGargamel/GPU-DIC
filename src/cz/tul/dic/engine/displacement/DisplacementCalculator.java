/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.displacement;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public abstract class DisplacementCalculator {

    private static final Map<DisplacementCalculation, DisplacementCalculator> data;

    static {
        data = new HashMap<>();
        data.put(DisplacementCalculation.FIND_MAX_AND_AVERAGE, new FindMaxAndAverage());
    }

    public static void computeDisplacement(final TaskContainer tc, final int round, int nextRound, final Map<ROI, List<Facet>> facetMap) throws ComputationException {
        final Object o = tc.getParameter(TaskParameter.DISPLACEMENT_CALCULATION_METHOD);
        if (o == null) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "NULL displacement calculation type.");
        }
        final DisplacementCalculation type = (DisplacementCalculation) o;

        if (data.containsKey(type)) {
            data.get(type).buildFinalResults(tc, round, nextRound, facetMap);
        } else {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported strain estimation - " + type.toString());
        }
        Logger.trace("Displacement calculated for round {0} using {1}.", round, type);
    }

    abstract void buildFinalResults(final TaskContainer tc, final int round, int nextRound, final Map<ROI, List<Facet>> facetMap) throws ComputationException;

}
