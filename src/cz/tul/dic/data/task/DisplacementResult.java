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
