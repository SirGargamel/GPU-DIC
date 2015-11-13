/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine;

import cz.tul.dic.data.task.FullTask;
import cz.tul.dic.engine.opencl.solvers.AbstractTaskSolver;
import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.roi.AbstractROI;
import cz.tul.dic.data.task.Hint;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.debug.DebugControl;
import cz.tul.dic.debug.Stats;
import cz.tul.dic.engine.displacement.DisplacementCalculator;
import cz.tul.dic.data.Interpolation;
import cz.tul.dic.engine.opencl.solvers.Solver;
import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.data.result.DisplacementResult;
import cz.tul.dic.data.result.Result;
import cz.tul.dic.data.subset.generator.AbstractSubsetGenerator;
import cz.tul.dic.engine.strain.StrainEstimator;
import cz.tul.dic.engine.strain.StrainEstimationMethod;
import cz.tul.dic.data.subset.generator.SubsetGenerator;
import cz.tul.dic.engine.opencl.kernels.info.KernelInfo;
import cz.tul.dic.engine.opencl.memory.AbstractOpenCLMemoryManager;
import cz.tul.dic.output.NameGenerator;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
        
        AbstractOpenCLMemoryManager.getInstance().assignTask(tc);

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
            TaskContainerUtils.serializeTaskToBinary(tc, new File(NameGenerator.generateBinary(tc)));
        } catch (IOException ex) {
            Logger.error(ex, "Task serialization to binary failed.");
        }
    }

    public void computeRound(final TaskContainer task, final int roundFrom, final int roundTo) throws ComputationException {
        stopEngine = false;

        final long time = System.currentTimeMillis();

        Logger.trace("Computing round {}:{} - {}.", roundFrom, roundTo, task);
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
        solver.setKernel((KernelInfo) task.getParameter(TaskParameter.KERNEL));
        solver.setInterpolation((Interpolation) task.getParameter(TaskParameter.INTERPOLATION));
        final TaskSplitMethod taskSplit = (TaskSplitMethod) task.getParameter(TaskParameter.TASK_SPLIT_METHOD);
        final Object taskSplitValue = task.getParameter(TaskParameter.TASK_SPLIT_PARAM);
        solver.setTaskSplitVariant(taskSplit, taskSplitValue);

        strain = StrainEstimator.initStrainEstimator((StrainEstimationMethod) task.getParameter(TaskParameter.STRAIN_ESTIMATION_METHOD));

        final int filterSize = (int) task.getParameter(TaskParameter.FILTER_KERNEL_SIZE);
        final Image in = task.getImage(roundFrom);        
        in.filter(filterSize);
        final Image out = task.getImage(roundTo);
        out.filter(filterSize);

        // prepare data
        setChanged();
        notifyObservers(SubsetGenerator.class);
        
        final AbstractSubsetGenerator generator = AbstractSubsetGenerator.initGenerator((SubsetGenerator) task.getParameter(TaskParameter.SUBSET_GENERATOR_METHOD));
        final HashMap<AbstractROI, List<AbstractSubset>> subsets = generator.generateSubsets(task, roundFrom);

        // compute round                
        final HashMap<AbstractROI, List<CorrelationResult>> correlations = new HashMap<>(task.getRois(roundFrom).size());
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
                                    in, out,
                                    subsets.get(roi),
                                    generateDeformations(task.getDeformationLimits(roundFrom, roi), subsets.get(roi).size())),
                            task.getSubsetSize(roundFrom, roi)));
        }

        setChanged();
        notifyObservers(DisplacementCalculator.class);
        final DisplacementResult displacement = DisplacementCalculator.computeDisplacement(correlations, subsets, task, roundFrom);

        task.setResult(roundFrom, roundTo, new Result(subsets, correlations, displacement));

        final Future future = exec.submit(new OverlapComputation(task, roundFrom, roundTo, strain));

        if (DebugControl.isDebugMode()) {
            Stats.getInstance().dumpDeformationsStatisticsUsage(roundFrom);
            Stats.getInstance().dumpDeformationsStatisticsPerQuality(roundFrom);
            Stats.getInstance().drawSubsetQualityStatistics(subsets, roundFrom, roundTo);
            Stats.getInstance().drawPointResultStatistics(roundFrom, roundTo);
        }

        try {
            setChanged();
            notifyObservers(StrainEstimator.class);
            future.get();
        } catch (InterruptedException | ExecutionException | NullPointerException ex) {
            Logger.warn(ex);
        }

        Logger.debug("Computed round {}:{}.", roundFrom, roundTo);

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
