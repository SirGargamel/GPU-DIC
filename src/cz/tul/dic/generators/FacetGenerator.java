package cz.tul.dic.generators;

import cz.tul.dic.data.TaskContainer;
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
    }
    
    public static void generateFacets(final TaskContainer tc) {
        final FacetGeneratorMode mode = (FacetGeneratorMode) tc.getParameter(FacetGeneratorMode.class);
        if (generators.containsKey(mode)) {
            generators.get(mode).generateFacets(tc);
        } else {
            throw new IllegalArgumentException("Unsupported mode of facet generator - " + mode.toString());
        }
    }
    
}
