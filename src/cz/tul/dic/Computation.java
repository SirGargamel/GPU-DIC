package cz.tul.dic;

import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.engine.opencl.WorkSizeManager;
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

    static {
        IN_IMAGES = new LinkedList<>();

        IN_IMAGES.add(new File("d:\\temp\\image000.bmp"));
        IN_IMAGES.add(new File("d:\\temp\\image001.bmp"));
        IN_IMAGES.add(new File("d:\\temp\\image002.bmp"));
        IN_IMAGES.add(new File("d:\\temp\\image003.bmp"));
//        IN_IMAGES.add(new File("d:\\temp\\image004.bmp"));
    }

    public static void commenceComputation() throws IOException {
        final TaskContainer tc = new TaskContainer();

        // load input data
//        InputLoader.loadInput(IN_VIDEO_ART, tc);                
        InputLoader.loadInput(IN_IMAGES, tc);
//        InputLoader.loadInput(IN_VIDEO_REAL, tc);

        // select ROI        
        System.err.println("TODO Check ROI if valid, make smaller if needed.");
        System.err.println("TODO If no ROI provided, choose whole image.");
        tc.addRoi(new ROI(44, 49, 146, 109), 0);

        // select facet size
        tc.setFacetSize(10);

        // add outputs                
        tc.addExportTask(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.ABS, new File("D:\\test0.bmp"), 0));
        tc.addExportTask(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.ABS, new File("D:\\test1.bmp"), 1));
        tc.addExportTask(new ExportTask(ExportMode.MAP, ExportTarget.FILE, Direction.ABS, new File("D:\\test2.bmp"), 2));
        tc.addExportTask(new ExportTask(ExportMode.SEQUENCE, ExportTarget.FILE, Direction.ABS, new File("D:\\test.avi")));
//        tc.addExportTask(new ExportTask(ExportMode.MAP, ExportTarget.CSV, new Object[]{0, new File("D:\\testMap.csv")}));
//        tc.addExportTask(new ExportTask(ExportMode.LINE, ExportTarget.CSV, new Object[]{0, 20, 20, new File("D:\\testLine.csv")}));

        // generate facets
        tc.addParameter(TaskParameter.FACET_GENERATOR_MODE, FacetGeneratorMode.CLASSIC);
        System.err.println("TODO TightModeFacetGenerator");
        FacetGenerator.generateFacets(tc);

        // generate deformations
        tc.addParameter(TaskParameter.DEFORMATION_DEGREE, DeformationDegree.ZERO);
        tc.addParameter(TaskParameter.DEFORMATION_BOUNDS, new double[]{-5, 5, 0.5, -5, 5, 0.5});
//        tc.addParameter(TaskParameter.DEFORMATION_DEGREE, DeformationDegree.FIRST);
//        tc.addParameter(TaskParameter.DEFORMATION_BOUNDS, new double[] {-2, 2, 0.5, -5, 5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5, -0.5, 0.5, 0.5});        
        DeformationGenerator.generateDeformations(tc);

        // split to subtask according to deformations and limits
        System.err.println("TODO TaskSplitter");

        // compute task
        System.err.println("TODO Precompute ideal work size");
        final WorkSizeManager wsm = new WorkSizeManager();
        final Engine engine = new Engine(wsm);
        engine.computeTask(tc);

        // perform export                
        System.err.println("TODO Deformations coloring according to limits.");
        System.err.println("TODO ExportCsv");
        System.err.println("TODO ExportGUI");
        Exporter.export(tc);
    }

}
