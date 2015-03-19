/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.cluster;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Analyzer2D extends ClusterAnalyzer<double[]> {

    private final Map<Integer, Map<Integer, Integer>> counterVal = new HashMap<>();
    private final Map<Integer, Map<Integer, Double>> counterQ = new HashMap<>();
    private final List<double[]> values;

    public Analyzer2D() {
        values = new LinkedList<>();
    }

    @Override
    public double[] findMajorValue() {
        int valX, valY;
        Map<Integer, Integer> mVal;
        Map<Integer, Double> mQ;
        for (double[] val : values) {
            valX = (int) Math.round(val[0] / precision);
            valY = (int) Math.round(val[1] / precision);

            if (counterVal.containsKey(valX)) {
                mVal = counterVal.get(valX);
                mQ = counterQ.get(valX);
            } else {
                mVal = new HashMap<>();
                counterVal.put(valX, mVal);
                mQ = new HashMap<>();
                counterQ.put(valX, mQ);
            }

            if (mVal.containsKey(valY)) {
                mVal.put(valY, mVal.get(valY) + 1);
                mQ.put(valY, mQ.get(valY) + val[2]);
            } else {
                mVal.put(valY, 1);
                mQ.put(valY, val[2]);
            }
        }

        int maxCount = -1;
        int maxDx = 0;
        int maxDy = 0;
        int val;
        double q = 0;
        for (Entry<Integer, Map<Integer, Integer>> dX : counterVal.entrySet()) {
            for (Entry<Integer, Integer> dY : dX.getValue().entrySet()) {
                val = dY.getValue();
                if (val > maxCount) {
                    maxCount = val;
                    maxDx = dX.getKey();
                    maxDy = dY.getKey();
                    q = counterQ.get(dX.getKey()).get(dY.getKey());
                }
            }
        }
        return new double[]{maxDx * precision, maxDy * precision, q};
    }

    @Override
    public void addValue(double[] val) {
        values.add(new double[]{val[0], val[1], val[2]});
    }

    public List<double[]> listValues() {
        return values;
    }

}
