package cz.tul.dic.engine.cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Analyzer2D extends ClusterAnalyzer<double[]> {

    final Map<Integer, Map<Integer, Integer>> counter = new HashMap<>();

    @Override
    public double[] findMajorValue() {
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
    }

    @Override
    public void addValue(double[] val) {
        final int valX = (int) Math.round(val[0] / precision);
        final int valY = (int) Math.round(val[1] / precision);

        Map<Integer, Integer> m;
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

}
