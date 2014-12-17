package cz.tul.dic.engine.opencl.solvers;

import java.io.Serializable;
import java.util.Arrays;

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
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(Arrays.toString(deformation));
        sb.append(": ");
        sb.append(value);
        return sb.toString();
    }

}
