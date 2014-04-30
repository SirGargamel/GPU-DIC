package cz.tul.dic.engine.strain;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Petr Jecmen
 */
public abstract class StrainEstimator {
    
    private static final Map<StrainEstimationType, StrainEstimator> data;
    
    static {
        data = new HashMap<>();
        data.put(StrainEstimationType.DIFFERENTIATION, new Differentiator());
        data.put(StrainEstimationType.LOCAL_LEAST_SQUARES, new LocalLeastSquare());
    }
    
    abstract void estimateStrain(final TaskContainer tc, final int round);
    
    public static void computeStrain(final TaskContainer tc, final int round) throws ComputationException {
        final Object o = tc.getParameter(TaskParameter.STRAIN_ESTIMATION_METHOD);
        if (o == null) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "NULL strain estimation type.");
        }
        final StrainEstimationType type = (StrainEstimationType) o;
        
        if (data.containsKey(type)) {
            data.get(type).estimateStrain(tc, round);
        } else {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported strain estimation - " + type.toString());
        }        
    }
    
}
