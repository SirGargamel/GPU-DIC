package cz.tul.dic;

/**
 *
 * @author Petr Jecmen
 */
public interface Constants {

    double PRECISION_ZERO = 0.5;
    double PRECISION_FIRST = 0.25;
    double[] DEFORMATION_LIMITS_ZERO = new double[]{-1, 1, PRECISION_ZERO, -5, 5, PRECISION_ZERO};
    double[] DEFORMATION_LIMITS_FIRST = new double[]{
        -5, 5, PRECISION_ZERO, -5, 5, PRECISION_ZERO,
        -0.5, 0.5, PRECISION_FIRST, -0.5, 0.5, PRECISION_FIRST, -0.5, 0.5, PRECISION_FIRST, -0.5, 0.5, PRECISION_FIRST};
}
