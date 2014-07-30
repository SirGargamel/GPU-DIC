package cz.tul.dic.generators;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.deformation.Deformation;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationLimit;
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
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Illegal number of deformation parameters - received " + limits.length + ", required at least 6.");
        }

        final int coeffCount = 2;
        int size = coeffCount;
        size *= computeSize(limits, 0);
        size *= computeSize(limits, 3);

        final double[] result = new double[size];

        int index;
        int i = 0;
        for (double u = limits[DeformationLimit.UMIN]; u <= limits[DeformationLimit.UMAX]; u += limits[DeformationLimit.USTEP]) {
            for (double v = limits[DeformationLimit.VMIN]; v <= limits[DeformationLimit.VMAX]; v += limits[DeformationLimit.VSTEP]) {
                index = i * coeffCount;
                result[index + Deformation.U] = u;
                result[index + Deformation.V] = v;
                i++;

                if (limits[DeformationLimit.VSTEP] == 0) {
                    break;
                }
            }
            if (limits[DeformationLimit.USTEP] == 0) {
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
        for (double u = limits[DeformationLimit.UMIN]; u <= limits[DeformationLimit.UMAX]; u += limits[DeformationLimit.USTEP]) {
            for (double v = limits[DeformationLimit.VMIN]; v <= limits[DeformationLimit.VMAX]; v += limits[DeformationLimit.VSTEP]) {
                for (double ux = limits[DeformationLimit.UXMIN]; ux <= limits[DeformationLimit.UXMAX]; ux += limits[DeformationLimit.UXSTEP]) {
                    for (double uy = limits[DeformationLimit.UYMIN]; uy <= limits[DeformationLimit.UYMAX]; uy += limits[DeformationLimit.UYSTEP]) {
                        for (double vx = limits[DeformationLimit.VXMIN]; vx <= limits[DeformationLimit.VXMAX]; vx += limits[DeformationLimit.VXSTEP]) {
                            for (double vy = limits[DeformationLimit.VYMIN]; vy <= limits[DeformationLimit.VYMAX]; vy += limits[DeformationLimit.VYSTEP]) {
                                index = i * coeffCount;
                                result[index + Deformation.U] = u;
                                result[index + Deformation.V] = v;
                                result[index + Deformation.UX] = ux;
                                result[index + Deformation.UY] = uy;
                                result[index + Deformation.VX] = vx;
                                result[index + Deformation.VY] = vy;
                                i++;

                                if (limits[DeformationLimit.VYSTEP] == 0) {
                                    break;
                                }
                            }
                            if (limits[DeformationLimit.VXSTEP] == 0) {
                                break;
                            }
                        }
                        if (limits[DeformationLimit.UYSTEP] == 0) {
                            break;
                        }
                    }
                    if (limits[DeformationLimit.UXSTEP] == 0) {
                        break;
                    }
                }
                if (limits[DeformationLimit.VSTEP] == 0) {
                    break;
                }
            }
            if (limits[DeformationLimit.USTEP] == 0) {
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
        for (double u = limits[DeformationLimit.UMIN]; u <= limits[DeformationLimit.UMAX]; u += limits[DeformationLimit.USTEP]) {
            for (double v = limits[DeformationLimit.VMIN]; v <= limits[DeformationLimit.VMAX]; v += limits[DeformationLimit.VSTEP]) {
                for (double ux = limits[DeformationLimit.UXMIN]; ux <= limits[DeformationLimit.UXMAX]; ux += limits[DeformationLimit.UXSTEP]) {
                    for (double uy = limits[DeformationLimit.UYMIN]; uy <= limits[DeformationLimit.UYMAX]; uy += limits[DeformationLimit.UYSTEP]) {
                        for (double vx = limits[DeformationLimit.VXMIN]; vx <= limits[DeformationLimit.VXMAX]; vx += limits[DeformationLimit.VXSTEP]) {
                            for (double vy = limits[DeformationLimit.VYMIN]; vy <= limits[DeformationLimit.VYMAX]; vy += limits[DeformationLimit.VYSTEP]) {
                                for (double uxx = limits[DeformationLimit.UXXMIN]; uxx <= limits[DeformationLimit.UXXMAX]; uxx += limits[DeformationLimit.UXXSTEP]) {
                                    for (double uyy = limits[DeformationLimit.UYYMIN]; uyy <= limits[DeformationLimit.UYYMAX]; uyy += limits[DeformationLimit.UYYSTEP]) {
                                        for (double uxy = limits[DeformationLimit.UXYMIN]; uxy <= limits[DeformationLimit.UXYMAX]; uxy += limits[DeformationLimit.UXYSTEP]) {
                                            for (double vxx = limits[DeformationLimit.VXXMIN]; vxx <= limits[DeformationLimit.VXXMAX]; vxx += limits[DeformationLimit.VXXSTEP]) {
                                                for (double vyy = limits[DeformationLimit.VYYMIN]; vyy <= limits[DeformationLimit.VYYMAX]; vyy += limits[DeformationLimit.VYYSTEP]) {
                                                    for (double vxy = limits[DeformationLimit.VXYMIN]; vxy <= limits[DeformationLimit.VXYMAX]; vxy += limits[DeformationLimit.VXYSTEP]) {
                                                        index = i * coeffCount;
                                                        result[index + Deformation.U] = u;
                                                        result[index + Deformation.V] = v;
                                                        result[index + Deformation.UX] = ux;
                                                        result[index + Deformation.UY] = uy;
                                                        result[index + Deformation.VX] = vx;
                                                        result[index + Deformation.VY] = vy;
                                                        result[index + Deformation.UXX] = uxx;
                                                        result[index + Deformation.UYY] = uyy;
                                                        result[index + Deformation.UXY] = uxy;
                                                        result[index + Deformation.VXX] = vxx;
                                                        result[index + Deformation.VYY] = vyy;
                                                        result[index + Deformation.VXY] = vxy;
                                                        i++;

                                                        if (limits[DeformationLimit.VXYSTEP] == 0) {
                                                            break;
                                                        }
                                                    }
                                                    if (limits[DeformationLimit.VYYSTEP] == 0) {
                                                        break;
                                                    }
                                                }
                                                if (limits[DeformationLimit.VXXSTEP] == 0) {
                                                    break;
                                                }
                                            }
                                            if (limits[DeformationLimit.UXYSTEP] == 0) {
                                                break;
                                            }
                                        }
                                        if (limits[DeformationLimit.UYYSTEP] == 0) {
                                            break;
                                        }
                                    }
                                    if (limits[DeformationLimit.UXXSTEP] == 0) {
                                        break;
                                    }
                                }
                                if (limits[DeformationLimit.VYSTEP] == 0) {
                                    break;
                                }
                            }
                            if (limits[DeformationLimit.VXSTEP] == 0) {
                                break;
                            }
                        }
                        if (limits[DeformationLimit.UYSTEP] == 0) {
                            break;
                        }
                    }
                    if (limits[DeformationLimit.UXSTEP] == 0) {
                        break;
                    }
                }
                if (limits[DeformationLimit.VSTEP] == 0) {
                    break;
                }
            }
            if (limits[DeformationLimit.USTEP] == 0) {
                break;
            }
        }

        return result;
    }

}
