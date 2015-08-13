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

    public static final int E_XX = 0;
    public static final int E_YY = 1;
    public static final int E_XY = 2;
    private final double[][][] strain;
    private final double[][] qualityX, qualityY;

    public StrainResult(final double[][][] strain, final double[][] qualityX, final double[][] qualityY) {
        this.strain = strain;
        this.qualityX = qualityX;
        this.qualityY = qualityY;
    }

    public double[][][] getStrain() {
        return strain;
    }

    public double[][] getQualityX() {
        return qualityX;
    }

    public double[][] getQualityY() {
        return qualityY;
    }

}
