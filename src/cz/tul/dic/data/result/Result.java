/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.result;

import cz.tul.dic.data.roi.ROI;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Lenam s.r.o.
 */
public class Result implements Serializable {

    private final Map<ROI, List<CorrelationResult>> correlations;
    private final DisplacementResult displacementResult;
    private final StrainResult strainResult;

    public Result(final Map<ROI, List<CorrelationResult>> correlations, final DisplacementResult displacement) {
        this.correlations = correlations;
        this.displacementResult = displacement;
        strainResult = null;
    }

    public Result(final DisplacementResult displacement) {
        this.displacementResult = displacement;
        correlations = null;
        strainResult = null;
    }

    public Result(final Result subResult, final StrainResult strainResult) {
        this.correlations = subResult.getCorrelations();
        this.displacementResult = subResult.getDisplacementResult();
        this.strainResult = strainResult;
    }

    public Map<ROI, List<CorrelationResult>> getCorrelations() {
        return correlations;
    }

    public DisplacementResult getDisplacementResult() {
        return displacementResult;
    }

    public StrainResult getStrainResult() {
        return strainResult;
    }
}
