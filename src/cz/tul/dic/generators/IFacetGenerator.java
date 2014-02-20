package cz.tul.dic.generators;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.task.TaskContainer;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Petr Jecmen
 */
public interface IFacetGenerator {

    List<List<Facet>> generateFacets(final TaskContainer tc);
    
    FacetGeneratorMode getMode();
    
}
