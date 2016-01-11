/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.solvers;

import com.jogamp.opencl.CLException;
import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Interpolation;
import cz.tul.dic.data.deformation.DeformationOrder;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.FullTask;
import cz.tul.dic.data.task.TaskDefaultValues;
import cz.tul.dic.data.task.splitter.AbstractTaskSplitter;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.debug.IGPUResultsReceiver;
import cz.tul.dic.engine.opencl.DeviceManager;
import cz.tul.dic.engine.opencl.kernel.Kernel;
import cz.tul.dic.engine.opencl.kernel.KernelInfo;
import cz.tul.dic.engine.opencl.kernel.KernelManager;
import cz.tul.dic.engine.opencl.memory.AbstractOpenCLMemoryManager;
import cz.tul.pj.journal.Journal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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

    private static final Map<Class, Solver> SOLVERS;
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
    protected Map<AbstractSubset, CorrelationResult> results;
    protected Map<AbstractSubset, double[]> deformations;
    protected Map<AbstractSubset, Integer> weights;
    protected List<AbstractSubset> subsetsToCompute;
    protected double[] gpuData;
    protected boolean usesWeights;
    // logging
    private final Map<AbstractSubset, ComputationInfo> computationInfo;
    private static final List<IGPUResultsReceiver> LISTENERS;

    static {
        LISTENERS = new LinkedList<>();

        SOLVERS = new HashMap<>();
        SOLVERS.put(BruteForce.class, Solver.BRUTE_FORCE);
        SOLVERS.put(CoarseFine.class, Solver.COARSE_FINE);
        SOLVERS.put(SPGD.class, Solver.SPGD);
        SOLVERS.put(NewtonRaphsonCentral.class, Solver.NEWTON_RHAPSON_CENTRAL);
        SOLVERS.put(NewtonRaphsonCentralHE.class, Solver.NEWTON_RHAPSON_CENTRAL_HE);
        SOLVERS.put(NewtonRaphsonForward.class, Solver.NEWTON_RHAPSON_FORWARD);
        SOLVERS.put(NewtonRaphsonForwardHE.class, Solver.NEWTON_RHAPSON_FORWARD_HE);
    }

    protected AbstractTaskSolver() {
        memManager = AbstractOpenCLMemoryManager.getInstance();

        final Solver solver = SOLVERS.get(this.getClass());
        kernelType = KernelManager.getBestKernel(solver.supportsWeighedCorrelation());

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
            Logger.warn(ex, "Error instantiating class {}, using default correlation calculator.", type);
            return new CoarseFine();
        }
    }

    public void endTask() {
        memManager.clearMemory();
        kernel.clearMemory();
        DeviceManager.clearMemory();
    }

    public synchronized List<CorrelationResult> solve(
            final FullTask fullTask) throws ComputationException {
        Journal.addDataEntry(fullTask, "Solving full task", "Using \"{0}\" solver.", getClass().getSimpleName());
        Journal.createSubEntry();

        stop = false;
        computationInfo.clear();

        this.fullTask = fullTask;

        final List<AbstractSubset> subsets = fullTask.getSubsets();
        if (subsets.isEmpty()) {
            return new ArrayList<>(0);
        }
        final int subsetCount = subsets.size();

        results = new LinkedHashMap<>(subsetCount);
        deformations = new ConcurrentHashMap<>(subsetCount);
        subsetsToCompute = Collections.synchronizedList(new ArrayList<>(subsets));

        weights = new ConcurrentHashMap<>(subsetCount);
        for (int i = 0; i < fullTask.getSubsets().size(); i++) {
            weights.put(fullTask.getSubsets().get(i), fullTask.getSubsetWeights().get(i));
        }

        kernel = Kernel.createInstance(kernelType, memManager, getDeformationCount());

        this.subsetSize = fullTask.getSubsets().get(0).getSize();
        usesWeights = kernel.getKernelInfo().getCorrelation() == KernelInfo.Correlation.WZNSSD;

        long time = System.nanoTime();

        final List<CorrelationResult> result = solve();

        kernel.clearMemory();

        time = System.nanoTime() - time;
        Journal.addDataEntry(computationInfo, "Full task solved", "Task completed in {0}ms.", time / 1_000_000);
        Journal.closeSubEntry();

        return result;
    }

    public abstract List<CorrelationResult> solve() throws ComputationException;

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
            throw new ComputationException(ComputationExceptionCause.OPENCL_ERROR, ex);
        }

        Journal.addEntry("Computation subtask completed.", "Found solution for {0} subsets.", taskResults.size());

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
        final boolean needsBestResult = needsBestResult();
        boolean finished = false;
        List<double[]> gpuDataList = new LinkedList<>();
        while (!finished) {
            try {
                ComputationTask ct;
                while (ts.hasNext()) {
                    if (stop) {
                        return;
                    }
                    ct = ts.next();
                    if (needsBestResult) {
                        ct.setResults(kernel.computeFindBest(ct));
                        // pick best results for this computation task and discard ct data                   
                        pickBestResultsForTask(ct, results, fullTask.getSubsets());
                    } else {
                        // TODO join gpuData in case of split computation and no best results
                        gpuDataList.add(kernel.computeRaw(ct));
                    }
                }
                finished = true;
            } catch (ComputationException ex) {
                memManager.clearMemory();
                gpuDataList.clear();
                throw ex;
            }
        }
        if (!gpuDataList.isEmpty() && !LISTENERS.isEmpty()) {
            if (gpuDataList.size() == 1) {
                gpuData = gpuDataList.get(0);
            } else {
                int length = 0;
                for (double[] dA : gpuDataList) {
                    length += dA.length;
                }
                gpuData = new double[length];
                int base = 0;
                int l;
                for (double[] dA : gpuDataList) {
                    l = dA.length;
                    System.arraycopy(dA, 0, gpuData, base, l);
                    base += l;
                }
            }

            for (IGPUResultsReceiver rr : LISTENERS) {
                rr.dumpGpuResults(gpuData, fullTask.getSubsets(), fullTask.getDeformations());
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

    public static void registerGPUDataListener(final IGPUResultsReceiver listener) {
        LISTENERS.add(listener);
        Logger.trace("Registering {} for GPU results.", listener);
    }

    public static void deregisterGPUDataListener(final IGPUResultsReceiver listener) {
        LISTENERS.remove(listener);
        Logger.trace("Deregistering {} for GPU results.", listener);
    }

}
