/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
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
public final class Engine extends Observable {

    private static final Engine INSTANCE;
    private final StrainEstimation strain;
    private TaskSolver solver;
    private boolean stopEngine;

    static {
        INSTANCE = new Engine();
    }

    public static Engine getInstance() {
        return INSTANCE;
    }

    private Engine() {
        super();
        strain = new StrainEstimation();
    }

    public void computeTask(final TaskContainer tc) throws ComputationException, IOException {
        stopEngine = false;
        setChanged();
        notifyObservers(0);

        tc.clearResultData();
        TaskContainerUtils.checkTaskValidity(tc);

        final Set<Hint> hints = tc.getHints();
        int r, nextR;
        for (Map.Entry<Integer, Integer> e : TaskContainerUtils.getRounds(tc).entrySet()) {
            if (stopEngine) {
                endTask();
                return;
            }

            r = e.getKey();
            nextR = e.getValue();

            setChanged();
            notifyObservers(r);

            computeRound(tc, r, nextR);
            exportRound(tc, r);
        }

        Stats.getInstance().dumpDeformationsStatisticsUsage();
        Stats.getInstance().dumpDeformationsStatisticsPerQuality();

        if (!hints.contains(Hint.NO_STRAIN)) {
            if (stopEngine) {
                return;
            }
            setChanged();
            notifyObservers(StrainEstimation.class);
            strain.computeStrain(tc);
        }

        endTask();

        Exporter.export(tc);
        TaskContainerUtils.serializeTaskToBinary(tc, new File(NameGenerator.generateBinary(tc)));
    }

    public void computeRound(final TaskContainer task, final int roundFrom, final int roundTo) throws ComputationException, IOException {
        stopEngine = false;

        final long time = System.currentTimeMillis();

        Logger.trace("Computing round {0}:{1} - {2}.", roundFrom, roundTo, task);
        final Set<Hint> hints = task.getHints();
        if (hints.contains(Hint.NO_STATS)) {
            DebugControl.pauseDebugMode();
        } else {
            DebugControl.resumeDebugMode();
        }
        Stats.getInstance().setTaskContainer(task);

        setChanged();
        notifyObservers(TaskContainerUtils.class);
        TaskContainerUtils.checkTaskValidity(task);

        // prepare correlation calculator
        solver = TaskSolver.initSolver((Solver) task.getParameter(TaskParameter.SOLVER));
        solver.setKernel((KernelType) task.getParameter(TaskParameter.KERNEL));
        solver.setInterpolation((Interpolation) task.getParameter(TaskParameter.INTERPOLATION));
        final TaskSplitMethod taskSplit = (TaskSplitMethod) task.getParameter(TaskParameter.TASK_SPLIT_METHOD);
        final Object taskSplitValue = task.getParameter(TaskParameter.TASK_SPLIT_PARAM);
        solver.setTaskSplitVariant(taskSplit, taskSplitValue);

        // prepare data
        setChanged();
        notifyObservers(FacetGenerator.class);
        final Map<ROI, List<Facet>> facets = FacetGenerator.generateFacets(task, roundFrom);

        // compute round                
        for (ROI roi : task.getRois(roundFrom)) {
            if (stopEngine) {
                return;
            }
            // compute and store result
            setChanged();
            notifyObservers(TaskSolver.class);
            task.setResult(
                    roundFrom, roi,
                    solver.solve(
                            task.getImage(roundFrom), task.getImage(roundTo),
                            facets.get(roi),
                            generateDeformations(task.getDeformationLimits(roundFrom, roi), facets.get(roi).size()),
                            DeformationUtils.getDegreeFromLimits(task.getDeformationLimits(roundFrom, roi)),
                            task.getFacetSize(roundFrom, roi)));
        }

        setChanged();
        notifyObservers(DisplacementCalculator.class);
        DisplacementCalculator.computeDisplacement(task, roundFrom, roundTo, facets);

        if (DebugControl.isDebugMode()) {
            Stats.getInstance().dumpDeformationsStatisticsUsage(roundFrom);
            Stats.getInstance().dumpDeformationsStatisticsPerQuality(roundFrom);
            Stats.getInstance().drawFacetQualityStatistics(facets, roundFrom, roundTo);
            Stats.getInstance().drawPointResultStatistics(roundFrom, roundTo);
        }

        Logger.debug("Computed round {0}:{1}.", roundFrom, roundTo);

        setChanged();
        notifyObservers(System.currentTimeMillis() - time);
    }

    public void endTask() {
        if (solver != null) {
            solver.endTask();
        }
    }

    private static List<double[]> generateDeformations(final double[] limits, final int facetCount) {
        return Collections.nCopies(facetCount, limits);
    }

    private void exportRound(final TaskContainer task, final int round) throws IOException, ComputationException {
        final Iterator<ExportTask> it = task.getExports().iterator();
        ExportTask eTask;
        while (it.hasNext()) {
            eTask = it.next();
            if (eTask.getMode().equals(ExportMode.MAP) && eTask.getDataParams()[0] == round && !isStrainExport(eTask)) {
                Exporter.export(task, eTask);
            }
        }
    }

    private boolean isStrainExport(final ExportTask eTask) {
        final Direction dir = eTask.getDirection();
        return dir == Direction.Eabs || dir == Direction.Exy || dir == Direction.Exx || dir == Direction.Eyy;
    }

    public void stop() {
        stopEngine = true;
        solver.stop();
        strain.stop();
        Logger.debug("Stopping engine.");
    }

}
