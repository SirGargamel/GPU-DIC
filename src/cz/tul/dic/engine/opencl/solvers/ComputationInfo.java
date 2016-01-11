/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.solvers;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.data.subset.AbstractSubset;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Petr Ječmen
 */
@XStreamAlias("ComputationInfo")
public class ComputationInfo {
    
    private final AbstractSubset subset;
    @XStreamAlias("Results")
    private final List<CorrelationResult> results;
    private String terminationInfo;

    public ComputationInfo(AbstractSubset subset) {
        this.subset = subset;
        results = new LinkedList<>();
        terminationInfo = "Max iteration count reached";
    }

    public void setTerminationInfo(String terminationInfo) {
        this.terminationInfo = terminationInfo;
    }
    
    public void addResult(final CorrelationResult result) {
        results.add(result);
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        
        sb.append(subset).append(" : ").append(terminationInfo).append(" after ").append(results.size()).append(" rounds; ");
        for (CorrelationResult result : results) {
            sb.append("{");
            sb.append(result);
            sb.append("}, ");
        }
        sb.setLength(sb.length() - ", ".length());        
        
        return sb.toString();
    }
    
}
