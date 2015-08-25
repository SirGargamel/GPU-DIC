/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Analyzer1D extends AbstractClusterAnalyzer<Double> {

    private final Map<Integer, Integer> counter = new HashMap<>();

    @Override
    public Double findMajorValue() {
        // find best
        int maxCnt = -1, maxVal = 0, val;
        for (Entry<Integer, Integer> e : counter.entrySet()) {
            val = e.getValue();
            if (val > maxCnt) {
                maxCnt = val;
                maxVal = e.getKey();
            }
        }

        return maxVal * precision;
    }

    @Override
    public void addValue(Double d) {
        final int val = (int) Math.round(d / precision);
        if (counter.containsKey(val)) {
            counter.put(val, counter.get(val) + 1);
        } else {
            counter.put(val, 1);
        }
    }

}
