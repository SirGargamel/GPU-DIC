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
        final int facetSize = tc.getFacetSize(round, roi);
        final int facetSize2 = facetSize * facetSize;

        int[] counterPos = new int[50];
        int[] counterNeg = new int[50];

        int val;
        int[] facetData;
        for (Facet f : facets) {
            facetData = f.getData();
            for (int i = 0; i < facetSize2; i++) {
                val = (int) Math.round(10 * results[facetData[i * 2]][facetData[i * 2 + 1]][Coordinates.Y]);
                if (val >= 0) {
                    if (val >= counterPos.length) {
                        int[] tmp = counterPos;
                        counterPos = new int[val + 1];
                        System.arraycopy(tmp, 0, counterPos, 0, tmp.length);
                    }

                    counterPos[val]++;
                } else {
                    val = -val;

                    if (val >= counterNeg.length) {
                        int[] tmp = counterNeg;
                        counterNeg = new int[val + 1];
                        System.arraycopy(tmp, 0, counterNeg, 0, tmp.length);
                    }

                    counterNeg[val]++;
                }
            }
        }

        val = 0;
        int count = -1;
        for (int i = 0; i < counterPos.length; i++) {
            if (counterPos[i] > count) {
                count = counterPos[i];
                val = i;
            }
        }

        for (int i = 0; i < counterNeg.length; i++) {
            if (counterNeg[i] > count) {
                count = counterNeg[i];
                val = -i;
            }
        }
        
        return val / 10.0;

//        double result = 0, tmp;
//        count = 0;
//        for (Facet f : facets) {
//            facetData = f.getData();
//            for (int i = 0; i < facetSize2; i++) {
//                tmp = results[facetData[i * 2]][facetData[i * 2 + 1]][Coordinates.Y];
//                if (tmp >= val && (tmp - val) < 0.5) {
//                    result += tmp;
//                    count++;
//                } else if (tmp < val && (val - tmp) <= 0.5) {
//                    result += tmp;
//                    count++;
//                }
//            }
//        }

//        return result / (double) count;
    }

}
