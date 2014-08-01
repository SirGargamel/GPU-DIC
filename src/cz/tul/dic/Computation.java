package cz.tul.dic;

import cz.tul.dic.complextask.ComplexTaskSolver;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerChecker;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.strain.StrainEstimator;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportTarget;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import java.io.File;
import java.io.IOException;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class Computation {

    private static final File OUT_DIR = new File("D:\\temp\\results");

    public static void computeDynamicTask(TaskContainer tc) throws IOException, ComputationException {
        TaskContainerChecker.checkTaskValidity(tc);
        
        final File in = (File) tc.getParameter(TaskParameter.IN);
        final int facetSize = (int) tc.getParameter(TaskParameter.FACET_SIZE);
        // displacement export
        tc.getExports().clear();
        for (int r : TaskContainerUtils.getRounds(tc).keySet()) {
            tc.addExport(ExportTask.generateMapExport(Direction.Dx, ExportTarget.FILE, generateTargetFile(r, facetSize, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_MODE), Direction.Dx), r));
            tc.addExport(ExportTask.generateMapExport(Direction.Dy, ExportTarget.FILE, generateTargetFile(r, facetSize, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_MODE), Direction.Dy), r));
            tc.addExport(ExportTask.generateMapExport(Direction.Dabs, ExportTarget.FILE, generateTargetFile(r, facetSize, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_MODE), Direction.Dabs), r));
        }
//        result.addExport(ExportTask.generateSequenceExport(Direction.Dabs, ExportTarget.FILE, generateTargetFile(true, null, in.getName(), facetSize, facetGenMode, Direction.Dabs)));

        long time = System.nanoTime();        
        ComplexTaskSolver cts = new ComplexTaskSolver();
        cts.solveComplexTask(tc);
        time = System.nanoTime() - time;
        Logger.info("Finished dynamic task " + tc.getParameter(TaskParameter.FACET_SIZE) + "/" + tc.getParameter(TaskParameter.KERNEL) + " in " + (time / 1000000.0) + "ms.");

        Exporter.export(tc);
    }

    public static void commenceComputationDynamicStrainParamSweep(final TaskContainer tc, final int strainParamMin, final int strainParamMax) throws ComputationException, IOException {
        computeDynamicTask(tc);

        // strain sweep and export
        final File in = (File) tc.getParameter(TaskParameter.IN);
        final int facetSize = (int) tc.getParameter(TaskParameter.FACET_SIZE);
        for (int strainParam = strainParamMin; strainParam <= strainParamMax; strainParam++) {
            tc.setParameter(TaskParameter.STRAIN_ESTIMATION_PARAMETER, strainParam);
            StrainEstimator.computeStrain(tc);

            tc.getExports().clear();
            for (int r : TaskContainerUtils.getRounds(tc).keySet()) {
                tc.addExport(ExportTask.generateMapExport(Direction.Exx, ExportTarget.FILE, generateTargetFile(r, facetSize, strainParam, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_MODE), Direction.Exx), r));
                tc.addExport(ExportTask.generateMapExport(Direction.Eyy, ExportTarget.FILE, generateTargetFile(r, facetSize, strainParam, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_MODE), Direction.Eyy), r));
                tc.addExport(ExportTask.generateMapExport(Direction.Exy, ExportTarget.FILE, generateTargetFile(r, facetSize, strainParam, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_MODE), Direction.Exy), r));
                tc.addExport(ExportTask.generateMapExport(Direction.Eabs, ExportTarget.FILE, generateTargetFile(r, facetSize, strainParam, in.getName(), tc.getParameter(TaskParameter.FACET_GENERATOR_MODE), Direction.Eabs), r));
            }
//            result.addExport(ExportTask.generateSequenceExport(Direction.Eabs, ExportTarget.FILE, generateTargetFile(true, null, in.getName(), facetSize, strainParam, facetGenMode, Direction.Eabs)));
            Exporter.export(tc);
        }
    }

    private static File generateTargetFile(final Integer round, final Integer facetSize, final Object... params) {
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
            sb.append(".bmp");
        } else {
            sb.append("video.avi");
        }

        final File result = new File(sb.toString());
        Utils.ensureDirectoryExistence(result.getParentFile());
        return result;
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

}
