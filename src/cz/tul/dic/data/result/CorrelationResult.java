/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.result;

import java.io.Serializable;
import java.util.Arrays;

/**
 *
 * @author Petr Ječmen
 */
public class CorrelationResult implements Serializable {

    private final double quality;
    private final double[] deformation;

    public CorrelationResult(final double value, final double[] deformation) {
        this.quality = value;
        this.deformation = deformation;
    }
    
    public double getQuality() {
        return quality;
    }

    public double[] getDeformation() {
        return deformation;
    }    

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(Arrays.toString(deformation));
        sb.append(": ");
        sb.append(quality);
        return sb.toString();
    }

}
