/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.debug;

import cz.tul.dic.data.subset.AbstractSubset;
import java.util.List;

/**
 *
 * @author Petr Ječmen
 */
public interface IGPUResultsReceiver {
    
    void dumpGpuResults(final float[] resultData, final List<AbstractSubset> subsets, final List<double[]> deformationLimits);
    
}
