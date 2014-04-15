package cz.tul.dic.complextask;

import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.engine.cluster.Analyzer1D;

/**
 *
 * @author Petr Jecmen
 */
public class FacetDeformationAnalyzator {
    
    private static final double PRECISION = 0.1;

    public static double determineROIShift(final TaskContainer tc, final int round, final ROI roi) {
        final double[][][] results = tc.getPerPixelResult(round);
        final Analyzer1D analyzer = new Analyzer1D();
        analyzer.setPrecision(PRECISION);

        for (int x = roi.getX1(); x <= roi.getX2(); x++) {
            for (int y = roi.getY1(); y <= roi.getY2(); y++) {
                if (roi.isPointInside(x, y) && results[x][y] != null) {
                    analyzer.addValue(results[x][y][Coordinates.Y]);
                }
            }
        }

        return analyzer.findMajorValue();
    }

}
