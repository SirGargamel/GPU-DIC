package cz.tul.dic;

import cz.tul.dic.complextask.ComplextTaskSolver;
import cz.tul.dic.data.roi.CircularROI;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerChecker;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.task.splitter.TaskSplit;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.engine.ResultCompilation;
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import cz.tul.dic.generators.facet.FacetGeneratorMode;
import cz.tul.dic.input.InputLoader;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportTarget;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.LoggingLevel;
import org.pmw.tinylog.writers.ConsoleWriter;

/**
 *
 * @author Petr Jecmen
 */
public class Computation {

    private static final LoggingLevel LOGGING_LEVEL = LoggingLevel.TRACE;
//    private static final File IN_VIDEO_REAL = new File("d:\\temp\\7202845m.avi");
//    private static final File IN_VIDEO_ART = new File("d:\\temp\\image.avi");
//    private static final List<File> IN_IMAGES;
    private static final File OUT_DIR = new File("D:\\temp\\results");
    private static final int SIZE_MIN = 3;
    private static final int SIZE_MAX = 50;
    private static final int SIZE_STEP = 1;

    static {
        Configurator.defaultConfig().writer(new ConsoleWriter()).level(LOGGING_LEVEL).activate();

//        IN_IMAGES = new LinkedList<>();
////        IN_IMAGES.add(new File("d:\\temp\\image000.bmp"));
////        IN_IMAGES.add(new File("d:\\temp\\image001.bmp"));
////        IN_IMAGES.add(new File("d:\\temp\\image002.bmp"));
////        IN_IMAGES.add(new File("d:\\temp\\image003.bmp"));
////        IN_IMAGES.add(new File("d:\\temp\\image004.bmp"));
//
//        IN_IMAGES.add(new File("d:\\temp\\7202845m.avi00000.bmp"));
//        IN_IMAGES.add(new File("d:\\temp\\7202845m.avi00004.bmp"));        
    }

    public static void commenceComputationStatic(final Object in) throws IOException, ComputationException {
        final Engine engine = new Engine();

        long time;
        TaskContainer tc, loadedTc;
        Set<ExportTask> loadedExports;
        for (int size = SIZE_MIN; size <= SIZE_MAX; size += SIZE_STEP) {
            tc = generateTask(in, size, KernelType.CL_1D_I_V_LL_MC_D);

            InputLoader.loadInput(tc);

            // generate exports
            generateExports(tc);
            final File input = (File) tc.getParameter(TaskParameter.IN);

            TaskContainerUtils.serializeTaskToConfig(tc);
            loadedTc = TaskContainerUtils.deserializeTaskFromConfig((File) in);
//            System.out.println(loadedTc);

            TaskContainerChecker.checkTaskValidity(tc);

            time = System.nanoTime();
            engine.computeTask(tc);
            time = System.nanoTime() - time;
            for (ExportTask et : tc.getExports()) {
                Exporter.export(et, tc);
            }
            Logger.info("Finished task " + size + "/" + tc.getParameter(TaskParameter.KERNEL) + " in " + (time / 1000000.0) + "ms.");
        }
        Logger.info("All done !!!");
    }

    private static TaskContainer generateTask(final Object in, final int facetSize, final KernelType kernelType) throws IOException {
        final TaskContainer tc = new TaskContainer(in);

        // select ROI 
        final ROI r1 = new RectangleROI(135, 19, 179, 200);
        tc.addRoi(0, r1);
        final ROI r2 = new CircularROI(108, 101, 26);
        tc.addRoi(0, r2);
        final ROI r3 = new CircularROI(203, 101, 26);
        tc.addRoi(0, r3);

        // select facet size
        TaskContainerUtils.setUniformFacetSize(tc, 0, facetSize);

        // facets
        tc.setParameter(TaskParameter.FACET_GENERATOR_MODE, FacetGeneratorMode.TIGHT);
//        tc.setParameter(TaskParameter.FACET_GENERATOR_MODE, FacetGeneratorMode.CLASSIC);
        tc.setParameter(TaskParameter.FACET_GENERATOR_SPACING, 1);

        // deformations
        tc.setDeformationLimits(0, r1, new double[]{-1, 1, 0.5, -5, 5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5});
        tc.setDeformationLimits(0, r2, new double[]{-1, 1, 0.5, -10, 1, 0.5});
        tc.setDeformationLimits(0, r3, new double[]{-1, 1, 0.5, -10, 1, 0.5});

        // task
        tc.setParameter(TaskParameter.TASK_SPLIT_VARIANT, TaskSplit.STATIC);
        tc.setParameter(TaskParameter.TASK_SPLIT_VALUE, 1000);

        // opencl
        tc.setParameter(TaskParameter.KERNEL, kernelType);

        return tc;
    }

    private static void generateExports(final TaskContainer tc) {
        // prepare ROIS
        final List<ROI> circular = new ArrayList<>(2);
        final List<ROI> rect = new ArrayList<>(1);
        for (ROI roi : tc.getRois(0)) {
            if (roi instanceof CircularROI) {
                circular.add(roi);
            } else {
                rect.add(roi);
            }
        }

        final String target = OUT_DIR.getAbsolutePath().concat(File.separator).concat(tc.getParameter(TaskParameter.KERNEL).toString()).concat("-");
        final String ext = String.format("%02d", tc.getFacetSize(0, rect.get(0))).concat(".bmp");
        for (int round = 0; round < TaskContainerUtils.getRoundCount(tc); round++) {
            tc.addExport(ExportTask.generateMapExport(Direction.Dx, ExportTarget.FILE, new File(target.concat(String.format("%02d", round)).concat("-X-").concat(ext)), round));
            tc.addExport(ExportTask.generateMapExport(Direction.Dy, ExportTarget.FILE, new File(target.concat(String.format("%02d", round)).concat("-Y-").concat(ext)), round));
            tc.addExport(ExportTask.generateMapExport(Direction.Dabs, ExportTarget.FILE, new File(target.concat(String.format("%02d", round)).concat("-ABS-").concat(ext)), round));

//            tc.addExport(ExportTask.generateMapExport(Direction.DX, ExportTarget.FILE, new File(target.concat(String.format("%02d", round)).concat("-DX-").concat(ext)), round));
//            tc.addExport(ExportTask.generateMapExport(Direction.DY, ExportTarget.FILE, new File(target.concat(String.format("%02d", round)).concat("-DY-").concat(ext)), round));
//            tc.addExport(ExportTask.generateMapExport(Direction.DABS, ExportTarget.FILE, new File(target.concat(String.format("%02d", round)).concat("-DABS-").concat(ext)), round));
            tc.addExport(ExportTask.generateMapExport(Direction.Dabs, ExportTarget.FILE, new File(target.concat(String.format("%02d", round)).concat("-ABS-RC-").concat(ext)), round, circular.toArray(new ROI[0])));
//            tc.addExport(ExportTask.generateMapExport(Direction.DABS, ExportTarget.FILE, new File(target.concat(String.format("%02d", round)).concat("-DABS-RC-").concat(ext)),round, circular.toArray(new ROI[0])));

            tc.addExport(ExportTask.generateMapExport(Direction.Dabs, ExportTarget.FILE, new File(target.concat(String.format("%02d", round)).concat("-ABS-RR-").concat(ext)), round, rect.toArray(new ROI[0])));
//            tc.addExport(ExportTask.generateMapExport(Direction.DABS, ExportTarget.FILE, new File(target.concat(String.format("%02d", round)).concat("-DABS-RR-").concat(ext)), round, rect.toArray(new ROI[0])));
        }
    }

    public static void commenceComputationDynamic(final Object in, final int facetSize) throws IOException, ComputationException {
        TaskContainer tc = new TaskContainer(in);
        InputLoader.loadInput(tc);

        final int roiRadius = 26;
        tc.addRoi(0, new CircularROI(108, 12, roiRadius));
        tc.addRoi(0, new CircularROI(201, 7, roiRadius));
        tc.addRoi(0, new CircularROI(108, 86, roiRadius));
        tc.addRoi(0, new CircularROI(202, 84, roiRadius));

        tc.setParameter(TaskParameter.FACET_SIZE, facetSize);
        tc.setParameter(TaskParameter.FACET_GENERATOR_MODE, FacetGeneratorMode.TIGHT);
        tc.setParameter(TaskParameter.FACET_GENERATOR_SPACING, 1);
        tc.setParameter(TaskParameter.INTERPOLATION, Interpolation.BILINEAR);
        tc.setParameter(TaskParameter.RESULT_COMPILATION, ResultCompilation.MAJOR_AVERAGING);
        tc.setParameter(TaskParameter.KERNEL, KernelType.CL_1D_I_V_LL_MC_D);
//        tc.setParameter(TaskParameter.TASK_SPLIT_VARIANT, TaskSplit.STATIC);
//        tc.setParameter(TaskParameter.TASK_SPLIT_VALUE, 1000);        

        TaskContainerChecker.checkTaskValidity(tc);

//        TaskContainerUtils.serializeTaskToConfig(tc);
        final String target = OUT_DIR.getAbsolutePath().concat(File.separator).concat("dyn").concat(File.separator).concat(((File) tc.getParameter(TaskParameter.IN)).getName()).concat("-");
        final String ext = String.format("%02d", facetSize);
        for (int round = 0; round < TaskContainerUtils.getRoundCount(tc); round++) {
            tc.addExport(ExportTask.generateMapExport(Direction.Dx, ExportTarget.FILE, new File(target.concat("-Dx-").concat(String.format("%02d", round)).concat("-").concat(ext).concat(".bmp")), round));
            tc.addExport(ExportTask.generateMapExport(Direction.Dy, ExportTarget.FILE, new File(target.concat("-Dy-").concat(String.format("%02d", round)).concat("-").concat(ext).concat(".bmp")), round));
            tc.addExport(ExportTask.generateMapExport(Direction.Dabs, ExportTarget.FILE, new File(target.concat("-Dabs-").concat(String.format("%02d", round)).concat("-").concat(ext).concat(".bmp")), round));
        }
        tc.addExport(ExportTask.generateSequenceExport(Direction.Dabs, ExportTarget.FILE, new File(target.concat("Dabs-").concat(ext).concat(".avi"))));

        computeDynamicTask(tc);

//        TaskContainerUtils.serializeTaskToBinary(tc);
//        TaskContainerUtils.exportTask(tc);
//        System.out.println(tc);
//        System.out.println(loadedTc);
    }

    public static void commenceComputationDynamic(final TaskContainer tc) throws IOException, ComputationException {
        TaskContainerChecker.checkTaskValidity(tc);

        final String target = OUT_DIR.getAbsolutePath().concat(File.separator).concat("dyn").concat(File.separator).concat(((File) tc.getParameter(TaskParameter.IN)).getName());
        final String ext = String.format("%02d", tc.getParameter(TaskParameter.FACET_SIZE));
        for (int round = 0; round < TaskContainerUtils.getRoundCount(tc); round++) {
            tc.addExport(ExportTask.generateMapExport(Direction.Dx, ExportTarget.FILE, new File(target.concat("-Dx-").concat(String.format("%02d", round)).concat("-").concat(ext).concat(".bmp")), round));
            tc.addExport(ExportTask.generateMapExport(Direction.Dy, ExportTarget.FILE, new File(target.concat("-Dy-").concat(String.format("%02d", round)).concat("-").concat(ext).concat(".bmp")), round));
            tc.addExport(ExportTask.generateMapExport(Direction.Dabs, ExportTarget.FILE, new File(target.concat("-Dabs-").concat(String.format("%02d", round)).concat("-").concat(ext).concat(".bmp")), round));
            tc.addExport(ExportTask.generateMapExport(Direction.Exx, ExportTarget.FILE, new File(target.concat("-Exx-").concat(String.format("%02d", round)).concat("-").concat(ext).concat(".bmp")), round));
            tc.addExport(ExportTask.generateMapExport(Direction.Eyy, ExportTarget.FILE, new File(target.concat("-Eyy-").concat(String.format("%02d", round)).concat("-").concat(ext).concat(".bmp")), round));
            tc.addExport(ExportTask.generateMapExport(Direction.Exy, ExportTarget.FILE, new File(target.concat("-Exy-").concat(String.format("%02d", round)).concat("-").concat(ext).concat(".bmp")), round));
            tc.addExport(ExportTask.generateMapExport(Direction.Eabs, ExportTarget.FILE, new File(target.concat("-Eabs-").concat(String.format("%02d", round)).concat("-").concat(ext).concat(".bmp")), round));
        }
        tc.addExport(ExportTask.generateSequenceExport(Direction.Dabs, ExportTarget.FILE, new File(target.concat("Dabs-").concat(ext).concat(".avi"))));
        tc.addExport(ExportTask.generateSequenceExport(Direction.Eabs, ExportTarget.FILE, new File(target.concat("Eabs-").concat(ext).concat(".avi"))));

        computeDynamicTask(tc);

//        TaskContainerUtils.serializeTaskToBinary(tc);
//        TaskContainerUtils.exportTask(tc);
//        System.out.println(tc);
//        System.out.println(loadedTc);
    }

    public static void computeDynamicTask(TaskContainer tc) throws IOException {
        try {
            long time = System.nanoTime();
            ComplextTaskSolver cts = new ComplextTaskSolver();
            final TaskContainer result = cts.solveComplexTask(tc);
            time = System.nanoTime() - time;
            Logger.info("Finished dynamic task " + tc.getParameter(TaskParameter.FACET_SIZE) + "/" + tc.getParameter(TaskParameter.KERNEL) + " in " + (time / 1000000.0) + "ms.");

            for (ExportTask et : result.getExports()) {
                Exporter.export(et, result);
            }
        } catch (ComputationException ex) {
            Logger.error(ex);
        }
    }

}
