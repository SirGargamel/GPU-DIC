/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task;

import java.io.Serializable;

/**
 *
 * @author Petr Ječmen
 */
public class DisplacementResult implements Serializable {

    private final double[][][] deformation;
    private final double[][] quality;

    public DisplacementResult(double[][][] deformation, double[][] quality) {
        this.deformation = deformation;
        this.quality = quality;
    }

    public double[][][] getDisplacement() {
        return deformation;
    }

    public double[][] getQuality() {
        return quality;
    }

}
