package cz.tul.dic.engine.opencl.solvers;

import java.io.Serializable;

/**
 *
 * @author Petr Ječmen
 */
public class CorrelationResult implements Serializable {

    private final float value;
    private final double[] deformation;

    public float getValue() {
        return value;
    }

    public double[] getDeformation() {
        return deformation;
    }

    public CorrelationResult(float value, double[] deformation) {
        this.value = value;
        this.deformation = deformation;
    }

}
