package cz.tul.dic.engine.strain;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class StrainEstimation extends Observable {

    private static final Map<StrainEstimationMethod, StrainEstimator> data;

    static {
        data = new HashMap<>();
        data.put(StrainEstimationMethod.LOCAL_LEAST_SQUARES, new LocalLeastSquare());
    }

    public void computeStrain(final TaskContainer tc, final int roundFrom, int roundTo) throws ComputationException {
        final Object o = tc.getParameter(TaskParameter.STRAIN_ESTIMATION_METHOD);
        if (o == null) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "NULL strain estimation type.");
        }
        final StrainEstimationMethod type = (StrainEstimationMethod) o;

        if (data.containsKey(type)) {
            data.get(type).estimateStrain(tc, roundFrom, roundTo);
        } else {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported strain estimation - " + type.toString());
        }

        Logger.trace("Strain  calculated for {0}:{1} using {2}.", roundFrom, roundTo, type);
    }

    public void computeStrain(final TaskContainer tc) throws ComputationException {
        final int roudZero = TaskContainerUtils.getFirstRound(tc);
        int counter = 0;
        for (Entry<Integer, Integer> e : TaskContainerUtils.getRounds(tc).entrySet()) {
            computeStrain(tc, e.getKey(), e.getValue());
            computeStrain(tc, roudZero, e.getValue());

            counter++;
            setChanged();
            notifyObservers(counter);
        }
    }

    public static abstract class StrainEstimator {

        abstract void estimateStrain(final TaskContainer tc, final int roundFrom, int roundTo) throws ComputationException;

    }

}
