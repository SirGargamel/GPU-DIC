package cz.tul.dic.generators;

import cz.tul.dic.DeformationDegree;
import cz.tul.dic.data.TaskContainer;
import cz.tul.dic.data.TaskParameter;

/**
 *
 * @author Petr Jecmen
 */
public class DeformationGenerator {

    private static final double[] DEFAULT_BOUNDS = new double[]{
        -10, 10, 0.25, -10, 10, 0.25,
        -0.5, 0.5, 0.1, -0.5, 0.5, 0.1, -0.5, 0.5, 0.1, -0.5, 0.5, 0.1,
        -0.1, 0.1, 0.025, -0.1, 0.1, 0.025, -0.1, 0.1, 0.025, -0.1, 0.1, 0.025, -0.1, 0.1, 0.025, -0.1, 0.1, 0.025};
    private static final DeformationDegree DEFAULT_BOUNDS_DEGREE = DeformationDegree.FIRST;

    public static void generateDeformations(final TaskContainer tc) {
        final double[] bounds;
        final DeformationDegree degree;

        final Object o = tc.getParameter(TaskParameter.DEFORMATION_BOUNDS);
        final Object o2 = tc.getParameter(TaskParameter.DEFORMATION_DEGREE);
        if (o == null && o2 != null) {
            bounds = DEFAULT_BOUNDS;
            tc.addParameter(TaskParameter.DEFORMATION_BOUNDS, bounds);

            degree = (DeformationDegree) (o2);
        } else if (o != null && o2 == null) {
            bounds = (double[]) o;

            degree = DEFAULT_BOUNDS_DEGREE;
            tc.addParameter(TaskParameter.DEFORMATION_DEGREE, DEFAULT_BOUNDS_DEGREE);
        } else if (o != null && o2 != null) {
            bounds = (double[]) o;
            degree = (DeformationDegree) (o2);
        } else {
            bounds = DEFAULT_BOUNDS;
            tc.addParameter(TaskParameter.DEFORMATION_BOUNDS, bounds);

            degree = DEFAULT_BOUNDS_DEGREE;
            tc.addParameter(TaskParameter.DEFORMATION_DEGREE, DEFAULT_BOUNDS_DEGREE);
        }

        final double[] result;
        switch (degree) {
            case ZERO:
                result = generateZeroDegree(bounds);
                break;
            case FIRST:
                result = generateFirstDegree(bounds);
                break;
            case SECOND:
                result = generateSecondDegree(bounds);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported degree of deformation - " + degree + ".");

        }

        tc.setDeformations(result);
    }

    private static double[] generateZeroDegree(final double[] bounds) {
        if (bounds.length < 6) {
            throw new IllegalArgumentException("Illegal number of deformation parameters - received " + bounds.length + ", required 6.");
        }

        final int coeffCount = 2;
        int size = coeffCount;
        size *= (bounds[1] - bounds[0]) / bounds[2] + 1;
        size *= (bounds[4] - bounds[3]) / bounds[5] + 1;

        final double[] result = new double[size];

        int index;
        int i = 0;
        for (double u = bounds[0]; u <= bounds[1]; u += bounds[2]) {
            for (double v = bounds[3]; v <= bounds[4]; v += bounds[5]) {
                index = i * coeffCount;
                result[index] = u;
                result[index + 1] = v;
                i++;
            }
        }

        return result;
    }

    private static double[] generateFirstDegree(final double[] bounds) {
        if (bounds.length < 18) {
            throw new IllegalArgumentException("Illegal number of deformation parameters - received " + bounds.length + ", required 18.");
        }

        final int coeffCount = 6;
        int size = coeffCount;
        size *= (bounds[1] - bounds[0]) / bounds[2] + 1;
        size *= (bounds[4] - bounds[3]) / bounds[5] + 1;
        size *= (bounds[7] - bounds[6]) / bounds[8] + 1;
        size *= (bounds[10] - bounds[9]) / bounds[11] + 1;
        size *= (bounds[13] - bounds[12]) / bounds[14] + 1;
        size *= (bounds[16] - bounds[15]) / bounds[17] + 1;

        final double[] result = new double[size];

        int index;
        int i = 0;
        for (double u = bounds[0]; u <= bounds[1]; u += bounds[2]) {
            for (double v = bounds[3]; v <= bounds[4]; v += bounds[5]) {
                for (double ux = bounds[6]; ux <= bounds[7]; ux += bounds[8]) {
                    for (double uy = bounds[9]; uy <= bounds[10]; uy += bounds[11]) {
                        for (double vx = bounds[12]; vx <= bounds[13]; vx += bounds[14]) {
                            for (double vy = bounds[15]; vy <= bounds[16]; vy += bounds[17]) {
                                index = i * coeffCount;
                                result[index] = u;
                                result[index + 1] = v;
                                result[index + 2] = ux;
                                result[index + 3] = uy;
                                result[index + 4] = vx;
                                result[index + 5] = vy;
                                i++;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private static double[] generateSecondDegree(final double[] bounds) {
        if (bounds.length < 36) {
            throw new IllegalArgumentException("Illegal number of deformation parameters - received " + bounds.length + ", required 36.");
        }

        final int coeffCount = 12;
        int size = coeffCount;
        size *= (bounds[1] - bounds[0]) / bounds[2] + 1;
        size *= (bounds[4] - bounds[3]) / bounds[5] + 1;
        size *= (bounds[7] - bounds[6]) / bounds[8] + 1;
        size *= (bounds[10] - bounds[9]) / bounds[11] + 1;
        size *= (bounds[13] - bounds[12]) / bounds[14] + 1;
        size *= (bounds[16] - bounds[15]) / bounds[17] + 1;
        size *= (bounds[19] - bounds[18]) / bounds[20] + 1;
        size *= (bounds[22] - bounds[21]) / bounds[23] + 1;
        size *= (bounds[25] - bounds[24]) / bounds[26] + 1;
        size *= (bounds[28] - bounds[27]) / bounds[29] + 1;
        size *= (bounds[31] - bounds[30]) / bounds[32] + 1;
        size *= (bounds[34] - bounds[33]) / bounds[35] + 1;

        final double[] result = new double[size];

        int index;
        int i = 0;
        for (double u = bounds[0]; u <= bounds[1]; u += bounds[2]) {
            for (double v = bounds[3]; v <= bounds[4]; v += bounds[5]) {
                for (double ux = bounds[6]; ux <= bounds[7]; ux += bounds[8]) {
                    for (double uy = bounds[9]; uy <= bounds[10]; uy += bounds[11]) {
                        for (double vx = bounds[12]; vx <= bounds[13]; vx += bounds[14]) {
                            for (double vy = bounds[15]; vy <= bounds[16]; vy += bounds[17]) {
                                for (double uxx = bounds[18]; uxx <= bounds[19]; uxx += bounds[20]) {
                                    for (double uxy = bounds[21]; uxy <= bounds[22]; uxy += bounds[23]) {
                                        for (double uyy = bounds[24]; uyy <= bounds[25]; uyy += bounds[26]) {
                                            for (double vxx = bounds[27]; vxx <= bounds[28]; vxx += bounds[29]) {
                                                for (double vxy = bounds[30]; vxy <= bounds[31]; vxy += bounds[32]) {
                                                    for (double vyy = bounds[33]; vyy <= bounds[34]; vyy += bounds[35]) {
                                                        index = i * coeffCount;
                                                        result[index] = u;
                                                        result[index + 1] = v;
                                                        result[index + 2] = ux;
                                                        result[index + 3] = uy;
                                                        result[index + 4] = vx;
                                                        result[index + 5] = vy;
                                                        result[index + 6] = uxx;
                                                        result[index + 7] = uxy;
                                                        result[index + 8] = uyy;
                                                        result[index + 9] = vxx;
                                                        result[index + 10] = vxy;
                                                        result[index + 11] = vyy;
                                                        i++;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

}
