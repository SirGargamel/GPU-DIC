package cz.tul.dic.engine.displacement;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.FacetUtils;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.ResultCompilation;
import cz.tul.dic.engine.cluster.Analyzer2D;
import java.util.List;
import java.util.Map;

public class FindMaxAndAverage extends DisplacementCalculator {

    private static final double PRECISION = 0.5;

    @Override
    public void buildFinalResults(TaskContainer tc, int round, Map<ROI, List<Facet>> facetMap) throws ComputationException {
        final Image img = tc.getImage(round);
        final int width = img.getWidth();
        final int height = img.getHeight();

        final double[][][] finalResults = new double[width][height][];
        final Analyzer2D[][] counters = new Analyzer2D[width][height];
        List<Facet> facets;
        List<double[][]> results;
        Facet f;
        double[] d;
        int x, y;
        Analyzer2D counter;
        Map<int[], double[]> deformedFacet;
        DeformationDegree degree;
        StringBuilder sb = new StringBuilder();
//        System.out.println("Round " + round);

        for (ROI roi : tc.getRois(round)) {
            facets = facetMap.get(roi);
            results = tc.getResults(round, roi);

            degree = DeformationUtils.getDegree(results.get(0)[0]);

            for (int i = 0; i < facets.size(); i++) {
                f = facets.get(i);
                d = results.get(i)[0];

//                if (roi instanceof RectangleROI) {
//                    sb.setLength(0);
//                    for (double val : d) {
//                        sb.append(val);
//                        sb.append(";");
//                    }
//                    sb.setLength(sb.length() - 1);
//                    System.out.println(sb.toString());
//                }
                deformedFacet = FacetUtils.deformFacet(f, d, degree);
                for (Map.Entry<int[], double[]> e : deformedFacet.entrySet()) {
                    x = e.getKey()[Coordinates.X];
                    y = e.getKey()[Coordinates.Y];

                    counter = counters[x][y];
                    if (counter == null) {
                        counter = new Analyzer2D();
                        counter.setPrecision(PRECISION);
                        counters[x][y] = counter;
                    }
                    counter.addValue(e.getValue());
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

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                counter = counters[i][j];
                if (counter != null) {
                    majorVal = counter.findMajorValue();
                    if (rc.equals(ResultCompilation.MAJOR)) {
                        finalResults[i][j] = new double[]{majorVal[0], majorVal[1]};
                    } else if (rc.equals(ResultCompilation.MAJOR_AVERAGING)) {
                        count = 0;
                        val[0] = 0;
                        val[1] = 0;

                        for (double[] vals : counter.listValues()) {
                            if (dist2(vals, majorVal) <= maxDist2) {
                                val[0] += vals[0];
                                val[1] += vals[1];
                                count++;
                            }
                        }

                        finalResults[i][j] = new double[]{val[0] / (double) count, val[1] / (double) count};
                    } else {
                        throw new UnsupportedOperationException("Unsupported method of result compilation - " + rc);
                    }
                }
            }
        }

        tc.setDisplacement(round, finalResults);
    }
    
    private static double dist2(final double[] val1, final double[] val2) {
        double a = val2[0] - val1[0];
        double b = val2[1] - val1[1];
        return a * a + b * b;
    }

}
