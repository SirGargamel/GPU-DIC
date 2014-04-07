package cz.tul.dic.engine.opencl;

import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import cz.tul.dic.data.deformation.DeformationDegree;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class KernelSourcePreparator {

    private static final String KERNEL_EXTENSION = ".cl";
    private static final String REPLACE_FACET_SIZE = "-1";
    private static final String REPLACE_DEFORMATION_X = "%DEF_X%";
    private static final String REPLACE_DEFORMATION_Y = "%DEF_Y%";
    private static final String REPLACE_DEFORMATION_DEGREE = "%DEF_D%";
    private static final String REPLACE_INTERPOLATION = "%INT%";
    private static final String PLUS = " + ";
    private static final String MUL = " * ";
    private final String kernelName;
    private String kernel;

    public static String prepareKernel(final String kernelName, final int facetSize, final DeformationDegree deg, final int deformationArrayLength, final boolean usesVectorization, final Interpolation interpolation) throws IOException {
        final KernelSourcePreparator kp = new KernelSourcePreparator(kernelName);

        kp.loadKernel();
        kp.prepareFacetSize(facetSize);
        kp.prepareDeformations(deg, deformationArrayLength, usesVectorization);
        kp.prepareInterpolation(interpolation);

        return kp.kernel;
    }

    private KernelSourcePreparator(final String kernelName) {
        this.kernelName = kernelName;
    }

    private void loadKernel() throws IOException {
        try (BufferedReader bin = new BufferedReader(new InputStreamReader(WorkSizeManager.class.getResourceAsStream(kernelName.concat(KERNEL_EXTENSION))))) {
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

    private void prepareDeformations(final DeformationDegree deg, final int deformationArrayLength, final boolean usesVectorization) {
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
        switch (deg) {
            case ZERO:
                sb.append(x);
                sb.append(PLUS);
                sb.append("deformations[baseIndexDeformation]");
//                kernel = kernel.replaceFirst(REPLACE_DEFORMATION_X, "coords.x + deformations[baseIndexDeformation]");
                kernel = kernel.replaceFirst(REPLACE_DEFORMATION_X, sb.toString());

                sb.setLength(0);
                sb.append(y);
                sb.append(PLUS);
                sb.append("deformations[baseIndexDeformation + 1]");
//                kernel = kernel.replaceFirst(REPLACE_DEFORMATION_Y, "coords.y + deformations[baseIndexDeformation + 1]");
                kernel = kernel.replaceFirst(REPLACE_DEFORMATION_Y, sb.toString());
                break;
            case FIRST:
                sb.append(x);
                sb.append(PLUS);
                sb.append("deformations[baseIndexDeformation]");
                sb.append(PLUS);
                sb.append("deformations[baseIndexDeformation + 2]");
                sb.append(MUL);
                sb.append(dx);
                sb.append(PLUS);
                sb.append("deformations[baseIndexDeformation + 4]");
                sb.append(MUL);
                sb.append(dy);
//                kernel = kernel.replaceFirst(REPLACE_DEFORMATION_X, "coords.x + deformations[baseIndexDeformation] + deformations[baseIndexDeformation + 2] * def.x + deformations[baseIndexDeformation + 4] * def.y");
                kernel = kernel.replaceFirst(REPLACE_DEFORMATION_X, sb.toString());

                sb.setLength(0);
                sb.append(y);
                sb.append(PLUS);
                sb.append("deformations[baseIndexDeformation + 1]");
                sb.append(PLUS);
                sb.append("deformations[baseIndexDeformation + 3]");
                sb.append(MUL);
                sb.append(dx);
                sb.append(PLUS);
                sb.append("deformations[baseIndexDeformation + 5]");
                sb.append(MUL);
                sb.append(dy);
//                kernel = kernel.replaceFirst(REPLACE_DEFORMATION_Y, "coords.y + deformations[baseIndexDeformation + 1] + deformations[baseIndexDeformation + 3] * def.x + deformations[baseIndexDeformation + 5] * def.y");
                kernel = kernel.replaceFirst(REPLACE_DEFORMATION_Y, sb.toString());
                break;
            case SECOND:
                throw new IllegalArgumentException("Second degree not supported yet");
            default:
                throw new IllegalArgumentException("Unsupported degree of deformation");
        }
        kernel = kernel.replaceFirst(REPLACE_DEFORMATION_DEGREE, Integer.toString(deformationArrayLength));
    }

    private void prepareInterpolation(final Interpolation interpolation) {
        String resourceName;
        switch (interpolation) {
            case BILINEAR:
                resourceName = "interpolate-bilinear.cl";
                break;
            case BICUBIC:
                resourceName = "interpolate-bicubic.cl";
                break;
            default:
                throw new IllegalArgumentException("Unsupported type of interpolation.");
        }
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(KernelSourcePreparator.class.getResourceAsStream("/cz/tul/dic/engine/opencl/interpolation/".concat(resourceName))))) {
            final StringBuilder sb = new StringBuilder();
            br.lines().forEachOrdered((String t) -> {
                sb.append(t);
                sb.append("\n");
            });
            kernel = kernel.replaceFirst(REPLACE_INTERPOLATION, sb.toString());
        } catch (Exception ex) {
            Logger.getLogger(KernelSourcePreparator.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
