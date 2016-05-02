/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.subset;

import cz.tul.dic.debug.converters.SubsetConverter;
import cz.tul.pj.journal.Journal;
import java.io.Serializable;

/**
 *
 * @author Lenam s.r.o.
 */
public abstract class AbstractSubset implements Serializable {

    private final double[] center;
    private final int size;
    private final int[] data;
    
    static {
        Journal.registerConverter(new SubsetConverter());
    }

    public AbstractSubset(final double[] center, final int size, final int[] data) {
        this.center = new double[center.length];
        System.arraycopy(center, 0, this.center, 0, center.length);
        this.size = size;
        this.data = new int[data.length];
        System.arraycopy(data, 0, this.data, 0, data.length);
    }

    public int[] getData() {
        return data;
    }

    public double[] getCenter() {
        return center;
    }

    public int getSize() {
        return size;
    }

}
