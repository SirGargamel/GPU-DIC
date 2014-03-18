package cz.tul.dic.generators.facet;

import cz.tul.dic.data.task.TaskContainer;

/**
 *
 * @author Petr Jecmen
 */
public interface IFacetGenerator {

    void generateFacets(final TaskContainer tc, final int round);
    
    FacetGeneratorMode getMode();
    
}
