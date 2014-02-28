package cz.tul.dic.generators.facet;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Petr Jecmen
 */
public class FacetGenerator {

    private static final Map<FacetGeneratorMode, IFacetGenerator> generators;

    static {
        generators = new HashMap<>();

        IFacetGenerator fg = new SimpleFacetGenerator();
        generators.put(fg.getMode(), fg);
    }

    public static void generateFacets(final TaskContainer tc) {
        final FacetGeneratorMode mode = (FacetGeneratorMode) tc.getParameter(TaskParameter.FACET_GENERATOR_MODE);
        if (generators.containsKey(mode)) {
            tc.assignFacets(generators.get(mode).generateFacets(tc));
        } else {
            throw new IllegalArgumentException("Unsupported mode of facet generator - " + mode.toString());
        }
    }

}
