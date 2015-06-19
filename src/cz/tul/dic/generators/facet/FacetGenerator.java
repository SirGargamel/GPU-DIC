/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.generators.facet;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.roi.ROI;
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
public class FacetGenerator {

    private static final Map<FacetGeneratorMethod, AbstractFacetGenerator> generators;

    static {
        generators = new HashMap<>();
        
        AbstractFacetGenerator fg = new TightFacetGenerator();
        generators.put(fg.getMode(), fg);
    }

    public static Map<ROI, List<Facet>> generateFacets(final TaskContainer tc, final int round) throws ComputationException {
        final FacetGeneratorMethod mode = (FacetGeneratorMethod) tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD);
        if (generators.containsKey(mode)) {            
            final Map<ROI, List<Facet>> result = generators.get(mode).generateFacets(tc, round);
            final StringBuilder sb = new StringBuilder();
            for (Entry<ROI, List<Facet>> e : result.entrySet()) {
                sb.append(e.getKey().toString());
                sb.append(" -- ");
                sb.append(e.getValue().size());
                sb.append("; ");
            }
            Logger.trace(sb.toString());
            return result;
        } else {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported mode of facet generator - " + mode.toString());
        }
    }

}
