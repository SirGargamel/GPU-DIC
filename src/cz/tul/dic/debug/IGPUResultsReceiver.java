package cz.tul.dic.debug;

import cz.tul.dic.data.Facet;
import java.util.List;

/**
 *
 * @author Petr Ječmen
 */
public interface IGPUResultsReceiver {
    
    void dumpGpuResults(final float[] resultData, final List<Facet> facets, final List<double[]> deformationLimits);
    
}
