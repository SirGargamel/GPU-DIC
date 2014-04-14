package cz.tul.dic.generators.facet;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Container;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Petr Jecmen
 */
public class FacetGenerator {

    private static final Map<FacetGeneratorMode, AbstractFacetGenerator> generators;

    static {
        generators = new HashMap<>();

        AbstractFacetGenerator fg = new SimpleFacetGenerator();
        generators.put(fg.getMode(), fg);
        fg = new TightFacetGenerator();
        generators.put(fg.getMode(), fg);
    }

    public static Map<ROI, List<Facet>> generateFacets(final TaskContainer tc, final int round) throws ComputationException {
        final FacetGeneratorMode mode = (FacetGeneratorMode) tc.getParameter(TaskParameter.FACET_GENERATOR_MODE);
        if (generators.containsKey(mode)) {            
            return generators.get(mode).generateFacets(tc, round);
        } else {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported mode of facet generator - " + mode.toString());
        }
    }

}
