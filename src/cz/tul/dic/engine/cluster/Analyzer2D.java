/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.cluster;

import cz.tul.dic.engine.cluster.Analyzer2D.Analayzer2DData;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

public class Analyzer2D extends AbstractClusterAnalyzer<Analayzer2DData> {

    private final Map<Integer, Map<Integer, Integer>> counterVal = new HashMap<>();
    private final Map<Integer, Map<Integer, Double>> counterQ = new HashMap<>();

    public Analyzer2D() {
        values = new LinkedList<>();
    }

    @Override
    public Analayzer2DData findMajorValue() {
        int valX, valY;
        Map<Integer, Integer> mVal;
        Map<Integer, Double> mQ;
        for (Analayzer2DData val : values) {
            valX = (int) Math.round(val.getX() / precision);
            valY = (int) Math.round(val.getY() / precision);

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
                mQ.put(valY, mQ.get(valY) + val.getQuality());
            } else {
                mVal.put(valY, 1);
                mQ.put(valY, val.getQuality());
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
        return new Analayzer2DData(maxDx * precision, maxDy * precision, q);
    }

    public static class Analayzer2DData {

        private final double x, y, quality;

        public Analayzer2DData(double x, double y, double quality) {
            this.x = x;
            this.y = y;
            this.quality = quality;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getQuality() {
            return quality;
        }
    }

}
