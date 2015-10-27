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
import cz.tul.dic.engine.opencl.kernels.Kernel;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class KernelSourcePreparator {

    public static final String KERNEL_EXTENSION = ".cl";
    private static final String REPLACE_INIT = "%INIT%";
    private static final String REPLACE_CORRELATION = "%CORR%";
    private static final String REPLACE_EXTENSION = ".source";
    private static final String REPLACE_SUBSET_SIZE = "%SS%";
    private static final String REPLACE_DELTA = "%C&S%";
    private static final String REPLACE_DEFORMATION = "%DEF%";
    private static final String REPLACE_DEFORMATION_X = "%DEF_X%";
    private static final String REPLACE_DEFORMATION_Y = "%DEF_Y%";
    private static final String REPLACE_DEFORMATION_DEGREE = "%DEF_D%";
    private static final String REPLACE_DEFORMATION_COMPUTATION = "%DEF_C%";
    private static final String REPLACE_INTERPOLATION = "%INT%";
    private static final String TEXT_DEFORMATION_ARRAY = "deformation[";
    private static final String PLUS = " + ";
    private static final String MUL = " * ";
    private final String kernelName;
    private String kernel;

    private KernelSourcePreparator(final String kernelName) {
        this.kernelName = kernelName;
    }

    public static String prepareKernel(
            final String kernelName,
            final int subsetSize, final DeformationDegree deg,
            final boolean is2D, final boolean usesVectorization, final Interpolation interpolation,
            final boolean usesImage, final boolean usesLocalMemory, final boolean usesMemoryCoalescing) throws ComputationException {
        final KernelSourcePreparator kp = new KernelSourcePreparator(kernelName);

        try {
            kp.loadKernel();
            kp.prepareInit(is2D, usesLocalMemory, usesLocalMemory);
            kp.prepareCorrelation(usesVectorization);
            kp.prepareDeltaAndStore();
            kp.prepareDeformations(deg, usesVectorization, usesLocalMemory);
            kp.prepareInterpolation(interpolation, usesImage);
            kp.prepareSubsetSize(subsetSize);
            return kp.kernel;
        } catch (IOException ex) {
            throw new ComputationException(ComputationExceptionCause.IO, ex);
        }
    }

    private void loadKernel() throws IOException {
        try (BufferedReader bin = new BufferedReader(new InputStreamReader(Kernel.class.getResourceAsStream(kernelName.concat(KERNEL_EXTENSION))))) {
            final StringBuilder sb = new StringBuilder();
            while (bin.ready()) {
                sb.append(bin.readLine());
                sb.append("\n");
            }
            kernel = sb.toString();
        }
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
        kernel = kernel.replaceAll(REPLACE_DEFORMATION_COMPUTATION, sb.toString());

        resourceName = "deformation-compute-";
        if (usesLocalMemory) {
            resourceName = resourceName.concat("local");
        } else {
            resourceName = resourceName.concat("global");
        }
        kernel = kernel.replaceAll(REPLACE_DEFORMATION, loadKernelResource(resourceName));

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

    private void prepareCorrelation(boolean usesVectorization) {
        String resourceName = "correlate-";
        if (usesVectorization) {
            resourceName = resourceName.concat("vec");
        } else {
            resourceName = resourceName.concat("noVec");
        }

        kernel = kernel.replaceAll(
                REPLACE_CORRELATION,
                loadKernelResource(resourceName));
    }

    private void prepareDeltaAndStore() {
        kernel = kernel.replaceAll(
                REPLACE_DELTA,
                loadKernelResource("computeDeltaAndStore.source"));
    }

    private void prepareSubsetSize(final int subsetSize) {
        kernel = kernel.replaceAll(REPLACE_SUBSET_SIZE, Integer.toString(subsetSize));
    }
}
