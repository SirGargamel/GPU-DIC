package cz.tul.dic.engine.displacement;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.strain.Differentiator;
import cz.tul.dic.engine.strain.LocalLeastSquare;
import cz.tul.dic.engine.strain.StrainEstimationType;
import cz.tul.dic.engine.strain.StrainEstimator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public abstract class DisplacementCalculator {
    
    private static final Map<DisplacementCalculationType, DisplacementCalculator> data;
    
    static {
        data = new HashMap<>();
        data.put(DisplacementCalculationType.FIND_MAX_AND_AVERAGE, new FindMaxAndAverage());        
    }        
    
    public static void computeDisplacement(final TaskContainer tc, final int round, final Map<ROI, List<Facet>> facetMap) throws ComputationException {
        Logger.trace("Computing displacement for round {0}.", round);
        
        final Object o = tc.getParameter(TaskParameter.DISPLACEMENT_CALCULATION_TYPE);
        if (o == null) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "NULL displacement calculation type.");
        }
        final DisplacementCalculationType type = (DisplacementCalculationType) o;
        
        if (data.containsKey(type)) {
            data.get(type).buildFinalResults(tc, round, facetMap);
        } else {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported strain estimation - " + type.toString());
        }        
    }
    
    abstract void buildFinalResults(final TaskContainer tc, final int round, final Map<ROI, List<Facet>> facetMap) throws ComputationException;
    
}
