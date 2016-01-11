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
import java.util.HashMap;
import java.util.List;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public abstract class AbstractSubsetGenerator {

    public static AbstractSubsetGenerator initGenerator(final SubsetGenerator method) {
        try {
            final Class<?> cls = Class.forName("cz.tul.dic.data.subset.generator.".concat(method.getClassName()));
            return (AbstractSubsetGenerator) cls.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            Logger.warn(ex, "Error instantiating class {}, using default subset generator.", method);
            return new EqualSpacingSubsetGenerator();
        }
    }

    public abstract HashMap<AbstractROI, List<AbstractSubset>> generateSubsets(final TaskContainer task, final int round) throws ComputationException;

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
