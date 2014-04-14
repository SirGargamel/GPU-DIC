package cz.tul.dic.engine.cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Analyzer1D extends ClusterAnalyzer<Double> {

    @Override
    public Double findMajorValue() {
        final Map<Integer, Integer> counter = new HashMap<>();

        int val;
        for (Double d : values) {
            val = (int) Math.round(d / precision);
            if (counter.containsKey(val)) {
                counter.put(val, counter.get(val) + 1);
            } else {
                counter.put(val, 1);
            }
        }

        // find best
        int maxCnt = -1, maxVal = 0;
        for (Entry<Integer, Integer> e : counter.entrySet()) {
            val = e.getValue();
            if (val > maxCnt) {
                maxCnt = val;
                maxVal = e.getKey();
            }
        }

        return maxVal * precision;
        
//        final double[][][] results = tc.getPerPixelResult(round);
//
//        int[] counterPos = new int[50];
//        int[] counterNeg = new int[50];
//
//        int val;        
//        for (int x = roi.getX1(); x <= roi.getX2(); x++) {
//            for (int y = roi.getY1(); y <= roi.getY2(); y++) {
//                if (roi.isPointInside(x, y) && results[x][y] != null) {
//                    val = (int) Math.round(10 * results[x][y][Coordinates.Y]);
//                    if (val >= 0) {
//                        if (val >= counterPos.length) {
//                            int[] tmp = counterPos;
//                            counterPos = new int[val + 1];
//                            System.arraycopy(tmp, 0, counterPos, 0, tmp.length);
//                        }
//
//                        counterPos[val]++;
//                    } else {
//                        val = -val;
//
//                        if (val >= counterNeg.length) {
//                            int[] tmp = counterNeg;
//                            counterNeg = new int[val + 1];
//                            System.arraycopy(tmp, 0, counterNeg, 0, tmp.length);
//                        }
//
//                        counterNeg[val]++;
//                    }
//                }
//
//            }
//        }
//
//        val = 0;
//        int count = -1;
//        for (int i = 0; i < counterPos.length; i++) {
//            if (counterPos[i] > count) {
//                count = counterPos[i];
//                val = i;
//            }
//        }
//
//        for (int i = 0; i < counterNeg.length; i++) {
//            if (counterNeg[i] > count) {
//                count = counterNeg[i];
//                val = -i;
//            }
//        }
//
//        return val / 10.0;
    }

}
