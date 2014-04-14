package cz.tul.dic.engine.cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Analyzer2D extends ClusterAnalyzer<double[]> {

    @Override
    public double[] findMajorValue() {
        final Map<Integer, Map<Integer, Integer>> counter = new HashMap<>();

        int valX, valY;
        Map<Integer, Integer> m;
        for (double[] d : values) {
            valX = (int) Math.round(d[0] / precision);
            valY = (int) Math.round(d[1] / precision);

            if (counter.containsKey(valX)) {
                m = counter.get(valX);
            } else {
                m = new HashMap<>();
                counter.put(valX, m);
            }

            if (m.containsKey(valY)) {
                m.put(valY, m.get(valY) + 1);
            } else {
                m.put(valY, 1);
            }
        }

        int maxCount = -1;
        int maxDx = 0;
        int maxDy = 0;
        int val;
        for (Entry<Integer, Map<Integer, Integer>> dX : counter.entrySet()) {
            for (Entry<Integer, Integer> dY : dX.getValue().entrySet()) {
                val = dY.getValue();
                if (val > maxCount) {
                    maxCount = val;
                    maxDx = dX.getKey();
                    maxDy = dY.getKey();
                }
            }
        }
        return new double[]{maxDx * precision, maxDy * precision};        

//        @SuppressWarnings("unchecked")
//        final Map<Integer, Map<Integer, Integer>>[][] counter = new Map[width][height];
//        List<Facet> facets;
//        List<double[][]> results;
//        Facet f;
//        double[] d;
//        int x, y, valX, valY;
//        Map<Integer, Map<Integer, Integer>> map;
//        Map<Integer, Integer> mapX;
//        Map<int[], double[]> deformedFacet;
//        DeformationDegree degree;
//        for (ROI roi : tc.getRois(round)) {
//            facets = facetMap.get(roi);
//            results = tc.getResults(round, roi);
//
//            degree = DeformationUtils.getDegree(results.get(0)[0]);
//
//            for (int i = 0; i < facets.size(); i++) {
//                f = facets.get(i);
//                d = results.get(i)[0];
//
//                deformedFacet = FacetUtils.deformFacet(f, d, degree);
//                for (Entry<int[], double[]> e : deformedFacet.entrySet()) {
//                    x = e.getKey()[Coordinates.X];
//                    y = e.getKey()[Coordinates.Y];
//                    valX = (int) Math.round(PRECISION * e.getValue()[Coordinates.X]);
//                    valY = (int) Math.round(PRECISION * e.getValue()[Coordinates.Y]);
//
//                    map = counter[x][y];
//                    if (map == null) {
//                        map = new HashMap<>();
//                        counter[x][y] = map;
//                    }
//                    mapX = map.get(valX);
//                    if (mapX == null) {
//                        mapX = new HashMap<>();
//                        map.put(valX, mapX);
//                    }
//                    if (mapX.containsKey(valY)) {
//                        mapX.put(valY, mapX.get(valY) + 1);
//                    } else {
//                        mapX.put(valY, 1);
//                    }
//                }
//            }
//        }
//
//        int maxCount, maxDx, maxDy, val;
//        for (int i = 0; i < width; i++) {
//            for (int j = 0; j < height; j++) {
//                map = counter[i][j];
//                if (map != null) {
//                    maxCount = -1;
//                    maxDx = 0;
//                    maxDy = 0;
//                    for (Entry<Integer, Map<Integer, Integer>> dX : map.entrySet()) {
//                        for (Entry<Integer, Integer> dY : dX.getValue().entrySet()) {
//                            val = dY.getValue();
//                            if (val > maxCount) {
//                                maxCount = val;
//                                maxDx = dX.getKey();
//                                maxDy = dY.getKey();
//                            }
//                        }
//                    }
//                    finalResults[i][j] = new double[]{maxDx / PRECISION, maxDy / PRECISION};
//                }
//            }
//        }
    }

}
