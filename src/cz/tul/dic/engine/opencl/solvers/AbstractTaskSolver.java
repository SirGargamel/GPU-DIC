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
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskDefaultValues;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.data.task.splitter.AbstractTaskSplitter;
import cz.tul.dic.data.task.FullTask;
import cz.tul.dic.engine.opencl.DeviceManager;
import cz.tul.dic.engine.opencl.kernel.Kernel;
import cz.tul.dic.data.Interpolation;
import cz.tul.dic.data.deformation.DeformationOrder;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.engine.opencl.kernel.KernelInfo;
import cz.tul.dic.engine.opencl.kernel.KernelManager;
import cz.tul.dic.engine.opencl.memory.AbstractOpenCLMemoryManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;
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
    // data storage
    protected FullTask fullTask;
    protected DeformationOrder deformationOrder;
    protected Map<AbstractSubset, CorrelationResult> results;
    protected Map<AbstractSubset, double[]> deformations;
    protected Map<AbstractSubset, Integer> weights;
    protected List<AbstractSubset> subsetsToCompute;
    protected double[] gpuData;
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

        this.fullTask = fullTask;
        deformationOrder = DeformationUtils.getOrderFromLimits(fullTask.getDeformationLimits().get(0));

        final List<AbstractSubset> subsets = fullTask.getSubsets();
        final int subsetCount = subsets.size();
        results = new LinkedHashMap<>(subsetCount);
        deformations = new ConcurrentHashMap<>(subsetCount);
        subsetsToCompute = Collections.synchronizedList(new ArrayList<>(subsets));

        weights = new ConcurrentHashMap<>(subsetCount);
        for (int i = 0; i < fullTask.getSubsets().size(); i++) {
            weights.put(fullTask.getSubsets().get(i), fullTask.getSubsetWeights().get(i));
        }

        kernel = Kernel.createInstance(kernelType, memManager, getDeformationCount());
        Logger.trace("Kernel prepared - {}", kernel);

        this.subsetSize = subsetSize;
        final boolean usesWeights = kernel.getKernelInfo().getCorrelation() == KernelInfo.Correlation.WZNSSD;

        long time = System.nanoTime();

        final List<CorrelationResult> result = solve(fullTask, usesWeights);

        time = System.nanoTime() - time;
        Logger.debug("Task [{}] computed in {}ms using {}.", fullTask, time / 1_000_000, getClass().getSimpleName());
        Logger.debug(dumpComputationInfo());

        kernel.clearMemory();

        return result;
    }

    public abstract List<CorrelationResult> solve(
            final FullTask fullTask,
            final boolean usesWeights) throws ComputationException;

    protected abstract boolean needsBestResult();
    
    public abstract long getDeformationCount();

    protected synchronized List<CorrelationResult> computeTask(
            final Kernel kernel, ComputationTask computationTask) throws ComputationException {
        final List<CorrelationResult> taskResults = new ArrayList<>(computationTask.getSubsets().size());
        for (int i = 0; i < computationTask.getSubsets().size(); i++) {
            taskResults.add(null);
        }

        try {            
            computationTask = adjustLimitsUse(kernel, computationTask);
            
            kernel.prepareKernel(subsetSize, computationTask.getOrder(), computationTask.usesLimits(), interpolation);

            final AbstractTaskSplitter ts = AbstractTaskSplitter.prepareSplitter(computationTask, taskSplitVariant, taskSplitValue);

            if (stop) {
                return taskResults;
            }

            computeSubtasks(ts, taskResults, kernel, computationTask);
        } catch (CLException ex) {
            memManager.clearMemory();
            Logger.debug(ex);
            throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, ex);
        }

        if (!stop) {
            Logger.trace("Found solution for {} subsets.", taskResults.size());
        }

        return taskResults;
    }
    
    private ComputationTask adjustLimitsUse(final Kernel kernel, final ComputationTask task) {                
        // ct coeffs - no change        
        // ct limits - 
        //      pick type according to kernel
        if (task.usesLimits() && kernel.getKernelInfo().getUseLimits() == KernelInfo.UseLimits.NO) {
            // generate deformation values
            final List<double[]> limits = task.getDeformations();
            final DeformationOrder order = task.getOrder();
            final List<double[]> deformationsFromLimits = DeformationUtils.generateDeformationsFromLimits(limits, order);
            return new ComputationTask(task.getImageA(), task.getImageB(), task.getSubsets(), task.getSubsetWeights(), deformationsFromLimits, order, false);
        } else {
            return task;
        }        
    }

    private void computeSubtasks(AbstractTaskSplitter ts, final List<CorrelationResult> results, final Kernel kernel, final ComputationTask fullTask) throws ComputationException {
        boolean finished = false;
        while (!finished) {
            try {
                ComputationTask ct;
                while (ts.hasNext()) {
                    if (stop) {
                        return;
                    }
                    ct = ts.next();
                    if (needsBestResult()) {
                        ct.setResults(kernel.compute(ct, true));
                        // pick best results for this computation task and discard ct data                   
                        pickBestResultsForTask(ct, results, fullTask.getSubsets());
                    } else {
                        // TODO join gpuData in case of split computation and no best results
                        kernel.compute(ct, false);
                    }                    
                }
                finished = true;
            } catch (ComputationException ex) {
                memManager.clearMemory();
                throw ex;
            }
        }
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

    protected void addSubsetResultInfo(final AbstractSubset subset, final CorrelationResult result) {
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
