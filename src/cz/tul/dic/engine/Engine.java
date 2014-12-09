package cz.tul.dic.engine;

import cz.tul.dic.engine.opencl.solvers.TaskSolver;
import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.Hint;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.debug.DebugControl;
import cz.tul.dic.debug.Stats;
import cz.tul.dic.engine.displacement.DisplacementCalculator;
import cz.tul.dic.engine.opencl.kernels.KernelType;
import cz.tul.dic.engine.opencl.interpolation.Interpolation;
import cz.tul.dic.engine.opencl.solvers.Solver;
import cz.tul.dic.engine.strain.StrainEstimation;
import cz.tul.dic.generators.facet.FacetGenerator;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.data.ExportMode;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import cz.tul.dic.output.NameGenerator;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class Engine extends Observable {

    private static final Engine instance;
    private final StrainEstimation strain;
    private TaskSolver correlation;
    private boolean stop;

    static {
        instance = new Engine();
    }

    public static Engine getInstance() {
        return instance;
    }

    private Engine() {
        strain = new StrainEstimation();
    }

    public void computeTask(final TaskContainer tc) throws ComputationException, IOException {
        stop = false;
        setChanged();
        notifyObservers(0);

        tc.clearResultData();
        TaskContainerUtils.checkTaskValidity(tc);

        final Set<Hint> hints = tc.getHints();
        int r, nextR;
        for (Map.Entry<Integer, Integer> e : TaskContainerUtils.getRounds(tc).entrySet()) {
            if (stop) {
                return;
            }

            r = e.getKey();
            nextR = e.getValue();
            
            setChanged();
            notifyObservers(r);

            computeRound(tc, r, nextR);
            exportRound(tc, r);            
        }

        Stats.dumpDeformationsStatisticsUsage();
        Stats.dumpDeformationsStatisticsPerQuality();

        if (!hints.contains(Hint.NO_STRAIN)) {
            if (stop) {
                return;
            }
            setChanged();
            notifyObservers(StrainEstimation.class);
            strain.computeStrain(tc);
        }

        Exporter.export(tc);
        TaskContainerUtils.serializeTaskToBinary(tc, new File(NameGenerator.generateBinary(tc)));
    }

    public void computeRound(final TaskContainer tc, final int roundFrom, final int roundTo) throws ComputationException, IOException {
        stop = false;

        long time = System.currentTimeMillis();

        Logger.trace("Computing round {0}:{1} - {2}.", roundFrom, roundTo, tc);
        final Set<Hint> hints = tc.getHints();
        if (hints.contains(Hint.NO_STATS)) {
            DebugControl.pauseDebugMode();
        } else {
            DebugControl.resumeDebugMode();
        }
        Stats.setTaskContainer(tc);

        setChanged();
        notifyObservers(TaskContainerUtils.class);
        TaskContainerUtils.checkTaskValidity(tc);

        // prepare correlation calculator
        correlation = TaskSolver.initSolver((Solver) tc.getParameter(TaskParameter.SOLVER));
        correlation.setKernel((KernelType) tc.getParameter(TaskParameter.KERNEL));
        correlation.setInterpolation((Interpolation) tc.getParameter(TaskParameter.INTERPOLATION));
        final TaskSplitMethod taskSplit = (TaskSplitMethod) tc.getParameter(TaskParameter.TASK_SPLIT_METHOD);
        final Object taskSplitValue = tc.getParameter(TaskParameter.TASK_SPLIT_PARAM);
        correlation.setTaskSplitVariant(taskSplit, taskSplitValue);

        // prepare data
        setChanged();
        notifyObservers(FacetGenerator.class);
        final Map<ROI, List<Facet>> facets = FacetGenerator.generateFacets(tc, roundFrom);

        // compute round                
        for (ROI roi : tc.getRois(roundFrom)) {
            if (stop) {
                return;
            }
            // compute and store result
            setChanged();
            notifyObservers(TaskSolver.class);
            tc.setResult(
                    roundFrom, roi,
                    correlation.solve(
                            tc.getImage(roundFrom), tc.getImage(roundTo),
                            facets.get(roi),
                            generateDeformations(tc.getDeformationLimits(roundFrom, roi), facets.get(roi).size()),
                            DeformationUtils.getDegreeFromLimits(tc.getDeformationLimits(roundFrom, roi)),
                            tc.getFacetSize(roundFrom, roi)));
        }

        setChanged();
        notifyObservers(DisplacementCalculator.class);
        DisplacementCalculator.computeDisplacement(tc, roundFrom, roundTo, facets);

        if (DebugControl.isDebugMode()) {
            Stats.dumpDeformationsStatisticsUsage(roundFrom);
            Stats.dumpDeformationsStatisticsPerQuality(roundFrom);
            Stats.drawFacetQualityStatistics(facets, roundFrom, roundTo);
            Stats.drawPointResultStatistics(roundFrom, roundTo);
        }

        Logger.debug("Computed round {0}:{1}.", roundFrom, roundTo);

        setChanged();
        notifyObservers(System.currentTimeMillis() - time);
    }

    private static List<double[]> generateDeformations(final double[] limits, final int facetCount) {
        return Collections.nCopies(facetCount, limits);
    }

    private void exportRound(final TaskContainer tc, final int round) throws IOException, ComputationException {
        Iterator<ExportTask> it = tc.getExports().iterator();
        ExportTask et;
        while (it.hasNext()) {
            et = it.next();
            if (et.getMode().equals(ExportMode.MAP) && et.getDataParams()[0] == round && !isStrainExport(et)) {
                Exporter.export(tc, et);
            }
        }
    }

    private boolean isStrainExport(ExportTask et) {
        final Direction dir = et.getDirection();
        return dir == Direction.Eabs || dir == Direction.Exy || dir == Direction.Exx || dir == Direction.Eyy;
    }

    public void stop() {
        stop = true;
        correlation.stop();
        strain.stop();
        Logger.debug("Stopping engine.");
    }

}
