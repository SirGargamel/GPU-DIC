package cz.tul.dic.complextask;

import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import java.util.List;

/**
 *
 * @author Petr Jecmen
 */
public class FacetDeformationAnalyzator {

    public static double determineROIShift(final TaskContainer tc, final int round, final ROI roi) {
        final List<Facet> facets = tc.getFacets(round, roi);
        final double[][][] results = tc.getPerPixelResult(round);
        final int facetSize = tc.getFacetSize();
        final int facetSize2 = facetSize * facetSize;

        int[] counterPos = new int[10];
        int[] counterNeg = new int[10];

        int val;
        int[] facetData;
        for (Facet f : facets) {
            facetData = f.getData();
            for (int i = 0; i < facetSize2; i++) {
                val = (int) Math.round(results[facetData[i * 2]][facetData[i * 2 + 1]][Coordinates.Y]);
                if (val >= 0) {
                    if (val >= counterPos.length) {
                        int[] tmp = counterPos;
                        counterPos = new int[val+1];
                        System.arraycopy(tmp, 0, counterPos, 0, tmp.length);
                    }

                    counterPos[val]++;
                } else {
                    val = -val;

                    if (val >= counterNeg.length) {
                        int[] tmp = counterNeg;
                        counterNeg = new int[val+1];
                        System.arraycopy(tmp, 0, counterNeg, 0, tmp.length);
                    }

                    counterNeg[val]++;
                }
            }
        }

        val = 0;
        int maxCount = -1;
        for (int i = 0; i < counterPos.length; i++) {
            if (counterPos[i] > maxCount) {
                maxCount = counterPos[i];
                val = i;
            }
        }
        
        for (int i = 0; i < counterNeg.length; i++) {
            if (counterNeg[i] > maxCount) {
                maxCount = counterNeg[i];
                val = -i;
            }
        }

        return val;
    }

}
