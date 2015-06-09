/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernels;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.deformation.Deformation;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
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

    public static final String KERNEL_EXTENSION = ".cl";
    private static final String REPLACE_FACET_SIZE = "-1";
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
            final int facetSize, final DeformationDegree deg,
            final boolean usesVectorization, final Interpolation interpolation, final boolean usesImage) throws ComputationException {
        final KernelSourcePreparator kp = new KernelSourcePreparator(kernelName);

        try {
            kp.loadKernel();
            kp.prepareFacetSize(facetSize);
            kp.prepareDeformations(deg, usesVectorization);
            kp.prepareInterpolation(interpolation, usesImage);
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

    private void prepareFacetSize(final int facetSize) {
        kernel = kernel.replaceAll(REPLACE_FACET_SIZE, Integer.toString(facetSize));
    }

    private void prepareDeformations(final DeformationDegree deg, final boolean usesVectorization) throws ComputationException {
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

        final StringBuilder sb = new StringBuilder();
        // deformation generation                
        final int defCoeffCount = DeformationUtils.getDeformationCoeffCount(deg);
        sb.append("const int limitsBase = facetId * ");
        sb.append(defCoeffCount * 3);
        sb.append(";\n");
        sb.append("const int countsBase = facetId * ");
        sb.append(defCoeffCount + 1);
        sb.append(";\n");
        sb.append("if (deformationId >= deformationCounts[countsBase + ");
        sb.append(defCoeffCount);
        sb.append("]) { return; }\n");
        sb.append("int counter = deformationId;\n");
        for (int i = 0; i < defCoeffCount; i++) {
            sb.append(TEXT_DEFORMATION_ARRAY);
            sb.append(i);
            sb.append("] = counter % deformationCounts[countsBase + ");
            sb.append(i);
            sb.append("];\n");
            sb.append("counter = counter / deformationCounts[countsBase + ");
            sb.append(i);
            sb.append("];\n");
        }
        for (int i = 0; i < defCoeffCount; i++) {
            sb.append(TEXT_DEFORMATION_ARRAY);
            sb.append(i);
            sb.append("] = deformationLimits[limitsBase + ");
            sb.append(i * 3);
            sb.append("] + deformation[");
            sb.append(i);
            sb.append("] * deformationLimits[limitsBase + ");
            sb.append(i * 3 + 2);
            sb.append("];\n");
        }
        kernel = kernel.replaceAll(REPLACE_DEFORMATION_COMPUTATION, sb.toString());

        switch (deg) {
            case ZERO:
                // coeff computation
                sb.setLength(0);
                sb.append(x);
                sb.append(PLUS);
                sb.append(TEXT_DEFORMATION_ARRAY)
                        .append(Deformation.U)
                        .append("]");
                kernel = kernel.replaceFirst(REPLACE_DEFORMATION_X, sb.toString());

                sb.setLength(0);
                sb.append(y);
                sb.append(PLUS);
                sb.append(TEXT_DEFORMATION_ARRAY)
                        .append(Deformation.V)
                        .append("]");
                kernel = kernel.replaceFirst(REPLACE_DEFORMATION_Y, sb.toString());
                break;
            case FIRST:
                // coeff computation
                sb.setLength(0);
                sb.append(x);
                sb.append(PLUS);
                sb.append(TEXT_DEFORMATION_ARRAY)
                        .append(Deformation.U)
                        .append("]");
                sb.append(PLUS);
                sb.append(TEXT_DEFORMATION_ARRAY)
                        .append(Deformation.UX)
                        .append("]");
                sb.append(MUL);
                sb.append(dx);
                sb.append(PLUS);
                sb.append(TEXT_DEFORMATION_ARRAY)
                        .append(Deformation.UY)
                        .append("]");
                sb.append(MUL);
                sb.append(dy);
                kernel = kernel.replaceFirst(REPLACE_DEFORMATION_X, sb.toString());

                sb.setLength(0);
                sb.append(y);
                sb.append(PLUS);
                sb.append(TEXT_DEFORMATION_ARRAY)
                        .append(Deformation.V)
                        .append("]");
                sb.append(PLUS);
                sb.append(TEXT_DEFORMATION_ARRAY)
                        .append(Deformation.VX)
                        .append("]");
                sb.append(MUL);
                sb.append(dx);
                sb.append(PLUS);
                sb.append(TEXT_DEFORMATION_ARRAY)
                        .append(Deformation.VY)
                        .append("]");
                sb.append(MUL);
                sb.append(dy);
                kernel = kernel.replaceFirst(REPLACE_DEFORMATION_Y, sb.toString());
                break;
            case SECOND:
                // coeff computation
                sb.setLength(0);
                sb.append(x);
                sb.append(PLUS);
                sb.append(TEXT_DEFORMATION_ARRAY)
                        .append(Deformation.U)
                        .append("]");
                sb.append(PLUS);
                sb.append(TEXT_DEFORMATION_ARRAY)
                        .append(Deformation.UX)
                        .append("]");
                sb.append(MUL);
                sb.append(dx);
                sb.append(PLUS);
                sb.append(TEXT_DEFORMATION_ARRAY)
                        .append(Deformation.UY)
                        .append("]");
                sb.append(MUL);
                sb.append(dy);
                sb.append(PLUS);
                sb.append("0.5 * deformation[")
                        .append(Deformation.UXX)
                        .append("]");
                sb.append(MUL);
                sb.append(dx);
                sb.append(MUL);
                sb.append(dx);
                sb.append(PLUS);
                sb.append("0.5 * deformation[")
                        .append(Deformation.UYY)
                        .append("]");
                sb.append(MUL);
                sb.append(dy);
                sb.append(MUL);
                sb.append(dy);
                sb.append(PLUS);
                sb.append(TEXT_DEFORMATION_ARRAY)
                        .append(Deformation.UXY)
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
                        .append(Deformation.V)
                        .append("]");
                sb.append(PLUS);
                sb.append(TEXT_DEFORMATION_ARRAY)
                        .append(Deformation.VX)
                        .append("]");
                sb.append(MUL);
                sb.append(dx);
                sb.append(PLUS);
                sb.append(TEXT_DEFORMATION_ARRAY)
                        .append(Deformation.VY)
                        .append("]");
                sb.append(MUL);
                sb.append(dy);
                sb.append(PLUS);
                sb.append("0.5 * deformation[")
                        .append(Deformation.VXX)
                        .append("]");
                sb.append(MUL);
                sb.append(dx);
                sb.append(MUL);
                sb.append(dx);
                sb.append(PLUS);
                sb.append("0.5 * deformation[")
                        .append(Deformation.VYY)
                        .append("]");
                sb.append(MUL);
                sb.append(dy);
                sb.append(MUL);
                sb.append(dy);
                sb.append(PLUS);
                sb.append(TEXT_DEFORMATION_ARRAY)
                        .append(Deformation.VXY)
                        .append("]");
                sb.append(MUL);
                sb.append(dx);
                sb.append(MUL);
                sb.append(dy);
                kernel = kernel.replaceFirst(REPLACE_DEFORMATION_Y, sb.toString());
                break;
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported degree of deformation");
        }
        kernel = kernel.replaceAll(REPLACE_DEFORMATION_DEGREE, Integer.toString(DeformationUtils.getDeformationCoeffCount(deg)));
    }    

    private void prepareInterpolation(final Interpolation interpolation, final boolean usesImage) throws ComputationException {
        String resourceName = "interpolate-";
        switch (interpolation) {
            case BILINEAR:
                resourceName = resourceName.concat("bilinear");
                break;
            case BICUBIC:
                resourceName = resourceName.concat("bicubic");
                break;
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported type of interpolation.");
        }
        if (usesImage) {
            resourceName = resourceName.concat("-image.cl");
        } else {
            resourceName = resourceName.concat("-array.cl");
        }
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(KernelSourcePreparator.class.getResourceAsStream("/cz/tul/dic/engine/opencl/interpolation/".concat(resourceName))))) {
            final StringBuilder sb = new StringBuilder();
            br.lines().forEachOrdered((String t) -> {
                sb.append(t);
                sb.append("\n");
            });
            kernel = kernel.replaceFirst(REPLACE_INTERPOLATION, sb.toString());
        } catch (Exception ex) {
            Logger.error(ex);
        }

    }
}
