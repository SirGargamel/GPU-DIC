package cz.tul.dic;

import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.TaskContainer;
import cz.tul.dic.data.TaskParameter;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.generators.DeformationGenerator;
import cz.tul.dic.generators.FacetGenerator;
import cz.tul.dic.generators.FacetGeneratorMode;
import cz.tul.dic.input.InputLoader;
import cz.tul.dic.output.ExportMode;
import cz.tul.dic.output.ExportTarget;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author Petr Jecmen
 */
public class Computation {
    
    private static final File IN = new File("d:\\temp\\image.avi");

    public static void commenceComputation() throws IOException {
        final TaskContainer tc = new TaskContainer();                
        
        // load input data
        InputLoader.loadInput(IN, tc);
        
        // select ROI        
        tc.addRoi(new ROI(0,0,320,240), 0);
        
        // select facet size
        tc.setFacetSize(5);
        
        // add outputs
        tc.addExportTask(new ExportTask(ExportMode.MAP, ExportTarget.IMAGE, new Object[] {0, new File("D:\\test.bmp")}));
        tc.addExportTask(new ExportTask(ExportMode.MAP, ExportTarget.CSV, new Object[] {0, new File("D:\\testMap.csv")}));
        tc.addExportTask(new ExportTask(ExportMode.LINE, ExportTarget.CSV, new Object[] {0, 20, 20, new File("D:\\testLine.csv")}));
        tc.addExportTask(new ExportTask(ExportMode.MAP, ExportTarget.AVI, new Object[] {new File("D:\\test.avi")}));
        
        // generate facets
        tc.addParameter(TaskParameter.FACET_GENERATOR_MODE, FacetGeneratorMode.CLASSIC);        
        System.err.println("TODO TightModeFacetGenerator");
        FacetGenerator.generateFacets(tc);
        
        // generate deformations
        System.err.println("TODO DeformationGenerator");
        DeformationGenerator.generateDeformations(tc);
        
        // split to subtask according to deformations and limits
        System.err.println("TODO TaskSplitter");
        
        // compute task
        System.err.println("TODO Engine");
        Engine.computeTask(tc);
        
        // perform export
        System.err.println("TODO ExportImage");
        System.err.println("TODO ExportCsvMap");
        System.err.println("TODO ExportCsvLine");
        System.err.println("TODO ExportAVI");        
        Exporter.export(tc);
        
    }
    
}
