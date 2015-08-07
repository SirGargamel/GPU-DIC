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
import cz.tul.dic.data.task.TaskParameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class SubsetGenerator {

    private static final Map<SubsetGeneratorMethod, AbstractSubsetGenerator> generators;

    static {
        generators = new HashMap<>();

        AbstractSubsetGenerator fg = new EqualSpacingSubsetGenerator();
        generators.put(fg.getMode(), fg);
    }

    private SubsetGenerator() {
    }

    public static Map<AbstractROI, List<AbstractSubset>> generateSubsets(final TaskContainer tc, final int round) throws ComputationException {
        final SubsetGeneratorMethod mode = (SubsetGeneratorMethod) tc.getParameter(TaskParameter.SUBSET_GENERATOR_METHOD);
        if (generators.containsKey(mode)) {
            final Map<AbstractROI, List<AbstractSubset>> result = generators.get(mode).generateSubsets(tc, round);
            final StringBuilder sb = new StringBuilder();
            for (Entry<AbstractROI, List<AbstractSubset>> e : result.entrySet()) {
                sb.append(e.getKey().toString());
                sb.append(" -- ");
                sb.append(e.getValue().size());
                sb.append("; ");
            }
            Logger.trace(sb.toString());
            return result;
        } else {
            throw new IllegalArgumentException("Unsupported mode of subset generator - " + mode);
        }
    }

}
