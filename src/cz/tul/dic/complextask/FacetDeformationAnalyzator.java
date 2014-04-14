package cz.tul.dic.complextask;

import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;

/**
 *
 * @author Petr Jecmen
 */
public class FacetDeformationAnalyzator {

    public static double determineROIShift(final TaskContainer tc, final int round, final ROI roi) {
        final double[][][] results = tc.getPerPixelResult(round);

        int[] counterPos = new int[50];
        int[] counterNeg = new int[50];

        int val;
        for (int x = roi.getX1(); x <= roi.getX2(); x++) {
            for (int y = roi.getY1(); y <= roi.getY2(); y++) {
                if (roi.isPointInside(x, y)) {
                    val = (int) Math.round(10 * results[x][y][Coordinates.Y]);
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
    }

}
