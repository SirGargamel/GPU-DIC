package cz.tul.dic.engine.cluster;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Analyzer2D extends ClusterAnalyzer<double[]> {

    private final Map<Integer, Map<Integer, Integer>> counter = new HashMap<>();
    private final List<double[]> values;

    public Analyzer2D() {
        values = new LinkedList<>();
    }

    @Override
    public double[] findMajorValue() {
        int valX, valY;
        Map<Integer, Integer> m;
        for (double[] val : values) {
            valX = (int) Math.round(val[0] / precision);
            valY = (int) Math.round(val[1] / precision);

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
    }

    @Override
    public void addValue(double[] val) {
        values.add(new double[]{val[0], val[1]});
    }

    public List<double[]> listValues() {
        return values;
    }

}
