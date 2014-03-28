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
import cz.tul.dic.engine.opencl.KernelType;
import cz.tul.dic.generators.facet.FacetGeneratorMode;
import cz.tul.dic.input.InputLoader;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportMode;
import cz.tul.dic.output.ExportTarget;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import cz.tul.dic.output.OutputUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
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
    private static final int SIZE_DYN = 19;
    private static final Set<ExportTask> exports;

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
        exports = new HashSet<>();
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
            OutputUtils.serializeExports(exports, tc);
            loadedExports = OutputUtils.deserializeExports((File) in);
//            System.out.println(loadedExports);

            TaskContainerUtils.serializeTaskContainerToConfig(tc);
            loadedTc = TaskContainerUtils.deserializeTaskContainerFromConfig((File) in);
//            System.out.println(loadedTc);

            TaskContainerChecker.checkTaskValidity(tc);

            time = System.nanoTime();
            engine.computeTask(tc);
            time = System.nanoTime() - time;
            for (ExportTask et : exports) {
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
        tc.addRoi(r1, 0);
        final ROI r2 = new CircularROI(108, 101, 26);
        tc.addRoi(r2, 0);
        final ROI r3 = new CircularROI(203, 101, 26);
        tc.addRoi(r3, 0);

        // select facet size
        TaskContainerUtils.setUniformFacetSize(tc, 0, facetSize);

        // facets
        tc.setParameter(TaskParameter.FACET_GENERATOR_MODE, FacetGeneratorMode.TIGHT);
//        tc.setParameter(TaskParameter.FACET_GENERATOR_MODE, FacetGeneratorMode.CLASSIC);
        tc.setParameter(TaskParameter.FACET_GENERATOR_SPACING, 1);

        // deformations
        tc.setDeformationLimits(new double[]{-1, 1, 0.5, -5, 5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5}, 0, r1);
        tc.setDeformationLimits(new double[]{-1, 1, 0.5, -10, 1, 0.5}, 0, r2);
        tc.setDeformationLimits(new double[]{-1, 1, 0.5, -10, 1, 0.5}, 0, r3);

        // task
        tc.setParameter(TaskParameter.TASK_SPLIT_VARIANT, TaskSplit.STATIC);
        tc.setParameter(TaskParameter.TASK_SPLIT_VALUE, 1000);

        // opencl
        tc.setParameter(TaskParameter.KERNEL, kernelType);

        return tc;
    }

    private static void generateExports(final TaskContainer tc) {
        exports.clear();

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
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.X, new File(target.concat(String.format("%02d", round)).concat("-X-").concat(ext)), new int[]{round}));
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.Y, new File(target.concat(String.format("%02d", round)).concat("-Y-").concat(ext)), new int[]{round}));
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.ABS, new File(target.concat(String.format("%02d", round)).concat("-ABS-").concat(ext)), new int[]{round}));

//            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.DX, new File(target.concat(String.format("%02d", round)).concat("-DX-").concat(ext)), new int[] {round}));
//            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.DY, new File(target.concat(String.format("%02d", round)).concat("-DY-").concat(ext)), new int[] {round}));
//            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.DABS, new File(target.concat(String.format("%02d", round)).concat("-DABS-").concat(ext)), new int[] {round}));
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.ABS, new File(target.concat(String.format("%02d", round)).concat("-ABS-RC-").concat(ext)), new int[]{round}, circular.toArray(new ROI[0])));
//            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.DABS, new File(target.concat(String.format("%02d", round)).concat("-DABS-RC-").concat(ext)), new int[] {round}, circular.toArray(new ROI[0])));

            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.ABS, new File(target.concat(String.format("%02d", round)).concat("-ABS-RR-").concat(ext)), new int[]{round}, rect.toArray(new ROI[0])));
//            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.DABS, new File(target.concat(String.format("%02d", round)).concat("-DABS-RR-").concat(ext)), new int[] {round}, rect.toArray(new ROI[0])));
        }
    }

    public static void commenceComputationDynamic(final Object in) throws IOException, ComputationException {
        TaskContainer tc = new TaskContainer(in);
        InputLoader.loadInput(tc);

        final int roiRadius = 26;
        tc.addRoi(new CircularROI(108, 12, roiRadius), 0);
        tc.addRoi(new CircularROI(201, 7, roiRadius), 0);
        tc.addRoi(new CircularROI(108, 86, roiRadius), 0);
        tc.addRoi(new CircularROI(202, 84, roiRadius), 0);

        TaskContainerUtils.setUniformFacetSize(tc, 0, roiRadius / 2);

        TaskContainerChecker.checkTaskValidity(tc);

        final String target = OUT_DIR.getAbsolutePath().concat(File.separator).concat("dyn").concat(File.separator).concat(tc.getParameter(TaskParameter.KERNEL).toString()).concat("-");
        final String ext = String.format("%02d", SIZE_DYN).concat(".bmp");
        for (int round = 0; round < TaskContainerUtils.getRoundCount(tc); round++) {
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.X, new File(target.concat(String.format("%02d", round)).concat("-X-").concat(ext)), new int[]{round}));
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.Y, new File(target.concat(String.format("%02d", round)).concat("-Y-").concat(ext)), new int[]{round}));
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.ABS, new File(target.concat(String.format("%02d", round)).concat("-ABS-").concat(ext)), new int[]{round}));
        }
        exports.add(new ExportTask(ExportMode.SEQUENCE, ExportTarget.FILE, Direction.X, new File(target.concat("-X-").concat(ext).replace("bmp", "avi")), null));
        exports.add(new ExportTask(ExportMode.SEQUENCE, ExportTarget.FILE, Direction.Y, new File(target.concat("-Y-").concat(ext).replace("bmp", "avi")), null));

        try {
            long time = System.nanoTime();
            ComplextTaskSolver.solveComplexTask(tc);
            time = System.nanoTime() - time;
            Logger.info("Finished dynamic task " + SIZE_DYN + "/" + tc.getParameter(TaskParameter.KERNEL) + " in " + (time / 1000000.0) + "ms.");

            for (ExportTask et : exports) {
                Exporter.export(et, tc);
            }
        } catch (ComputationException ex) {
            Logger.error(ex);
        }

        final File parent;
        if (in instanceof File) {
            parent = ((File) in).getParentFile();
        } else if (in instanceof List) {
            final List<File> inL = (List<File>) in;
            parent = inL.get(0).getParentFile();
        } else {
            throw new IllegalArgumentException("Unsupported type of input - " + in);
        }

        final File input = (File) tc.getParameter(TaskParameter.IN);
        TaskContainerUtils.serializeTaskContainerToConfig(tc);
        TaskContainer loadedTc = TaskContainerUtils.deserializeTaskContainerFromConfig((File) in);
//        System.out.println(tc);
//        System.out.println(loadedTc);
    }

}
