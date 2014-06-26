package cz.tul.dic.generators;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Petr Jecmen
 */
public class DeformationGenerator {

    public static Map<ROI, double[]> generateDeformations(final TaskContainer tc, final int round) throws ComputationException {
        final Map<ROI, double[]> result = new HashMap<>();

        for (ROI roi : tc.getRois(round)) {
            result.put(roi, generateDeformations(tc.getDeformationLimits(round, roi)));
        }

        return result;
    }

    public static double[] generateDeformations(final double[] limits) throws ComputationException {
        final DeformationDegree degree = DeformationUtils.getDegreeFromLimits(limits);
        final double[] deformations;

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
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported degree of deformation - " + degree + ".");

        }
        return deformations;
    }

    private static double[] generateZeroDegree(final double[] limits) throws ComputationException {
        if (limits.length < 6) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Illegal number of deformation parameters - received " + limits.length + ", required 6.");
        }

        final int coeffCount = 2;
        int size = coeffCount;
        size *= computeSize(limits, 0);
        size *= computeSize(limits, 3);

        final double[] result = new double[size];

        int index;
        int i = 0;
        for (double u = limits[0]; u <= limits[1]; u += limits[2]) {
            for (double v = limits[3]; v <= limits[4]; v += limits[5]) {
                index = i * coeffCount;
                result[index] = u;
                result[index + 1] = v;
                i++;

                if (limits[5] == 0) {
                    break;
                }
            }
            if (limits[2] == 0) {
                break;
            }
        }

        return result;
    }

    private static int computeSize(final double[] limits, final int base) {
        final int result;
        if (limits[base + 2] != 0 && limits[base] != limits[base + 1]) {
            result = (int) ((limits[base + 1] - limits[base]) / limits[base + 2] + 1);
        } else {
            result = 1;
        }
        return result;
    }

    private static double[] generateFirstDegree(final double[] limits) throws ComputationException {
        if (limits.length < 18) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Illegal number of deformation parameters - received " + limits.length + ", required 18.");
        }

        final int coeffCount = 6;
        int size = coeffCount;
        size *= computeSize(limits, 0);
        size *= computeSize(limits, 3);
        size *= computeSize(limits, 6);
        size *= computeSize(limits, 9);
        size *= computeSize(limits, 12);
        size *= computeSize(limits, 15);

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

                                if (limits[17] == 0) {
                                    break;
                                }
                            }
                            if (limits[14] == 0) {
                                break;
                            }
                        }
                        if (limits[11] == 0) {
                            break;
                        }
                    }
                    if (limits[8] == 0) {
                        break;
                    }
                }
                if (limits[5] == 0) {
                    break;
                }
            }
            if (limits[2] == 0) {
                break;
            }
        }

        return result;
    }

    private static double[] generateSecondDegree(final double[] limits) throws ComputationException {
        if (limits.length < 36) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Illegal number of deformation parameters - received " + limits.length + ", required 36.");
        }

        final int coeffCount = 12;
        int size = coeffCount;
        size *= computeSize(limits, 0);
        size *= computeSize(limits, 3);
        size *= computeSize(limits, 6);
        size *= computeSize(limits, 9);
        size *= computeSize(limits, 12);
        size *= computeSize(limits, 15);
        size *= computeSize(limits, 18);
        size *= computeSize(limits, 21);
        size *= computeSize(limits, 24);
        size *= computeSize(limits, 27);
        size *= computeSize(limits, 30);
        size *= computeSize(limits, 33);

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

                                                        if (limits[35] == 0) {
                                                            break;
                                                        }
                                                    }
                                                    if (limits[32] == 0) {
                                                        break;
                                                    }
                                                }
                                                if (limits[29] == 0) {
                                                    break;
                                                }
                                            }
                                            if (limits[26] == 0) {
                                                break;
                                            }
                                        }
                                        if (limits[23] == 0) {
                                            break;
                                        }
                                    }
                                    if (limits[20] == 0) {
                                        break;
                                    }
                                }
                                if (limits[17] == 0) {
                                    break;
                                }
                            }
                            if (limits[14] == 0) {
                                break;
                            }
                        }
                        if (limits[11] == 0) {
                            break;
                        }
                    }
                    if (limits[8] == 0) {
                        break;
                    }
                }
                if (limits[5] == 0) {
                    break;
                }
            }
            if (limits[2] == 0) {
                break;
            }
        }

        return result;
    }

}
