package cz.tul.dic.generators;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.deformation.Deformation;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationLimit;
import cz.tul.dic.data.deformation.DeformationUtils;

/**
 *
 * @author Petr Jecmen
 */
public class DeformationGenerator {

    public static double[] generateDeformations(final double[] limits, final long... fromTo) throws ComputationException {
        final DeformationDegree degree = DeformationUtils.getDegreeFromLimits(limits);
        final double[] deformations;

        final long from, to;
        if (fromTo.length > 1) {
            from = fromTo[0];
            to = fromTo[1];
        } else {
            from = 0;
            to = (int) Math.min(Integer.MAX_VALUE, calculateDeformationArraySize(limits));
        }

        switch (degree) {
            case ZERO:
                deformations = generateZeroDegree(limits, from, to);
                break;
            case FIRST:
                deformations = generateFirstDegree(limits, from, to);
                break;
            case SECOND:
                deformations = generateSecondDegree(limits, from, to);
                break;
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported degree of deformation - " + degree + ".");

        }
        return deformations;
    }

    private static double[] generateZeroDegree(final double[] limits, final long from, final long to) throws ComputationException {
        if (limits.length < 6) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Illegal number of deformation parameters - received " + limits.length + ", required at least 6.");
        }

        final int coeffCount = 2;
        final long size = Math.min(calculateDeformationArraySize(limits), to - from);
        if (size > Integer.MAX_VALUE) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Too many deformations requested - " + size);
        }
        final double[] result = new double[(int) size];

        int index;
        int i = 0;
        long counter = 0;
        loop:
        for (double u = limits[DeformationLimit.UMIN]; u <= limits[DeformationLimit.UMAX]; u += limits[DeformationLimit.USTEP]) {
            for (double v = limits[DeformationLimit.VMIN]; v <= limits[DeformationLimit.VMAX]; v += limits[DeformationLimit.VSTEP]) {
                index = i * coeffCount;
                if (index >= result.length) {
                    break loop;
                }

                result[index + Deformation.U] = u;
                result[index + Deformation.V] = v;
                counter += coeffCount;
                if (counter >= from) {
                    i++;
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

    private static int computeSize(final double[] limits, final int base) {
        final int result;
        if (limits[base + 2] != 0 && limits[base] != limits[base + 1]) {
            result = (int) ((limits[base + 1] - limits[base]) / limits[base + 2] + 1);
        } else {
            result = 1;
        }
        return result;
    }

    private static double[] generateFirstDegree(final double[] limits, final long from, final long to) throws ComputationException {
        if (limits.length < 18) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Illegal number of deformation parameters - received " + limits.length + ", required 18.");
        }

        final int coeffCount = 6;
        final long size = Math.min(calculateDeformationArraySize(limits), to - from);
        if (size > Integer.MAX_VALUE) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Too many deformations requested - " + size);
        }
        final double[] result = new double[(int) size];

        int index;
        int i = 0;
        long counter = 0;
        loop:
        for (double u = limits[DeformationLimit.UMIN]; u <= limits[DeformationLimit.UMAX]; u += limits[DeformationLimit.USTEP]) {
            for (double v = limits[DeformationLimit.VMIN]; v <= limits[DeformationLimit.VMAX]; v += limits[DeformationLimit.VSTEP]) {
                for (double ux = limits[DeformationLimit.UXMIN]; ux <= limits[DeformationLimit.UXMAX]; ux += limits[DeformationLimit.UXSTEP]) {
                    for (double uy = limits[DeformationLimit.UYMIN]; uy <= limits[DeformationLimit.UYMAX]; uy += limits[DeformationLimit.UYSTEP]) {
                        for (double vx = limits[DeformationLimit.VXMIN]; vx <= limits[DeformationLimit.VXMAX]; vx += limits[DeformationLimit.VXSTEP]) {
                            for (double vy = limits[DeformationLimit.VYMIN]; vy <= limits[DeformationLimit.VYMAX]; vy += limits[DeformationLimit.VYSTEP]) {
                                index = i * coeffCount;
                                if (index >= result.length) {
                                    break loop;
                                }

                                result[index + Deformation.U] = u;
                                result[index + Deformation.V] = v;
                                result[index + Deformation.UX] = ux;
                                result[index + Deformation.UY] = uy;
                                result[index + Deformation.VX] = vx;
                                result[index + Deformation.VY] = vy;
                                counter += coeffCount;
                                if (counter >= from) {
                                    i++;
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

    private static double[] generateSecondDegree(final double[] limits, final long from, final long to) throws ComputationException {
        if (limits.length < 36) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Illegal number of deformation parameters - received " + limits.length + ", required 36.");
        }

        final int coeffCount = 12;
        final long size = Math.min(calculateDeformationArraySize(limits), to - from);
        if (size > Integer.MAX_VALUE) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Too many deformations requested - " + size);
        }
        final double[] result = new double[(int) size];        

        int index;
        int i = 0;
        long counter = 0;
        loop:
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
                                                        if (index >= result.length) {
                                                            break loop;
                                                        }

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
                                                        counter += coeffCount;
                                                        if (counter >= from) {
                                                            i++;
                                                        }

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

    public static long calculateDeformationArraySize(final double[] limits) {
        final int coeffCount = limits.length / 3;
        long size = coeffCount;
        size *= computeSize(limits, 0);
        size *= computeSize(limits, 3);
        if (coeffCount > 2) {
            size *= computeSize(limits, 6);
            size *= computeSize(limits, 9);
            size *= computeSize(limits, 12);
            size *= computeSize(limits, 15);
            if (coeffCount > 6) {
                size *= computeSize(limits, 18);
                size *= computeSize(limits, 21);
                size *= computeSize(limits, 24);
                size *= computeSize(limits, 27);
                size *= computeSize(limits, 30);
                size *= computeSize(limits, 33);
            }
        }
        return size;
    }

}
