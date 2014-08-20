package cz.tul.dic.engine;

/**
 *
 * @author Petr Ječmen
 */
public class CorrelationResult {

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
