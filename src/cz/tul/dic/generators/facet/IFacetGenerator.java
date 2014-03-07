package cz.tul.dic.generators.facet;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.task.TaskContainer;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Petr Jecmen
 */
public interface IFacetGenerator {

    List<Facet> generateFacets(final TaskContainer tc, final int round);
    
    FacetGeneratorMode getMode();
    
}
