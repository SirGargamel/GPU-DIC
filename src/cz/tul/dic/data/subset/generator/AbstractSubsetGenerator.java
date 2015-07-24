/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.subset.generator;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.roi.AbstractROI;
import cz.tul.dic.data.task.TaskContainer;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Petr Jecmen
 */
public abstract class AbstractSubsetGenerator {

    public abstract Map<AbstractROI, List<AbstractSubset>> generateFacets(final TaskContainer tc, final int round) throws ComputationException;
    
    public abstract SubsetGeneratorMethod getMode();
    
    protected boolean checkAreaValidity(final double x1, final double y1, final double x2, final double y2, final double width, final double height) {
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
