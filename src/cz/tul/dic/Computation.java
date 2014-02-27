package cz.tul.dic;

import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.generators.DeformationGenerator;
import cz.tul.dic.generators.FacetGenerator;
import cz.tul.dic.generators.FacetGeneratorMode;
import cz.tul.dic.input.InputLoader;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportMode;
import cz.tul.dic.output.ExportTarget;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Petr Jecmen
 */
public class Computation {

    private static final File IN_VIDEO_REAL = new File("d:\\temp\\7202845m.avi");
    private static final File IN_VIDEO_ART = new File("d:\\temp\\image.avi");
    private static final List<File> IN_IMAGES;
    private static final File OUT_DIR = new File("D:\\temp\\results");
    private static final int SIZE_MIN = 10;
    private static final int SIZE_MAX = 30;
    private static final int SIZE_STEP = 1;    

    static {
        IN_IMAGES = new LinkedList<>();

        IN_IMAGES.add(new File("d:\\temp\\image000.bmp"));
        IN_IMAGES.add(new File("d:\\temp\\image001.bmp"));
        IN_IMAGES.add(new File("d:\\temp\\image002.bmp"));
        IN_IMAGES.add(new File("d:\\temp\\image003.bmp"));
        IN_IMAGES.add(new File("d:\\temp\\image004.bmp"));
    }

    public static void commenceComputation() throws IOException {                        
        final List<TaskContainer> tcs = new LinkedList<>();

        for (int size = SIZE_MIN; size <= SIZE_MAX; size+= SIZE_STEP ) {
//            for (KernelType kt : KernelType.values()) {
//                tcs.add(generateTask(IN_IMAGES, size, kt));
//            }
            tcs.add(generateTask(IN_IMAGES, size, KernelType.CL_1D_I_V_LL_MC_D));
        }                

        System.out.println("TODO TightModeFacetGenerator");
        System.out.println("TODO TaskSplitter");
        System.out.println("TODO Check task container parameters validity - ROI, exports, NULL data");
        System.out.println("TODO Compute ideal workSize dynamically");
        System.out.println("TODO ExportGUI");

        // compute task        
        final Engine engine = new Engine();

        long time;
        for (TaskContainer tc : tcs) {
            time = System.nanoTime();
            engine.computeTask(tc);
            time = System.nanoTime() - time;
            Exporter.export(tc);
            System.out.println("Finished round " + tc.getFacetSize() + "/" + tc.getParameter(TaskParameter.KERNEL) + " in " + (time / 1000000.0) + "ms.");
        }
        System.out.println("All done !!!");
    }

    private static TaskContainer generateTask(final Object in, final int facetSize, final KernelType kernelType) throws IOException {
        final TaskContainer tc = new TaskContainer();

        // load input data
        InputLoader.loadInput(in, tc);                

        // select ROI        
        tc.addRoi(new ROI(0, 0, 320, 240), 0);

        // select facet size
        tc.setFacetSize(facetSize);

        // add outputs             
        final String target = OUT_DIR.getAbsolutePath().concat(File.separator);
        final String ext = Integer.toString(facetSize).concat("-").concat(kernelType.name()).concat(".bmp");
        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        for (int round = 0; round < roundCount; round++) {
            tc.addExportTask(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.X, new File(target.concat(Integer.toString(round)).concat("-X-").concat(ext)), 0));
            tc.addExportTask(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.Y, new File(target.concat(Integer.toString(round)).concat("-Y-").concat(ext)), 0));
            tc.addExportTask(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.ABS, new File(target.concat(Integer.toString(round)).concat("-ABS-").concat(ext)), 0));
        }
//        tc.addExportTask(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.ABS, new File(target.concat("0-").concat(ext)), 0));
//        tc.addExportTask(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.ABS, new File(target.concat("1-").concat(ext)), 1));
//        tc.addExportTask(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.ABS, new File(target.concat("2-").concat(ext)), 2));
//        tc.addExportTask(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.ABS, new File(target.concat("3-").concat(ext)), 3));
//        tc.addExportTask(new ExportTask(ExportMode.SEQUENCE, ExportTarget.FILE, Direction.ABS, new File("D:\\test.avi")));
//        tc.addExportTask(new ExportTask(ExportMode.MAP, ExportTarget.CSV, Direction.ABS, new File("D:\\testMap.csv"), 0));
//        tc.addExportTask(new ExportTask(ExportMode.LINE, ExportTarget.CSV, Direction.ABS, new File("D:\\testLine.csv"), 0, 20, 20));

        // generate facets
        tc.addParameter(TaskParameter.FACET_GENERATOR_MODE, FacetGeneratorMode.CLASSIC);
        FacetGenerator.generateFacets(tc);

        // generate deformations
        tc.addParameter(TaskParameter.DEFORMATION_DEGREE, DeformationDegree.ZERO);
        tc.addParameter(TaskParameter.DEFORMATION_BOUNDS, new double[]{-10, 0, 0.5, -10, 0, 0.5});
//        tc.addParameter(TaskParameter.DEFORMATION_BOUNDS, new double[]{-10, 0, 0.5, -10, 0, 0.5});
        
//        tc.addParameter(TaskParameter.DEFORMATION_DEGREE, DeformationDegree.FIRST);
//        tc.addParameter(TaskParameter.DEFORMATION_BOUNDS, new double[] {-2, 2, 0.5, -5, 5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5});        
        DeformationGenerator.generateDeformations(tc);

        tc.addParameter(TaskParameter.KERNEL, kernelType);

        return tc;
    }

}
