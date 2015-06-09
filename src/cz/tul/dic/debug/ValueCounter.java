/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.debug;

import cz.tul.dic.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Petr Ječmen
 */
public class ValueCounter {

    private static final String SEPARATOR = ", ";
    private final Map<String, Integer> counter;
    private boolean enabled;

    private ValueCounter() {
        counter = new HashMap<>();
        enabled = DebugControl.isDebugMode();
    }

    public static ValueCounter createCounter() {
        final ValueCounter rc = new ValueCounter();
        DebugControl.addCounter(rc);
        rc.setEnabled(DebugControl.isDebugMode());
        return rc;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void inc(final double[] val) {
        if (enabled) {
            inc(toString(val));
        }
    }

    private String toString(final double[] val) {
        final StringBuilder sb = new StringBuilder("[");
        for (double d : val) {
            sb.append(Utils.format(d));
            sb.append(SEPARATOR);
        }
        sb.setLength(sb.length() - SEPARATOR.length());
        sb.append("]");
        return sb.toString();
    }

    public void inc(final double val) {
        if (enabled) {
            inc(Utils.format(val));
        }
    }

    public void inc(final float val) {
        if (enabled) {
            inc(Utils.format(val));
        }
    }

    public void inc() {
        if (enabled) {
            inc("NULL");
        }
    }

    private void inc(final String key) {
        if (counter.containsKey(key)) {
            counter.put(key, counter.get(key) + 1);
        } else {
            counter.put(key, 1);
        }
    }

    @Override
    public String toString() {
        List<Map.Entry> a = new ArrayList<>(counter.entrySet());
        Collections.sort(a,
                (Map.Entry o1, Map.Entry o2)
                -> ((Comparable) o1.getValue()).compareTo(o2.getValue())
        );

        final StringBuilder sb = new StringBuilder();
        a.stream().forEach(e
                -> sb.append("\n")
                .append(e.getKey())
                .append(" -- ")
                .append(e.getValue())
        );
        return sb.toString();
    }

    public void reset() {
        counter.clear();
    }

}
