package cz.tul.dic.generators;

import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import java.util.Set;

/**
 *
 * @author Petr Jecmen
 */
public class DeformationGenerator {

    public static void generateDeformations(final TaskContainer tc) {
        double[] limits, deformations;
        DeformationDegree degree;
        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        Set<ROI> rois, oldRois = null;
        for (int round = 0; round < roundCount; round++) {
            rois = tc.getRoi(round);
            if (rois != oldRois) {
                for (ROI roi : rois) {
                    limits = tc.getDeformationLimits(round, roi);
                    degree = DeformationUtils.getDegreeFromLimits(limits);

                    switch (degree) {
                        case ZERO:
                            deformations = generateZeroDegree(limits);
                            break;
                        case FIRST:
                            deformations = generateFirstDegree(limits);
                            break;
                        case SECOND:
                            deformations = generateSecondDegree(limits);
                            break;
                        default:
                            throw new UnsupportedOperationException("Unsupported degree of deformation - " + degree + ".");

                    }
                    tc.setDeformations(deformations, round, roi);
                }
            }
            oldRois = rois;
        }
    }

    private static double[] generateZeroDegree(final double[] limits) {
        if (limits.length < 6) {
            throw new IllegalArgumentException("Illegal number of deformation parameters - received " + limits.length + ", required 6.");
        }

        final int coeffCount = 2;
        int size = coeffCount;
        size *= (limits[1] - limits[0]) / limits[2] + 1;
        size *= (limits[4] - limits[3]) / limits[5] + 1;

        final double[] result = new double[size];

        int index;
        int i = 0;
        for (double u = limits[0]; u <= limits[1]; u += limits[2]) {
            for (double v = limits[3]; v <= limits[4]; v += limits[5]) {
                index = i * coeffCount;
                result[index] = u;
                result[index + 1] = v;
                i++;
            }
        }

        return result;
    }

    private static double[] generateFirstDegree(final double[] limits) {
        if (limits.length < 18) {
            throw new IllegalArgumentException("Illegal number of deformation parameters - received " + limits.length + ", required 18.");
        }

        final int coeffCount = 6;
        int size = coeffCount;
        size *= (limits[1] - limits[0]) / limits[2] + 1;
        size *= (limits[4] - limits[3]) / limits[5] + 1;
        size *= (limits[7] - limits[6]) / limits[8] + 1;
        size *= (limits[10] - limits[9]) / limits[11] + 1;
        size *= (limits[13] - limits[12]) / limits[14] + 1;
        size *= (limits[16] - limits[15]) / limits[17] + 1;

        final double[] result = new double[size];

        int index;
        int i = 0;
        for (double u = limits[0]; u <= limits[1]; u += limits[2]) {
            for (double v = limits[3]; v <= limits[4]; v += limits[5]) {
                for (double ux = limits[6]; ux <= limits[7]; ux += limits[8]) {
                    for (double uy = limits[9]; uy <= limits[10]; uy += limits[11]) {
                        for (double vx = limits[12]; vx <= limits[13]; vx += limits[14]) {
                            for (double vy = limits[15]; vy <= limits[16]; vy += limits[17]) {
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

    private static double[] generateSecondDegree(final double[] limits) {
        if (limits.length < 36) {
            throw new IllegalArgumentException("Illegal number of deformation parameters - received " + limits.length + ", required 36.");
        }

        final int coeffCount = 12;
        int size = coeffCount;
        size *= (limits[1] - limits[0]) / limits[2] + 1;
        size *= (limits[4] - limits[3]) / limits[5] + 1;
        size *= (limits[7] - limits[6]) / limits[8] + 1;
        size *= (limits[10] - limits[9]) / limits[11] + 1;
        size *= (limits[13] - limits[12]) / limits[14] + 1;
        size *= (limits[16] - limits[15]) / limits[17] + 1;
        size *= (limits[19] - limits[18]) / limits[20] + 1;
        size *= (limits[22] - limits[21]) / limits[23] + 1;
        size *= (limits[25] - limits[24]) / limits[26] + 1;
        size *= (limits[28] - limits[27]) / limits[29] + 1;
        size *= (limits[31] - limits[30]) / limits[32] + 1;
        size *= (limits[34] - limits[33]) / limits[35] + 1;

        final double[] result = new double[size];

        int index;
        int i = 0;
        for (double u = limits[0]; u <= limits[1]; u += limits[2]) {
            for (double v = limits[3]; v <= limits[4]; v += limits[5]) {
                for (double ux = limits[6]; ux <= limits[7]; ux += limits[8]) {
                    for (double uy = limits[9]; uy <= limits[10]; uy += limits[11]) {
                        for (double vx = limits[12]; vx <= limits[13]; vx += limits[14]) {
                            for (double vy = limits[15]; vy <= limits[16]; vy += limits[17]) {
                                for (double uxx = limits[18]; uxx <= limits[19]; uxx += limits[20]) {
                                    for (double uxy = limits[21]; uxy <= limits[22]; uxy += limits[23]) {
                                        for (double uyy = limits[24]; uyy <= limits[25]; uyy += limits[26]) {
                                            for (double vxx = limits[27]; vxx <= limits[28]; vxx += limits[29]) {
                                                for (double vxy = limits[30]; vxy <= limits[31]; vxy += limits[32]) {
                                                    for (double vyy = limits[33]; vyy <= limits[34]; vyy += limits[35]) {
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
