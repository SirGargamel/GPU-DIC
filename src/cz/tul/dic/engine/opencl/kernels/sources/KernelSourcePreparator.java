/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernels.sources;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.deformation.DeformationDirection;
import cz.tul.dic.data.Interpolation;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class KernelSourcePreparator {

    private static final String KERNEL_BASE_FILE = "kernel-base";
    private static final String REPLACE_CORRELATION_COMPUTE = "%CORR-C%";
    private static final String REPLACE_CORRELATION_MEAN = "%CORR-M%";
    private static final String REPLACE_EXTENSION = ".source";
    private static final String REPLACE_SUBSET_SIZE = "%SS%";
    private static final String REPLACE_DEFORMATION_DEFORM = "%DEF-S%";
    private static final String REPLACE_DEFORMATION_X = "%DEF_X%";
    private static final String REPLACE_DEFORMATION_Y = "%DEF_Y%";
    private static final String REPLACE_DEFORMATION_DEGREE = "%DEF_D%";
    private static final String REPLACE_DEFORMATION_COEFFS = "%DEF-C%";
    private static final String REPLACE_HEADER = "%HEAD%";
    private static final String REPLACE_INIT = "%INIT%";
    private static final String REPLACE_INTERPOLATION = "%INT%";
    private static final String REPLACE_STORE = "%S%";
    private static final String TEXT_DEFORMATION_ARRAY = "deformation[";
    private static final String PLUS = " + ";
    private static final String MUL = " * ";
    private String kernel;

    private KernelSourcePreparator() {
    }

    public static String prepareKernel(
            final int subsetSize, final DeformationDegree deg,
            final boolean is2D, final boolean usesVectorization, final Interpolation interpolation,
            final boolean usesImage, final boolean usesLocalMemory, final boolean usesMemoryCoalescing,
            final boolean subsetsGroupped, final boolean usesZNCC) throws ComputationException {
        final KernelSourcePreparator kp = new KernelSourcePreparator();

        try {
            kp.loadKernel();
            kp.prepareInterpolation(interpolation, usesImage);
            kp.prepareFunctionHeader(usesImage, usesVectorization, subsetsGroupped);
            kp.prepareInit(is2D, usesLocalMemory, usesMemoryCoalescing);
            kp.prepareCorrelation(usesVectorization, usesImage, usesZNCC);
            kp.prepareStore();
            kp.prepareDeformations(deg, usesVectorization, usesLocalMemory);
            kp.prepareSubsetSize(subsetSize);
            return kp.kernel;
        } catch (IOException ex) {
            throw new ComputationException(ComputationExceptionCause.IO, ex);
        }
    }

    private void loadKernel() throws IOException {
        kernel = loadKernelResource(KERNEL_BASE_FILE);
    }

    private void prepareInterpolation(final Interpolation interpolation, final boolean usesImage) {
        String resourceName = "interpolate-";
        switch (interpolation) {
            case BILINEAR:
                resourceName = resourceName.concat("bilinear");
                break;
            case BICUBIC:
                resourceName = resourceName.concat("bicubic");
                break;
            default:
                throw new IllegalArgumentException("Unsupported type of interpolation - " + interpolation);
        }
        if (usesImage) {
            resourceName = resourceName.concat("-image");
        } else {
            resourceName = resourceName.concat("-array");
        }

        kernel = kernel.replaceAll(
                REPLACE_INTERPOLATION,
                loadKernelResource(resourceName));
    }

    private void prepareFunctionHeader(final boolean usesImage, final boolean usesVectorization, final boolean subsetsGroupped) {
        final StringBuilder sb = new StringBuilder();
        String resourceName = "header-image-";
        if (usesImage) {
            resourceName = resourceName.concat("2Dt");
        } else {
            resourceName = resourceName.concat("array");
        }
        sb.append(loadKernelResource(resourceName));
        resourceName = "header-subsets";
        if (usesVectorization) {
            resourceName = resourceName.concat("-vec");
        }
        sb.append(loadKernelResource(resourceName));
        resourceName = "header-end";
        sb.append(loadKernelResource(resourceName));
        if (subsetsGroupped) {
            resourceName = resourceName.concat("-subsets-groupped");
        } else {
            resourceName = resourceName.concat("-subsets-alone");
        }
        sb.append(loadKernelResource(resourceName));
        kernel = kernel.replaceAll(REPLACE_HEADER, sb.toString());
    }

    private void prepareInit(final boolean is2D, final boolean usesLocalMemory, final boolean usesMemoryCoalescing) {
        String resourceName = "init-";
        if (is2D) {
            resourceName = resourceName.concat("2D-");
        } else {
            resourceName = resourceName.concat("1D-");
        }
        if (usesLocalMemory) {
            resourceName = resourceName.concat("local");
        } else {
            resourceName = resourceName.concat("global");
        }
        if (usesMemoryCoalescing) {
            resourceName = resourceName.concat("-MC");
        }
        kernel = kernel.replaceAll(REPLACE_INIT, loadKernelResource(resourceName));
    }

    private void prepareDeformations(final DeformationDegree deg, final boolean usesVectorization, final boolean usesLocalMemory) {
        final StringBuilder sb = new StringBuilder();
        String resourceName = "deformation-coeffs-";
        switch (deg) {
            case ZERO:
                resourceName = resourceName.concat("zero");
                break;
            case FIRST:
                resourceName = resourceName.concat("first");
                break;
            case SECOND:
                resourceName = resourceName.concat("second");
                break;
            default:
                throw new IllegalArgumentException("Unsupported degree of deformation - " + deg);
        }
        sb.append(loadKernelResource(resourceName));
        kernel = kernel.replaceAll(REPLACE_DEFORMATION_COEFFS, sb.toString());

        resourceName = "deformation-compute-";
        if (usesLocalMemory) {
            resourceName = resourceName.concat("local");
        } else {
            resourceName = resourceName.concat("global");
        }
        kernel = kernel.replaceAll(REPLACE_DEFORMATION_DEFORM, loadKernelResource(resourceName));

        final String x, y, dx, dy;
        if (usesVectorization) {
            x = "coords.x";
            y = "coords.y";
            dx = "def.x";
            dy = "def.y";
        } else {
            x = "x";
            y = "y";
            dx = "dx";
            dy = "dy";
        }
        switch (deg) {
            case ZERO:
                appendZeroOrderDeformation(x, y);
                break;
            case FIRST:
                appendFirstOrderDeformation(x, y, dx, dy);
                break;
            case SECOND:
                appendSecondOrderDeformation(x, y, dx, dy);
                break;
            default:
                throw new IllegalArgumentException("Unsupported degree of deformation - " + deg);
        }

        kernel = kernel.replaceAll(REPLACE_DEFORMATION_DEGREE, Integer.toString(DeformationUtils.getDeformationCoeffCount(deg)));
    }

    private void appendZeroOrderDeformation(final String x, final String y) {
        // coeff computation
        final StringBuilder sb = new StringBuilder();
        sb.append(x);
        sb.append(PLUS);
        sb.append(TEXT_DEFORMATION_ARRAY)
                .append(DeformationDirection.U)
                .append("]");
        kernel = kernel.replaceFirst(REPLACE_DEFORMATION_X, sb.toString());

        sb.setLength(0);
        sb.append(y);
        sb.append(PLUS);
        sb.append(TEXT_DEFORMATION_ARRAY)
                .append(DeformationDirection.V)
                .append("]");
        kernel = kernel.replaceFirst(REPLACE_DEFORMATION_Y, sb.toString());
    }

    private void appendFirstOrderDeformation(final String x, final String y, final String dx, final String dy) {
        // coeff computation
        final StringBuilder sb = new StringBuilder();
        sb.append(x);
        sb.append(PLUS);
        sb.append(TEXT_DEFORMATION_ARRAY)
                .append(DeformationDirection.U)
                .append("]");
        sb.append(PLUS);
        sb.append(TEXT_DEFORMATION_ARRAY)
                .append(DeformationDirection.UX)
                .append("]");
        sb.append(MUL);
        sb.append(dx);
        sb.append(PLUS);
        sb.append(TEXT_DEFORMATION_ARRAY)
                .append(DeformationDirection.UY)
                .append("]");
        sb.append(MUL);
        sb.append(dy);
        kernel = kernel.replaceFirst(REPLACE_DEFORMATION_X, sb.toString());

        sb.setLength(0);
        sb.append(y);
        sb.append(PLUS);
        sb.append(TEXT_DEFORMATION_ARRAY)
                .append(DeformationDirection.V)
                .append("]");
        sb.append(PLUS);
        sb.append(TEXT_DEFORMATION_ARRAY)
                .append(DeformationDirection.VX)
                .append("]");
        sb.append(MUL);
        sb.append(dx);
        sb.append(PLUS);
        sb.append(TEXT_DEFORMATION_ARRAY)
                .append(DeformationDirection.VY)
                .append("]");
        sb.append(MUL);
        sb.append(dy);
        kernel = kernel.replaceFirst(REPLACE_DEFORMATION_Y, sb.toString());
    }

    private void appendSecondOrderDeformation(final String x, final String y, final String dx, final String dy) {
        // coeff computation
        final StringBuilder sb = new StringBuilder();
        sb.append(x);
        sb.append(PLUS);
        sb.append(TEXT_DEFORMATION_ARRAY)
                .append(DeformationDirection.U)
                .append("]");
        sb.append(PLUS);
        sb.append(TEXT_DEFORMATION_ARRAY)
                .append(DeformationDirection.UX)
                .append("]");
        sb.append(MUL);
        sb.append(dx);
        sb.append(PLUS);
        sb.append(TEXT_DEFORMATION_ARRAY)
                .append(DeformationDirection.UY)
                .append("]");
        sb.append(MUL);
        sb.append(dy);
        sb.append(PLUS);
        sb.append("0.5 * deformation[")
                .append(DeformationDirection.UXX)
                .append("]");
        sb.append(MUL);
        sb.append(dx);
        sb.append(MUL);
        sb.append(dx);
        sb.append(PLUS);
        sb.append("0.5 * deformation[")
                .append(DeformationDirection.UYY)
                .append("]");
        sb.append(MUL);
        sb.append(dy);
        sb.append(MUL);
        sb.append(dy);
        sb.append(PLUS);
        sb.append(TEXT_DEFORMATION_ARRAY)
                .append(DeformationDirection.UXY)
                .append("]");
        sb.append(MUL);
        sb.append(dx);
        sb.append(MUL);
        sb.append(dy);
        kernel = kernel.replaceFirst(REPLACE_DEFORMATION_X, sb.toString());

        sb.setLength(0);
        sb.append(y);
        sb.append(PLUS);
        sb.append(TEXT_DEFORMATION_ARRAY)
                .append(DeformationDirection.V)
                .append("]");
        sb.append(PLUS);
        sb.append(TEXT_DEFORMATION_ARRAY)
                .append(DeformationDirection.VX)
                .append("]");
        sb.append(MUL);
        sb.append(dx);
        sb.append(PLUS);
        sb.append(TEXT_DEFORMATION_ARRAY)
                .append(DeformationDirection.VY)
                .append("]");
        sb.append(MUL);
        sb.append(dy);
        sb.append(PLUS);
        sb.append("0.5 * deformation[")
                .append(DeformationDirection.VXX)
                .append("]");
        sb.append(MUL);
        sb.append(dx);
        sb.append(MUL);
        sb.append(dx);
        sb.append(PLUS);
        sb.append("0.5 * deformation[")
                .append(DeformationDirection.VYY)
                .append("]");
        sb.append(MUL);
        sb.append(dy);
        sb.append(MUL);
        sb.append(dy);
        sb.append(PLUS);
        sb.append(TEXT_DEFORMATION_ARRAY)
                .append(DeformationDirection.VXY)
                .append("]");
        sb.append(MUL);
        sb.append(dx);
        sb.append(MUL);
        sb.append(dy);
        kernel = kernel.replaceFirst(REPLACE_DEFORMATION_Y, sb.toString());
    }

    private static String loadKernelResource(String resourceName) {
        if (!resourceName.endsWith(REPLACE_EXTENSION)) {
            resourceName = resourceName.concat(REPLACE_EXTENSION);
        }
        final StringBuilder sb = new StringBuilder();
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(KernelSourcePreparator.class.getResourceAsStream(resourceName)))) {
            br.lines().forEachOrdered((String t) -> {
                sb.append(t);
                sb.append("\n");
            });
        } catch (Exception ex) {
            Logger.error(ex);
        }
        return sb.toString();
    }

    private void prepareCorrelation(boolean usesVectorization, final boolean usesImage, final boolean usesZNCC) {
        String resourceName = "correlate-mean-";
        if (usesVectorization) {
            resourceName = resourceName.concat("vec-");
        } else {
            resourceName = resourceName.concat("noVec-");
        }
        if (usesImage) {
            resourceName = resourceName.concat("image");
        } else {
            resourceName = resourceName.concat("array");
        }

        kernel = kernel.replaceAll(
                REPLACE_CORRELATION_MEAN,
                loadKernelResource(resourceName));

        if (usesZNCC) {
            kernel = kernel.replaceAll(
                    REPLACE_CORRELATION_COMPUTE,
                    loadKernelResource("correlate-ZNCC.source"));
        } else {
            kernel = kernel.replaceAll(
                    REPLACE_CORRELATION_COMPUTE,
                    loadKernelResource("correlate-ZNSSD.source"));
        }
    }

    private void prepareStore() {
        kernel = kernel.replaceAll(
                REPLACE_STORE,
                loadKernelResource("storeResult.source"));
    }

    private void prepareSubsetSize(final int subsetSize) {
        kernel = kernel.replaceAll(REPLACE_SUBSET_SIZE, Integer.toString(subsetSize));
    }
}
