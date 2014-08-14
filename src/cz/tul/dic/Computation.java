package cz.tul.dic;

import cz.tul.dic.complextask.ComplexTaskSolver;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.CumulativeResultsCounter;
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

    private static final File OUT_DIR = new File("D:\\temp\\results");

    public static void commenceComputation(TaskContainer tc) throws IOException, ComputationException {
        TaskContainerUtils.checkTaskValidity(tc);
        
        // displacement export
        tc.getExports().clear();
        for (int r : TaskContainerUtils.getRounds(tc).keySet()) {
            tc.addExport(ExportTask.generateMapExport(Direction.Dx, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Dx)), r));
            tc.addExport(ExportTask.generateMapExport(Direction.Dy, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Dy)), r));
            tc.addExport(ExportTask.generateMapExport(Direction.Dabs, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Dabs)), r));
        }
//        result.addExport(ExportTask.generateSequenceExport(Direction.Dabs, ExportTarget.FILE, generateTargetFile(true, null, in.getName(), facetSize, facetGenMode, Direction.Dabs)));

        long time = System.nanoTime();
        Engine.getInstance().computeTask(tc);
        time = System.nanoTime() - time;
        Logger.info("Finished task " + tc.getParameter(TaskParameter.FACET_SIZE) + "/" + tc.getParameter(TaskParameter.LOCAL_SEARCH_PARAM) + "/" + tc.getParameter(TaskParameter.KERNEL) + " in " + (time / 1000000.0) + "ms.");
        
        TaskContainerUtils.serializeTaskToBinary(tc, new File(NameGenerator.generateBinary(tc)));

        for (int r : TaskContainerUtils.getRounds(tc).keySet()) {
            tc.addExport(ExportTask.generateMapExport(Direction.cDx, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.cDx)), r));
            tc.addExport(ExportTask.generateMapExport(Direction.cDy, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.cDy)), r));
            tc.addExport(ExportTask.generateMapExport(Direction.cDabs, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.cDabs)), r));
        }
        Exporter.export(tc);
    }

    public static void commenceComputationDynamic(TaskContainer tc) throws IOException, ComputationException {
        TaskContainerUtils.checkTaskValidity(tc);

        final File in = (File) tc.getParameter(TaskParameter.IN);
        final int facetSize = (int) tc.getParameter(TaskParameter.FACET_SIZE);
        // displacement export
        tc.getExports().clear();
        for (int r : TaskContainerUtils.getRounds(tc).keySet()) {
            tc.addExport(ExportTask.generateMapExport(Direction.Dx, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Dx)), r));
            tc.addExport(ExportTask.generateMapExport(Direction.Dy, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Dy)), r));
            tc.addExport(ExportTask.generateMapExport(Direction.Dabs, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Dabs)), r));
        }
//        result.addExport(ExportTask.generateSequenceExport(Direction.Dabs, ExportTarget.FILE, generateTargetFile(true, null, in.getName(), facetSize, facetGenMode, Direction.Dabs)));

        long time = System.nanoTime();
        ComplexTaskSolver cts = new ComplexTaskSolver();
        cts.solveComplexTask(tc);
        time = System.nanoTime() - time;
        Logger.info("Finished dynamic task " + tc.getParameter(TaskParameter.FACET_SIZE) + "/" + tc.getParameter(TaskParameter.LOCAL_SEARCH_PARAM) + "/" + tc.getParameter(TaskParameter.KERNEL) + " in " + (time / 1000000.0) + "ms.");

        final StringBuilder sb = new StringBuilder();
        sb.append(OUT_DIR.getAbsolutePath());
        sb.append(File.separator);
        sb.append(in.getName());
        sb.append(File.separator);
        sb.append(String.format("%02d", facetSize));
        sb.append(".task");
        TaskContainerUtils.serializeTaskToBinary(tc, new File(sb.toString()));

        for (int r : TaskContainerUtils.getRounds(tc).keySet()) {
            tc.addExport(ExportTask.generateMapExport(Direction.cDx, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.cDx)), r));
            tc.addExport(ExportTask.generateMapExport(Direction.cDy, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.cDy)), r));
            tc.addExport(ExportTask.generateMapExport(Direction.cDabs, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.cDabs)), r));
        }
        Exporter.export(tc);
    }

    public static void commenceComputationDynamicStrainParamSweep(final TaskContainer tc, final int strainParamMin, final int strainParamMax) throws ComputationException, IOException {
        commenceComputationDynamic(tc);

        // strain sweep and export       
        final StrainEstimation strain = new StrainEstimation();
        for (int strainParam = strainParamMin; strainParam <= strainParamMax; strainParam++) {
            tc.setParameter(TaskParameter.STRAIN_ESTIMATION_PARAM, strainParam);
            strain.computeStrain(tc);
            tc.setCumulativeStrain(CumulativeResultsCounter.calculate(tc, tc.getStrains()));

            tc.getExports().clear();
            for (int r : TaskContainerUtils.getRounds(tc).keySet()) {
                tc.addExport(ExportTask.generateMapExport(Direction.cExx, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.cExx)), r));
                tc.addExport(ExportTask.generateMapExport(Direction.cEyy, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.cEyy)), r));
                tc.addExport(ExportTask.generateMapExport(Direction.cExy, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.cExy)), r));
                tc.addExport(ExportTask.generateMapExport(Direction.cEabs, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.cEabs)), r));
                tc.addExport(ExportTask.generateMapExport(Direction.Exx, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Exx)), r));
                tc.addExport(ExportTask.generateMapExport(Direction.Eyy, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Eyy)), r));
                tc.addExport(ExportTask.generateMapExport(Direction.Exy, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Exy)), r));
                tc.addExport(ExportTask.generateMapExport(Direction.Eabs, ExportTarget.FILE, new File(NameGenerator.generateMap(tc, r, Direction.Eabs)), r));
            }
//            result.addExport(ExportTask.generateSequenceExport(Direction.Eabs, ExportTarget.FILE, generateTargetFile(true, null, in.getName(), facetSize, strainParam, facetGenMode, Direction.Eabs)));
            Exporter.export(tc);

            TaskContainerUtils.serializeTaskToBinary(tc, new File(NameGenerator.generateBinary(tc)));
        }
    }

    public static void commenceComputationDynamicWindowSizeSweep(final TaskContainer tc, final int strainParamMin, final int strainParamMax, final int windowSize) throws ComputationException, IOException {
        tc.setParameter(TaskParameter.LOCAL_SEARCH_PARAM, windowSize);

        TaskContainerUtils.checkTaskValidity(tc);

        final File in = (File) tc.getParameter(TaskParameter.IN);
        final int facetSize = (int) tc.getParameter(TaskParameter.FACET_SIZE);
        // displacement export
        tc.getExports().clear();
        for (int r : TaskContainerUtils.getRounds(tc).keySet()) {            
            tc.addExport(ExportTask.generateMapExport(Direction.Dx, ExportTarget.FILE, generateTargetFile(r, facetSize, windowSize, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD), Direction.Dx), r));
            tc.addExport(ExportTask.generateMapExport(Direction.Dy, ExportTarget.FILE, generateTargetFile(r, facetSize, windowSize, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD), Direction.Dy), r));
            tc.addExport(ExportTask.generateMapExport(Direction.Dabs, ExportTarget.FILE, generateTargetFile(r, facetSize, windowSize, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD), Direction.Dabs), r));
        }

        long time = System.nanoTime();
        ComplexTaskSolver cts = new ComplexTaskSolver();
        cts.solveComplexTask(tc);
        time = System.nanoTime() - time;
        Logger.info("Finished dynamic task " + tc.getParameter(TaskParameter.FACET_SIZE) + "/" + tc.getParameter(TaskParameter.LOCAL_SEARCH_PARAM) + "/" + tc.getParameter(TaskParameter.KERNEL) + " in " + (time / 1000000.0) + "ms.");

        for (int r : TaskContainerUtils.getRounds(tc).keySet()) {
            tc.addExport(ExportTask.generateMapExport(Direction.cDx, ExportTarget.FILE, generateTargetFile(r, facetSize, windowSize, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD), Direction.cDx), r));
            tc.addExport(ExportTask.generateMapExport(Direction.cDy, ExportTarget.FILE, generateTargetFile(r, facetSize, windowSize, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD), Direction.cDy), r));
            tc.addExport(ExportTask.generateMapExport(Direction.cDabs, ExportTarget.FILE, generateTargetFile(r, facetSize, windowSize, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD), Direction.cDabs), r));            
        }
        Exporter.export(tc);

        // strain sweep and export   
        final StrainEstimation strain = new StrainEstimation();
        for (int strainParam = strainParamMin; strainParam <= strainParamMax; strainParam++) {
            tc.setParameter(TaskParameter.STRAIN_ESTIMATION_PARAM, strainParam);
            strain.computeStrain(tc);
            tc.setCumulativeStrain(CumulativeResultsCounter.calculate(tc, tc.getStrains()));

            tc.getExports().clear();
            for (int r : TaskContainerUtils.getRounds(tc).keySet()) {
                tc.addExport(ExportTask.generateMapExport(Direction.cExx, ExportTarget.FILE, generateTargetFile(r, facetSize, windowSize, strainParam, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD), Direction.cExx), r));
                tc.addExport(ExportTask.generateMapExport(Direction.cEyy, ExportTarget.FILE, generateTargetFile(r, facetSize, windowSize, strainParam, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD), Direction.cEyy), r));
                tc.addExport(ExportTask.generateMapExport(Direction.cExy, ExportTarget.FILE, generateTargetFile(r, facetSize, windowSize, strainParam, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD), Direction.cExy), r));
                tc.addExport(ExportTask.generateMapExport(Direction.cEabs, ExportTarget.FILE, generateTargetFile(r, facetSize, windowSize, strainParam, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD), Direction.cEabs), r));
                tc.addExport(ExportTask.generateMapExport(Direction.Exx, ExportTarget.FILE, generateTargetFile(r, facetSize, windowSize, strainParam, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD), Direction.Exx), r));
                tc.addExport(ExportTask.generateMapExport(Direction.Eyy, ExportTarget.FILE, generateTargetFile(r, facetSize, windowSize, strainParam, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD), Direction.Eyy), r));
                tc.addExport(ExportTask.generateMapExport(Direction.Exy, ExportTarget.FILE, generateTargetFile(r, facetSize, windowSize, strainParam, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD), Direction.Exy), r));
                tc.addExport(ExportTask.generateMapExport(Direction.Eabs, ExportTarget.FILE, generateTargetFile(r, facetSize, windowSize, strainParam, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_METHOD), Direction.Eabs), r));
            }
            Exporter.export(tc);

            final StringBuilder sb = new StringBuilder();
            sb.append(OUT_DIR.getAbsolutePath());
            sb.append(File.separator);
            sb.append(in.getName());
            sb.append(File.separator);
            sb.append(String.format("%02d", facetSize));
            sb.append("_");
            sb.append(String.format("%02d", windowSize));
            sb.append("_");
            sb.append(String.format("%02d", strainParam));
            sb.append(".task");
            TaskContainerUtils.serializeTaskToBinary(tc, new File(sb.toString()));
        }
    }

    private static File generateTargetFile(final Integer round, final Integer facetSize, final Integer strainParam, final Object... params) {
        final StringBuilder sb = new StringBuilder();
        sb.append(OUT_DIR.getAbsolutePath());
        sb.append(File.separator);

        for (Object o : params) {
            if (o instanceof Direction) {
                sb.setLength(sb.length() - 1);
                sb.append(File.separator);
            }

            sb.append(String.valueOf(o));
            sb.append(File.separator);
        }

        sb.setLength(sb.length() - 1);
        sb.append(File.separator);

        if (round != null) {
            sb.append(String.format("%02d", round));
            sb.append("_");
            sb.append(String.format("%02d", facetSize));
            sb.append("_");
            sb.append(String.format("%02d", strainParam));
            sb.append(".bmp");
        } else {
            sb.append("video.avi");
        }

        final File result = new File(sb.toString());
        Utils.ensureDirectoryExistence(result.getParentFile());
        return result;
    }

    private static File generateTargetFile(final Integer round, final Integer facetSize, final Integer windowSize, final Integer strainParam, final Object... params) {
        final StringBuilder sb = new StringBuilder();
        sb.append(OUT_DIR.getAbsolutePath());
        sb.append(File.separator);

        for (Object o : params) {
            if (o instanceof Direction) {
                sb.setLength(sb.length() - 1);
                sb.append(File.separator);
            }

            sb.append(String.valueOf(o));
            sb.append(File.separator);
        }

        sb.setLength(sb.length() - 1);
        sb.append(File.separator);

        if (round != null) {
            sb.append(String.format("%02d", round));
            sb.append("_");
            sb.append(String.format("%02d", facetSize));
            sb.append("_");
            sb.append(String.format("%02d", strainParam));
            sb.append("_");
            sb.append(String.format("%02d", windowSize));
            sb.append(".bmp");
        } else {
            sb.append("video.avi");
        }

        final File result = new File(sb.toString());
        Utils.ensureDirectoryExistence(result.getParentFile());
        return result;
    }

}
