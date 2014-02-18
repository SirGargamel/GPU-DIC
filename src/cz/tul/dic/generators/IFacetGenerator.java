package cz.tul.dic.generators;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.TaskContainer;
import java.util.Set;

/**
 *
 * @author Petr Jecmen
 */
public interface IFacetGenerator {

    public Set<Facet> generateFacets(final TaskContainer tc);
    
}
