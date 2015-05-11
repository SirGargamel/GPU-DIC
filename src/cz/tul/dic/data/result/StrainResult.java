/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.result;

import java.io.Serializable;

/**
 *
 * @author Petr Ječmen
 */
public class StrainResult implements Serializable {

    private final double[][][] strain;
    private final double[][][] quality;

    public StrainResult(final double[][][] strain, final double[][][] quality) {
        this.strain = strain;
        this.quality = quality;
    }

    public double[][][] getStrain() {
        return strain;
    }

    public double[][][] getQuality() {
        return quality;
    }

}
