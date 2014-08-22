package cz.tul.dic.engine.opencl;

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
    private static final String PLUS = " + ";
    private static final String MUL = " * ";
    private final String kernelName;
    private String kernel;

    public static String prepareKernel(final String kernelName, final int facetSize, final DeformationDegree deg, final boolean usesVectorization, final Interpolation interpolation) throws IOException, ComputationException {
        final KernelSourcePreparator kp = new KernelSourcePreparator(kernelName);

        kp.loadKernel();
        kp.prepareFacetSize(facetSize);
        kp.prepareDeformations(deg, usesVectorization);
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
        switch (deg) {
            case ZERO:
                // deformation generation
                sb.setLength(0);
                sb.append("int counter = deformationId;\n");
                sb.append("deformation[0] = counter % deformationCounts[0];\n");
                sb.append("counter = counter / deformationCounts[0];\n");
                sb.append("deformation[1] = counter;\n");
                sb.append("deformation[0] = deformationLimits[0] + deformation[0] * deformationLimits[2];\n");
                sb.append("deformation[1] = deformationLimits[3] + deformation[1] * deformationLimits[5];\n");
//                sb.append("deformation[0] = 0;\n");
//                sb.append("deformation[1] = 0;\n");
                kernel = kernel.replaceAll(REPLACE_DEFORMATION_COMPUTATION, sb.toString());
                // coeff computation
                sb.setLength(0);
                sb.append(x);
                sb.append(PLUS);
                sb.append("deformation[")
                        .append(Deformation.U)
                        .append("]");
                kernel = kernel.replaceFirst(REPLACE_DEFORMATION_X, sb.toString());

                sb.setLength(0);
                sb.append(y);
                sb.append(PLUS);
                sb.append("deformation[")
                        .append(Deformation.V)
                        .append("]");
                kernel = kernel.replaceFirst(REPLACE_DEFORMATION_Y, sb.toString());
                break;
            case FIRST:
                // deformation generation
                sb.setLength(0);
                sb.append("int counter = deformationId;\n");
                for (int i = 0; i < 6; i++) {
                    sb.append("deformation[");
                    sb.append(i);
                    sb.append("] = counter % deformationCounts[");
                    sb.append(i);
                    sb.append("];\n");
                    sb.append("counter = counter / deformationCounts[");
                    sb.append(i);
                    sb.append("];\n");
                }              
                for (int i = 0; i < 6; i++) {
                    sb.append("deformation[");
                    sb.append(i);
                    sb.append("] = deformationLimits[");
                    sb.append(i * 3);
                    sb.append("] + deformation[");
                    sb.append(i);
                    sb.append("] * deformationLimits[");
                    sb.append(i * 3 + 2);
                    sb.append("];\n");
                }                
                kernel = kernel.replaceAll(REPLACE_DEFORMATION_COMPUTATION, sb.toString());
                // coeff computation
                sb.setLength(0);
                sb.append(x);
                sb.append(PLUS);
                sb.append("deformation[")
                        .append(Deformation.U)
                        .append("]");
                sb.append(PLUS);
                sb.append("deformation[")
                        .append(Deformation.UX)
                        .append("]");
                sb.append(MUL);
                sb.append(dx);
                sb.append(PLUS);
                sb.append("deformation[")
                        .append(Deformation.UY)
                        .append("]");
                sb.append(MUL);
                sb.append(dy);
                kernel = kernel.replaceFirst(REPLACE_DEFORMATION_X, sb.toString());

                sb.setLength(0);
                sb.append(y);
                sb.append(PLUS);
                sb.append("deformation[")
                        .append(Deformation.V)
                        .append("]");
                sb.append(PLUS);
                sb.append("deformation[")
                        .append(Deformation.VX)
                        .append("]");
                sb.append(MUL);
                sb.append(dx);
                sb.append(PLUS);
                sb.append("deformation[")
                        .append(Deformation.VY)
                        .append("]");
                sb.append(MUL);
                sb.append(dy);
                kernel = kernel.replaceFirst(REPLACE_DEFORMATION_Y, sb.toString());
                break;
            case SECOND:
                // deformation generation
                sb.setLength(0);
                sb.append("int counter = deformationId;\n");
                for (int i = 0; i < 12; i++) {
                    sb.append("deformation[");
                    sb.append(i);
                    sb.append("] = counter % deformationCounts[");
                    sb.append(i);
                    sb.append("];\n");
                    sb.append("counter = counter / deformationCounts[");
                    sb.append(i);
                    sb.append("];\n");
                }              
                for (int i = 0; i < 12; i++) {
                    sb.append("deformation[");
                    sb.append(i);
                    sb.append("] = deformationLimits[");
                    sb.append(i * 3);
                    sb.append("] + deformation[");
                    sb.append(i);
                    sb.append("] * deformationLimits[");
                    sb.append(i * 3 + 2);
                    sb.append("];\n");
                }  
                kernel = kernel.replaceAll(REPLACE_DEFORMATION_COMPUTATION, sb.toString());
                // coeff computation
                sb.setLength(0);
                sb.append(x);
                sb.append(PLUS);
                sb.append("deformation[")
                        .append(Deformation.U)
                        .append("]");
                sb.append(PLUS);
                sb.append("deformation[")
                        .append(Deformation.UX)
                        .append("]");
                sb.append(MUL);
                sb.append(dx);
                sb.append(PLUS);
                sb.append("deformation[")
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
                sb.append("deformation[")
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
                sb.append("deformation[")
                        .append(Deformation.V)
                        .append("]");
                sb.append(PLUS);
                sb.append("deformation[")
                        .append(Deformation.VX)
                        .append("]");
                sb.append(MUL);
                sb.append(dx);
                sb.append(PLUS);
                sb.append("deformation[")
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
                sb.append("deformation[")
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
        kernel = kernel.replaceAll(REPLACE_DEFORMATION_DEGREE, Integer.toString(DeformationUtils.getDeformationArrayLength(deg)));
    }

    private void prepareInterpolation(final Interpolation interpolation) throws ComputationException {
        String resourceName;
        switch (interpolation) {
            case BILINEAR:
                resourceName = "interpolate-bilinear.cl";
                break;
            case BICUBIC:
                resourceName = "interpolate-bicubic.cl";
                break;
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported type of interpolation.");
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
