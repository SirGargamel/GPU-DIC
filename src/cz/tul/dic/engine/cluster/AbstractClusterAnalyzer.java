/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.cluster;

import java.util.List;

/**
 *
 * @author Petr Jecmen
 * @param <T>
 */
public abstract class AbstractClusterAnalyzer<T> {

    private static final double DEFAULT_PRECISION = 0.1;
    protected double precision;
    protected List<T> values;

    public AbstractClusterAnalyzer() {
        precision = DEFAULT_PRECISION;
    }

    public void addValue(final T val) {
        values.add(val);
    }

    public List<T> listValues() {
        return values;
    }

    public void setPrecision(final double precision) {
        this.precision = precision;
    }

    public double getPrecision() {
        return precision;
    }

    public abstract T findMajorValue();

}
