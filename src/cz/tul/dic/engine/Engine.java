/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine;

import cz.tul.dic.data.task.FullTask;
import cz.tul.dic.engine.opencl.solvers.AbstractTaskSolver;
import cz.tul.dic.ComputationException;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.roi.AbstractROI;
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
import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.data.result.DisplacementResult;
import cz.tul.dic.data.result.Result;
import cz.tul.dic.engine.strain.StrainEstimator;
import cz.tul.dic.engine.strain.StrainEstimationMethod;
import cz.tul.dic.data.subset.generator.FacetGenerator;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.data.ExportMode;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import cz.tul.dic.output.NameGenerator;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Ječmen
 */
public final class Engine extends Observable implements Observer {

    private static final Engine INSTANCE;
    private final ExecutorService exec;
    private StrainEstimator strain;
    private AbstractTaskSolver solver;
    private boolean stopEngine;

    static {
        INSTANCE = new Engine();
    }

    private Engine() {
        super();
        exec = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors() - 1);
    }

    public static Engine getInstance() {
        return INSTANCE;
    }

    public void computeTask(final TaskContainer tc) throws ComputationException {
        stopEngine = false;
        setChanged();
        notifyObservers(0);

        tc.clearResultData();
        TaskContainerUtils.checkTaskValidity(tc);

        strain = StrainEstimator.initStrainEstimator((StrainEstimationMethod) tc.getParameter(TaskParameter.STRAIN_ESTIMATION_METHOD));
        final Set<Future<Void>> futures = new HashSet<>();

        int r, nextR, baseR = -1;
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

            if (baseR == -1) {
                baseR = r;
            } else {
                futures.add(exec.submit(new OverlapComputation(tc, baseR, nextR, strain)));
            }

            try {
                exportRound(tc, r);
            } catch (IOException ex) {
                Logger.error(ex, "Round export failed.");
            }
        }

        Stats.getInstance().dumpDeformationsStatisticsUsage();
        Stats.getInstance().dumpDeformationsStatisticsPerQuality();

        try {
            setChanged();
            notifyObservers(StrainEstimator.class);
            for (Future f : futures) {
                f.get();
            }
        } catch (InterruptedException | ExecutionException ex) {
            Logger.warn(ex);
        }

        endTask();

        try {
            Exporter.export(tc);
        } catch (IOException ex) {
            Logger.error(ex, "Task export failed.");
        }
        try {
            TaskContainerUtils.serializeTaskToBinary(tc, new File(NameGenerator.generateBinary(tc)));
        } catch (IOException ex) {
            Logger.error(ex, "Task serialization to binary failed.");
        }
    }

    public void computeRound(final TaskContainer task, final int roundFrom, final int roundTo) throws ComputationException {
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
        solver = AbstractTaskSolver.initSolver((Solver) task.getParameter(TaskParameter.SOLVER));
        solver.addObserver(this);
        solver.setKernel((KernelType) task.getParameter(TaskParameter.KERNEL));
        solver.setInterpolation((Interpolation) task.getParameter(TaskParameter.INTERPOLATION));
        final TaskSplitMethod taskSplit = (TaskSplitMethod) task.getParameter(TaskParameter.TASK_SPLIT_METHOD);
        final Object taskSplitValue = task.getParameter(TaskParameter.TASK_SPLIT_PARAM);
        solver.setTaskSplitVariant(taskSplit, taskSplitValue);

        strain = StrainEstimator.initStrainEstimator((StrainEstimationMethod) task.getParameter(TaskParameter.STRAIN_ESTIMATION_METHOD));

        // prepare data
        setChanged();
        notifyObservers(FacetGenerator.class);
        final Map<AbstractROI, List<AbstractSubset>> subsets = FacetGenerator.generateFacets(task, roundFrom);

        // compute round                
        final Map<AbstractROI, List<CorrelationResult>> correlations = new HashMap<>(task.getRois(roundFrom).size());
        for (AbstractROI roi : task.getRois(roundFrom)) {
            if (stopEngine) {
                return;
            }
            // compute and store result
            setChanged();
            notifyObservers(AbstractTaskSolver.class);
            correlations.put(
                    roi,
                    solver.solve(
                            new FullTask(
                                    task.getImage(roundFrom), task.getImage(roundTo),
                                    subsets.get(roi),
                                    generateDeformations(task.getDeformationLimits(roundFrom, roi), subsets.get(roi).size())),
                            DeformationUtils.getDegreeFromLimits(task.getDeformationLimits(roundFrom, roi)),
                            task.getSubsetSize(roundFrom, roi)));
        }

        setChanged();
        notifyObservers(DisplacementCalculator.class);
        final DisplacementResult displacement = DisplacementCalculator.computeDisplacement(correlations, subsets, task, roundFrom);

        task.setResult(roundFrom, roundTo, new Result(correlations, displacement));

        final Future future = exec.submit(new OverlapComputation(task, roundFrom, roundTo, strain));

        if (DebugControl.isDebugMode()) {
            Stats.getInstance().dumpDeformationsStatisticsUsage(roundFrom);
            Stats.getInstance().dumpDeformationsStatisticsPerQuality(roundFrom);
            Stats.getInstance().drawFacetQualityStatistics(subsets, roundFrom, roundTo);
            Stats.getInstance().drawPointResultStatistics(roundFrom, roundTo);
        }

        try {
            setChanged();
            notifyObservers(StrainEstimator.class);
            future.get();
        } catch (InterruptedException | ExecutionException | NullPointerException ex) {
            Logger.warn(ex);
        }

        Logger.debug("Computed round {0}:{1}.", roundFrom, roundTo);

        setChanged();
        notifyObservers(System.currentTimeMillis() - time);

        solver.deleteObserver(this);
    }

    public void endTask() {
        if (solver != null) {
            solver.endTask();
        }
    }

    private static List<double[]> generateDeformations(final double[] limits, final int subsetCount) {
        return Collections.nCopies(subsetCount, limits);
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
        return dir == Direction.EABS || dir == Direction.EXY || dir == Direction.EXX || dir == Direction.EYY;
    }

    public void stop() {
        stopEngine = true;
        solver.stop();
        strain.stop();
        exec.shutdownNow();
        Logger.debug("Stopping engine.");
    }

    @Override
    public void update(final Observable o, final Object arg) {
        if (o instanceof AbstractTaskSolver) {
            setChanged();
            notifyObservers(arg);
        } else {
            Logger.error("Illegal observable notification - " + o.toString());
        }
    }

    public ExecutorService getExecutorService() {
        return exec;
    }

}
