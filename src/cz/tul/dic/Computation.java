package cz.tul.dic;

import cz.tul.dic.data.Config;
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
import cz.tul.dic.generators.DeformationGenerator;
import cz.tul.dic.generators.facet.FacetGenerator;
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
import java.util.LinkedList;
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
    private static final File IN_VIDEO_REAL = new File("d:\\temp\\7202845m.avi");
    private static final File IN_VIDEO_ART = new File("d:\\temp\\image.avi");
    private static final List<File> IN_IMAGES;
    private static final File OUT_DIR = new File("D:\\temp\\results");
    private static final int SIZE_MIN = 3;
    private static final int SIZE_MAX = 35;
    private static final int SIZE_STEP = 1;
    private static final Set<ExportTask> exports;

    static {
        Configurator.defaultConfig().writer(new ConsoleWriter()).level(LOGGING_LEVEL).activate();

        IN_IMAGES = new LinkedList<>();
        IN_IMAGES.add(new File("d:\\temp\\image000.bmp"));
        IN_IMAGES.add(new File("d:\\temp\\image001.bmp"));
        IN_IMAGES.add(new File("d:\\temp\\image002.bmp"));
        IN_IMAGES.add(new File("d:\\temp\\image003.bmp"));
        IN_IMAGES.add(new File("d:\\temp\\image004.bmp"));

        exports = new HashSet<>();
    }

    public static void commenceComputation() throws IOException {
        final List<TaskContainer> tcs = new LinkedList<>();

        for (int size = SIZE_MIN; size <= SIZE_MAX; size += SIZE_STEP) {
//            for (KernelType kt : KernelType.values()) {
//                tcs.add(generateTask(IN_IMAGES, size, kt));
//            }            
//            tcs.add(generateTask(IN_IMAGES, size, KernelType.CL_1D_I_V_LL_MC));
            tcs.add(generateTask(IN_VIDEO_REAL, size, KernelType.CL_1D_I_V_LL_MC));
        }

        // compute task        
        final Engine engine = new Engine();

        long time;
        TaskContainer tc, loadedTc;
        Set<ExportTask> loadedExports;
        while (!tcs.isEmpty()) {
            tc = tcs.get(0);

            InputLoader.loadInput(tc);

            // generate exports
            generateExports(tc);
            Config.saveConfig("exportsConfig", OutputUtils.serializeExports(exports));
            loadedExports = OutputUtils.deserializeExports(Config.loadConfig("exportsConfig"));
//            System.out.println(loadedExports);
            
            Config.saveConfig("taskConfig", TaskContainerUtils.serializeTaskContainer(tc));
            loadedTc = TaskContainerUtils.deserializeTaskContainer(Config.loadConfig("taskConfig"));
//            System.out.println(loadedTc);

            TaskContainerChecker.checkTaskValidity(tc);

            FacetGenerator.generateFacets(tc);
            DeformationGenerator.generateDeformations(tc);

            time = System.nanoTime();
            engine.computeTask(tc);
            time = System.nanoTime() - time;
            for (ExportTask et : exports) {
                Exporter.export(et, tc);
            }
            Logger.info("Finished task " + tc.getFacetSize() + "/" + tc.getParameter(TaskParameter.KERNEL) + " in " + (time / 1000000.0) + "ms.");

            tcs.remove(0);
        }
        Logger.info("All done !!!");
    }

    private static TaskContainer generateTask(final Object in, final int facetSize, final KernelType kernelType) throws IOException {
        final TaskContainer tc = new TaskContainer(in);

        // select ROI 
        final ROI r1 = new RectangleROI(135, 19, 179, 200);
        tc.setRoi(r1, 0);
        final ROI r2 = new CircularROI(108, 101, 26);
        tc.setRoi(r2, 0);
        final ROI r3 = new CircularROI(203, 101, 26);
        tc.setRoi(r3, 0);

        // select facet size
        tc.setFacetSize(facetSize);

        // facets
        tc.setParameter(TaskParameter.FACET_GENERATOR_MODE, FacetGeneratorMode.TIGHT);
//        tc.setParameter(TaskParameter.FACET_GENERATOR_MODE, FacetGeneratorMode.CLASSIC);
        tc.setParameter(TaskParameter.FACET_GENERATOR_SPACING, 2);

        // deformations
        tc.setDeformationLimits(new double[]{-1, 1, 0.5, -5, 5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5}, 0, r1);
        tc.setDeformationLimits(new double[]{-10, 0, 0.5, -10, 0, 0.5}, 0, r2);
        tc.setDeformationLimits(new double[]{-10, 0, 0.5, -10, 0, 0.5}, 0, r3);

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
        for (ROI roi : tc.getRoi(0)) {
            if (roi instanceof CircularROI) {
                circular.add(roi);
            } else {
                rect.add(roi);
            }
        }
        
        
        final String target = OUT_DIR.getAbsolutePath().concat(File.separator);
        final String ext = String.format("%02d", tc.getFacetSize()).concat("-").concat(tc.getParameter(TaskParameter.KERNEL).toString()).concat(".bmp");
        for (int round = 0; round < TaskContainerUtils.getRoundCount(tc); round++) {
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.X, new File(target.concat(String.format("%02d", round)).concat("-X-").concat(ext)), new int[] {0}));
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.Y, new File(target.concat(String.format("%02d", round)).concat("-Y-").concat(ext)), new int[] {0}));
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.ABS, new File(target.concat(String.format("%02d", round)).concat("-ABS-").concat(ext)), new int[] {0}));
            
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.DX, new File(target.concat(String.format("%02d", round)).concat("-DX-").concat(ext)), new int[] {0}));
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.DY, new File(target.concat(String.format("%02d", round)).concat("-DY-").concat(ext)), new int[] {0}));
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.DABS, new File(target.concat(String.format("%02d", round)).concat("-DABS-").concat(ext)), new int[] {0}));
            
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.ABS, new File(target.concat(String.format("%02d", round)).concat("-ABS-RC-").concat(ext)), new int[] {0}, circular.toArray(new ROI[0])));
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.DABS, new File(target.concat(String.format("%02d", round)).concat("-DABS-RC-").concat(ext)), new int[] {0}, circular.toArray(new ROI[0])));
            
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.ABS, new File(target.concat(String.format("%02d", round)).concat("-ABS-RR-").concat(ext)), new int[] {0}, rect.toArray(new ROI[0])));
            exports.add(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.DABS, new File(target.concat(String.format("%02d", round)).concat("-DABS-RR-").concat(ext)), new int[] {0}, rect.toArray(new ROI[0])));
        }
    }

}
