package cz.tul.dic;

import cz.tul.dic.complextask.ComplexTaskSolver;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.engine.strain.StrainEstimation;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.target.ExportTarget;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import cz.tul.dic.output.NameGenerator;
import java.io.File;
import java.io.IOException;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class Computation {

    public static void commenceComputation(TaskContainer tc) throws IOException, ComputationException {
        TaskContainerUtils.checkTaskValidity(tc);

        // displacement export
        tc.getExports().clear();
        for (int r : TaskContainerUtils.getRounds(tc).keySet()) {
            tc.addExport(ExportTask.generateMapExport(Direction.dDx, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.dDx)), r));
            tc.addExport(ExportTask.generateMapExport(Direction.dDy, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.dDy)), r));
            tc.addExport(ExportTask.generateMapExport(Direction.dDabs, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.dDabs)), r));
            tc.addExport(ExportTask.generateMapExport(Direction.Dx, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Dx)), r));
            tc.addExport(ExportTask.generateMapExport(Direction.Dy, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Dy)), r));
            tc.addExport(ExportTask.generateMapExport(Direction.Dabs, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Dabs)), r));
        }

        long time = System.nanoTime();
        Engine.getInstance().computeTask(tc);
        Exporter.export(tc);
        time = System.nanoTime() - time;
        Logger.info("Finished task " + tc.getParameter(TaskParameter.FACET_SIZE) + "/" + tc.getParameter(TaskParameter.LOCAL_SEARCH_PARAM) + "/" + tc.getParameter(TaskParameter.KERNEL) + " in " + (time / 1000000.0) + "ms.");
    }

    public static void commenceComputationDynamic(TaskContainer tc) throws IOException, ComputationException {
        TaskContainerUtils.checkTaskValidity(tc);

        // displacement export
        tc.getExports().clear();
        for (int r : TaskContainerUtils.getRounds(tc).keySet()) {
            tc.addExport(ExportTask.generateMapExport(Direction.dDx, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.dDx)), r));
            tc.addExport(ExportTask.generateMapExport(Direction.dDy, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.dDy)), r));
            tc.addExport(ExportTask.generateMapExport(Direction.dDabs, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.dDabs)), r));
            tc.addExport(ExportTask.generateMapExport(Direction.Dx, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Dx)), r));
            tc.addExport(ExportTask.generateMapExport(Direction.Dy, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Dy)), r));
            tc.addExport(ExportTask.generateMapExport(Direction.Dabs, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Dabs)), r));
        }

        long time = System.nanoTime();
        ComplexTaskSolver cts = new ComplexTaskSolver();
        cts.solveComplexTask(tc);
        Exporter.export(tc);
        time = System.nanoTime() - time;
        Logger.info("Finished dynamic task " + tc.getParameter(TaskParameter.FACET_SIZE) + "/" + tc.getParameter(TaskParameter.LOCAL_SEARCH_PARAM) + "/" + tc.getParameter(TaskParameter.KERNEL) + " in " + (time / 1000000.0) + "ms.");
    }

    public static void commenceComputationDynamicStrainParamSweep(final TaskContainer tc, final double strainParamMin, final double strainParamMax) throws ComputationException, IOException {
        commenceComputationDynamic(tc);

        // strain sweep and export       
        final StrainEstimation strain = new StrainEstimation();
        for (double strainParam = strainParamMin; strainParam <= strainParamMax; strainParam++) {
            tc.setParameter(TaskParameter.STRAIN_ESTIMATION_PARAM, strainParam);
            strain.computeStrain(tc);            

            tc.getExports().clear();
            for (int r : TaskContainerUtils.getRounds(tc).keySet()) {
                tc.addExport(ExportTask.generateMapExport(Direction.Exx, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Exx)), r));
                tc.addExport(ExportTask.generateMapExport(Direction.Eyy, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Eyy)), r));
                tc.addExport(ExportTask.generateMapExport(Direction.Exy, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Exy)), r));
                tc.addExport(ExportTask.generateMapExport(Direction.Eabs, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Eabs)), r));                
            }
            Exporter.export(tc);
            TaskContainerUtils.serializeTaskToBinary(tc, new File(NameGenerator.generateBinary(tc)));
        }
    }
}
