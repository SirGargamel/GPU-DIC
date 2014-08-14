package cz.tul.dic.engine.strain;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import java.util.HashMap;
import java.util.Map;
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

    public void computeStrain(final TaskContainer tc, final int round) throws ComputationException {
        final Object o = tc.getParameter(TaskParameter.STRAIN_ESTIMATION_METHOD);
        if (o == null) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "NULL strain estimation type.");
        }
        final StrainEstimationMethod type = (StrainEstimationMethod) o;

        if (data.containsKey(type)) {
            data.get(type).estimateStrain(tc, round);
        } else {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported strain estimation - " + type.toString());
        }

        Logger.trace("Strain  calculated for round {0} using {1}.", round, type);
    }

    public void computeStrain(final TaskContainer tc) throws ComputationException {
        int counter = 0;
        for (int r : TaskContainerUtils.getRounds(tc).keySet()) {
            computeStrain(tc, r);

            counter++;
            setChanged();
            notifyObservers(counter);
        }
    }

    public static abstract class StrainEstimator {

        abstract void estimateStrain(final TaskContainer tc, final int round);

    }

}