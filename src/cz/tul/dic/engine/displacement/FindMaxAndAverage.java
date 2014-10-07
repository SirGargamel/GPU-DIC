package cz.tul.dic.engine.displacement;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.Utils;
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.FacetUtils;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.DisplacementResult;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.debug.DebugControl;
import cz.tul.dic.debug.Stats;
import cz.tul.dic.engine.CorrelationResult;
import cz.tul.dic.engine.ResultCompilation;
import cz.tul.dic.engine.cluster.Analyzer2D;
import cz.tul.dic.output.NameGenerator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.pmw.tinylog.Logger;

public class FindMaxAndAverage extends DisplacementCalculator {

    private static final double PRECISION = 0.5;

    @Override
    public void buildFinalResults(TaskContainer tc, int round, int nextRound, Map<ROI, List<Facet>> facetMap) throws ComputationException {
        final Image img = tc.getImage(round);
        final int width = img.getWidth();
        final int height = img.getHeight();
        final double resultQuality = (double) tc.getParameter(TaskParameter.RESULT_QUALITY);

        final int linesPerGroup = (int) tc.getParameter(TaskParameter.DISPLACEMENT_CALCULATION_PARAM) / width;
        final int groupCount = (int) Math.ceil(height / (double) linesPerGroup);

        final double[][][] finalDisplacement = new double[width][height][];
        final double[][] finalQuality = Utils.generateNaNarray(width, height);
        final Map<Integer, Map<Integer, Analyzer2D>> counters = new HashMap<>();
        List<Facet> facets;
        List<CorrelationResult> results;
        Facet f;
        double[] d;
        int x, y, lowerBound, upperBound = 0;
        Analyzer2D counter;
        Map<int[], double[]> deformedFacet;
        CorrelationResult cr;
        double quality;
        for (int g = 0; g < groupCount; g++) {
            lowerBound = upperBound;
            upperBound += linesPerGroup;
            upperBound = Math.min(upperBound, height - 1);
            counters.clear();

            for (ROI roi : tc.getRois(round)) {
                facets = facetMap.get(roi);
                results = tc.getResult(round, roi);

                for (int i = 0; i < facets.size(); i++) {
                    if (results.get(i) == null) {
                        continue;
                    }
                    cr = results.get(i);
                    if (cr.getValue() < resultQuality) {
                        continue;
                    }

                    d = cr.getDeformation();
                    quality = cr.getValue();
                    
                    f = facets.get(i);
                    if (f == null) {
                        Logger.warn("No facet - {0}", f);
                        continue;
                    }
                    if (!FacetUtils.areLinesInsideFacet(f, lowerBound, upperBound)) {
                        continue;
                    }

                    deformedFacet = FacetUtils.deformFacet(f, d);
                    for (Map.Entry<int[], double[]> e : deformedFacet.entrySet()) {
                        x = e.getKey()[Coordinates.X];
                        y = e.getKey()[Coordinates.Y];

                        if (y >= lowerBound && y <= upperBound) {
                            getAnalyzer(counters, x, y).addValue(new double[] {e.getValue()[0], e.getValue()[1], quality});
                        }
                    }
                }
            }

            double[] majorVal, val = new double[2];            
            int count;
            double maxDist2 = 4 * PRECISION * PRECISION;
            final ResultCompilation rc = (ResultCompilation) tc.getParameter(TaskParameter.RESULT_COMPILATION);
            if (rc == null) {
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "No result compilation method.");
            }

            for (Entry<Integer, Map<Integer, Analyzer2D>> eX : counters.entrySet()) {
                x = eX.getKey();
                for (Entry<Integer, Analyzer2D> eY : eX.getValue().entrySet()) {
                    y = eY.getKey();
                    counter = eY.getValue();
                    if (counter != null) {
                        majorVal = counter.findMajorValue();
                        if (rc.equals(ResultCompilation.MAJOR)) {
                            finalDisplacement[x][y] = new double[]{majorVal[0], majorVal[1]};
                        } else if (rc.equals(ResultCompilation.MAJOR_AVERAGING)) {
                            count = 0;
                            val[0] = 0;
                            val[1] = 0;
                            quality = 0;

                            for (double[] vals : counter.listValues()) {
                                if (dist2(vals, majorVal) <= maxDist2) {
                                    val[0] += vals[0];
                                    val[1] += vals[1];
                                    quality += vals[2];
                                    count++;
                                }
                            }

                            finalDisplacement[x][y] = new double[]{val[0] / (double) count, val[1] / (double) count};
                            finalQuality[x][y] = quality / (double) count;
                        } else {
                            throw new UnsupportedOperationException("Unsupported method of result compilation - " + rc);
                        }

                        if (DebugControl.isDebugMode()) {
                            Stats.exportPointSubResultsStatistics(counter, NameGenerator.generate2DValueHistogram(tc, nextRound, x, y));
                        }
                    }
                }
            }
        }

        tc.setDisplacement(round, nextRound, new DisplacementResult(finalDisplacement, finalQuality));
    }

    private Analyzer2D getAnalyzer(final Map<Integer, Map<Integer, Analyzer2D>> maps, final int x, final int y) {
        Map<Integer, Analyzer2D> m = maps.get(x);
        if (m == null) {
            m = new HashMap<>(1);
            maps.put(x, m);
        }

        Analyzer2D result = m.get(y);
        if (result == null) {
            result = new Analyzer2D();
            m.put(y, result);
        }

        return result;
    }

    private static double dist2(final double[] val1, final double[] val2) {
        double a = val2[0] - val1[0];
        double b = val2[1] - val1[1];
        return a * a + b * b;
    }

}
