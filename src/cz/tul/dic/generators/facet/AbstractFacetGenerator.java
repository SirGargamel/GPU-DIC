package cz.tul.dic.generators.facet;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.TaskContainer;

/**
 *
 * @author Petr Jecmen
 */
public abstract class AbstractFacetGenerator {

    public abstract void generateFacets(final TaskContainer tc, final int round) throws ComputationException;
    
    public abstract FacetGeneratorMode getMode();
    
    protected boolean checkAreaValidity(final int x1, final int y1, final int x2, final int y2, final int width, final int height) {
        boolean result = true;
        
        result &= x1 >= 0;
        result &= x2 >= 0;
        result &= y1 >= 0;
        result &= y2 >= 0;
        
        result &= x1 < width;
        result &= x2 < width;
        result &= y1 < height;
        result &= y2 < height;
        
        return result;
    }
    
}
