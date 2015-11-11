/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.solvers;

import cz.tul.dic.data.result.CorrelationResult;
import com.jogamp.opencl.CLException;
import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskDefaultValues;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.data.task.splitter.AbstractTaskSplitter;
import cz.tul.dic.data.task.FullTask;
import cz.tul.dic.engine.opencl.DeviceManager;
import cz.tul.dic.engine.opencl.kernels.Kernel;
import cz.tul.dic.data.Interpolation;
import cz.tul.dic.engine.opencl.kernels.info.KernelInfo;
import cz.tul.dic.engine.opencl.kernels.KernelManager;
import cz.tul.dic.engine.opencl.memory.AbstractOpenCLMemoryManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public abstract class AbstractTaskSolver extends Observable {

    final AbstractOpenCLMemoryManager memManager;
    // dynamic
    KernelInfo kernelType;
    Interpolation interpolation;
    TaskSplitMethod taskSplitVariant;
    Kernel kernel;
    int subsetSize;
    Object taskSplitValue;
    boolean stop;
    // logging
    private final Map<AbstractSubset, ComputationInfo> computationInfo;

    protected AbstractTaskSolver() {
        memManager = AbstractOpenCLMemoryManager.getInstance();

        kernelType = KernelManager.getBestKernel();
        interpolation = TaskDefaultValues.DEFAULT_INTERPOLATION;
        taskSplitVariant = TaskDefaultValues.DEFAULT_TASK_SPLIT_METHOD;
        taskSplitValue = null;

        computationInfo = new HashMap<>();
    }

    public static AbstractTaskSolver initSolver(final Solver type) {
        try {
            final Class<?> cls = Class.forName("cz.tul.dic.engine.opencl.solvers.".concat(type.getClassName()));
            return (AbstractTaskSolver) cls.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            Logger.warn("Error instantiating class {}, using default correlation calculator.", type);
            Logger.error(ex);
            return new NewtonRaphsonCentral();
        }
    }

    public void endTask() {
        memManager.clearMemory();
        kernel.clearMemory();
        DeviceManager.clearMemory();
    }

    public synchronized List<CorrelationResult> solve(
            final FullTask fullTask, int subsetSize) throws ComputationException {
        stop = false;
        computationInfo.clear();

        kernel = Kernel.createInstance(kernelType, memManager);
        Logger.trace("Kernel prepared - {}", kernel);

        this.subsetSize = subsetSize;

        long time = System.nanoTime();

        final List<CorrelationResult> result = solve(kernel, fullTask);

        time = System.nanoTime() - time;
        Logger.debug("Task [{}] computed in {}ms using {}.", fullTask, time / 1_000_000, getClass().getSimpleName());
        Logger.debug(dumpComputationInfo());

        kernel.clearMemory();

        return result;
    }

    public abstract List<CorrelationResult> solve(
            final Kernel kernel,
            final FullTask fullTask) throws ComputationException;

    protected abstract boolean needsBestResult();

    protected synchronized List<CorrelationResult> computeTask(
            final Kernel kernel, final FullTask fullTask) throws ComputationException {
        final List<CorrelationResult> result = new ArrayList<>(fullTask.getSubsets().size());
        DeformationDegree defDegree = null;
        for (int i = 0; i < fullTask.getSubsets().size(); i++) {
            result.add(null);
            defDegree = DeformationUtils.getDegreeFromLimits(fullTask.getDeformationLimits().get(i));
        }

        try {
            kernel.prepareKernel(subsetSize, defDegree, interpolation);

            final AbstractTaskSplitter ts = AbstractTaskSplitter.prepareSplitter(fullTask, taskSplitVariant, taskSplitValue);

            if (stop) {
                return result;
            }

            computeSubtasks(ts, result, kernel, fullTask);
        } catch (CLException ex) {
            memManager.clearMemory();
            Logger.debug(ex);
            throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, ex);
        }

        if (!stop) {
            Logger.trace("Found solution for {} subsets.", result.size());
        }

        return result;
    }

    private void computeSubtasks(AbstractTaskSplitter ts, final List<CorrelationResult> result, final Kernel kernel, final FullTask fullTask) throws ComputationException {
        Exception lastEx = null;
        boolean finished = false;
        while (ts.isSplitterReady() && !finished) {
            try {
                ComputationTask ct;
                CorrelationResult bestSubResult = null;
                while (ts.hasNext()) {
                    if (stop) {
                        return;
                    }
                    ct = ts.next();
                    ct.setResults(kernel.compute(ct, needsBestResult()));
                    // pick best results for this computation task and discard ct data
                    if (ct.isSubtask()) {
                        bestSubResult = pickBetterResult(bestSubResult, ct.getResults().get(0));
                    } else if (bestSubResult != null) {
                        bestSubResult = pickBetterResult(bestSubResult, ct.getResults().get(0));
                        // store result
                        final int globalSubsetIndex = fullTask.getSubsets().indexOf(ct.getSubsets().get(0));
                        if (globalSubsetIndex < 0) {
                            throw new IllegalArgumentException("Local subset not found in global registry.");
                        }
                        result.set(globalSubsetIndex, bestSubResult);
                        bestSubResult = null;
                    } else {
                        pickBestResultsForTask(ct, result, fullTask.getSubsets());
                    }
                }
                finished = true;
            } catch (ComputationException ex) {
                memManager.clearMemory();
                if (ex.getExceptionCause().equals(ComputationExceptionCause.MEMORY_ERROR)) {
                    Logger.warn(ex);
                    ts.signalTaskSizeTooBig();
                    ts = AbstractTaskSplitter.prepareSplitter(fullTask, taskSplitVariant, taskSplitValue);
                    lastEx = ex;
                } else {
                    throw ex;
                }
            }
        }
        if (!finished && lastEx != null) {
            memManager.clearMemory();
            throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, lastEx);
        }
    }

    private static CorrelationResult pickBetterResult(final CorrelationResult r1, final CorrelationResult r2) {
        final CorrelationResult result;
        if (r1 == null) {
            result = r2;
        } else if (Double.compare(r1.getQuality(), r2.getQuality()) == 0) {
            result = DeformationUtils.getAbs(r1.getDeformation()) < DeformationUtils.getAbs(r2.getDeformation()) ? r1 : r2;
        } else {
            result = r1.getQuality() > r2.getQuality() ? r1 : r2;
        }
        return result;
    }

    private static void pickBestResultsForTask(final ComputationTask task, final List<CorrelationResult> bestResults, final List<AbstractSubset> globalSubsets) throws ComputationException {
        final List<AbstractSubset> localSubsets = task.getSubsets();
        final int subsetCount = localSubsets.size();

        int globaSubsetIndex;
        final List<CorrelationResult> taskResults = task.getResults();
        for (int localSubsetIndex = 0; localSubsetIndex < subsetCount; localSubsetIndex++) {
            globaSubsetIndex = globalSubsets.indexOf(localSubsets.get(localSubsetIndex));
            if (globaSubsetIndex < 0) {
                throw new IllegalArgumentException("Local subset not found in global registry.");
            }

            if (localSubsetIndex >= taskResults.size()) {
                Logger.warn("No best value found for subset nr." + globaSubsetIndex);
                bestResults.set(globaSubsetIndex, new CorrelationResult(-1, null));
            } else {
                bestResults.set(globaSubsetIndex, taskResults.get(localSubsetIndex));
            }
        }
    }

    public void setKernel(KernelInfo kernel) {
        this.kernelType = kernel;
    }

    public void setInterpolation(Interpolation interpolation) {
        this.interpolation = interpolation;
    }

    public void setTaskSplitVariant(TaskSplitMethod taskSplitVariant, Object taskSplitValue) {
        this.taskSplitVariant = taskSplitVariant;
        this.taskSplitValue = taskSplitValue;
    }

    public void stop() {
        stop = true;
        if (kernel != null) {
            kernel.stopComputation();
        }
        endTask();
        Logger.debug("Stopping correlation counter.");
    }

    protected void addSubsetResult(final AbstractSubset subset, final CorrelationResult result) {
        getInfo(subset).addResult(result);
    }

    private ComputationInfo getInfo(final AbstractSubset subset) {
        ComputationInfo result = computationInfo.get(subset);
        if (result == null) {
            result = new ComputationInfo(subset);
            computationInfo.put(subset, result);
        }
        return result;
    }

    protected void addSubsetTerminationInfo(final AbstractSubset subset, final String info) {
        getInfo(subset).setTerminationInfo(info);
    }

    private String dumpComputationInfo() {
        final StringBuilder sb = new StringBuilder("Subset computation results:\n");
        for (ComputationInfo ci : computationInfo.values()) {
            sb.append(ci).append("\n");
        }
        sb.setLength(Math.max(0, sb.length() - "\n".length()));
        return sb.toString();
    }

}
