package cz.tul.dic.engine;

import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.opencl.WorkSizeManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author Petr Jecmen
 */
public class KernelPreparator {

    private static final String KERNEL_EXTENSION = ".cl";
    private static final String REPLACE_FACET_SIZE = "-1";
    private static final String REPLACE_DEFORMATION = "%DEF%";
    private final String kernelName;
    private String kernel;

    public static String prepareKernel(final String kernelName, final TaskContainer tc) throws IOException {
        final KernelPreparator kp = new KernelPreparator(kernelName);

        kp.loadKernel();
        kp.prepareFacetSize(tc.getFacetSize());
        kp.prepareDeformations((DeformationDegree) tc.getParameter(TaskParameter.DEFORMATION_DEGREE));

        return kp.kernel;
    }

    private KernelPreparator(final String kernelName) {
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

    private void prepareDeformations(final DeformationDegree deg) {
        switch (deg) {
            case ZERO:
                kernel = kernel.replaceFirst(REPLACE_DEFORMATION, "coords.x + deformations[baseIndexDeformation], \n"
                        + "coords.y + deformations[baseIndexDeformation + 1]");
                break;
            case FIRST:
                kernel = kernel.replaceFirst(REPLACE_DEFORMATION, "coords.x + deformations[baseIndexDeformation] + deformations[baseIndexDeformation + 2] * def.x + deformations[baseIndexDeformation + 4] * def.y, \n"
                        + "coords.y + deformations[baseIndexDeformation + 1] + deformations[baseIndexDeformation + 3] * def.x + deformations[baseIndexDeformation + 5] * def.y");
                break;
            case SECOND:
                throw new IllegalArgumentException("Second degree not supported yet");
            default:
                throw new IllegalArgumentException("Unsupported degree of deformation");
        }
    }
}
