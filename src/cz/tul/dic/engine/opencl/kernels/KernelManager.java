/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernels;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.subset.SquareSubset2D;
import cz.tul.dic.data.task.FullTask;
import cz.tul.dic.data.task.TaskDefaultValues;
import cz.tul.dic.engine.opencl.WorkSizeManager;
import cz.tul.dic.engine.opencl.kernels.KernelInfo.Type;
import cz.tul.dic.engine.opencl.solvers.AbstractTaskSolver;
import cz.tul.dic.engine.opencl.solvers.Solver;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class KernelManager {

    private static final KernelInfo DEFAULT_KERNEL = new KernelInfo(Type.BEST, KernelInfo.Input.BEST, KernelInfo.Correlation.BEST, KernelInfo.MemoryCoalescing.BEST);
    private static final List<KernelInfo> UNSUPPORTED_KERNELS;
    private static boolean inited;

    static {
        inited = false;

        UNSUPPORTED_KERNELS = new ArrayList<>();
        UNSUPPORTED_KERNELS.addAll(generatePossibleInfos(new KernelInfo(Type.CL2D, KernelInfo.Input.BEST, KernelInfo.Correlation.BEST, KernelInfo.MemoryCoalescing.YES)));
        UNSUPPORTED_KERNELS.addAll(generatePossibleInfos(new KernelInfo(Type.CL15D_pF, KernelInfo.Input.BEST, KernelInfo.Correlation.BEST, KernelInfo.MemoryCoalescing.YES)));

        Logger.debug("Initializing best kernel.");
        final AbstractTaskSolver solver = AbstractTaskSolver.initSolver(Solver.COARSE_FINE);
        solver.setInterpolation(TaskDefaultValues.DEFAULT_INTERPOLATION);
        solver.setTaskSplitVariant(TaskDefaultValues.DEFAULT_TASK_SPLIT_METHOD, TaskDefaultValues.DEFAULT_TASK_SPLIT_PARAMETER);

        try {
            for (KernelInfo ki : generateKernelInfos()) {
                testKernelInfo(solver, ki);
            }
        } catch (ComputationException ex) {
            Logger.debug(ex);
            throw new RuntimeException("Error initializing OpenCL.", ex);
        }
        solver.endTask();
        Logger.debug("Kernel performance assesment completed.");

        inited = true;
    }

    private static void testKernelInfo(final AbstractTaskSolver solver, final KernelInfo kernelInfo) throws ComputationException {
        solver.setKernel(kernelInfo);
        final Image img = Image.createImage(new BufferedImage(50, 50, BufferedImage.TYPE_BYTE_GRAY));
        final List<double[]> deformationLimits = new ArrayList<>(2);
        final double[] limits = new double[]{-49, 50, 0.05, -49, 50, 0.05};
        deformationLimits.add(limits);
        deformationLimits.add(limits);
        final int fs = 14;
        final List<AbstractSubset> subsets = new ArrayList<>(2);
        subsets.add(new SquareSubset2D(fs, 15, 15));
        subsets.add(new SquareSubset2D(fs, 15, 15));
        solver.solve(
                new FullTask(img, img, subsets, deformationLimits),
                fs);
    }

    public static KernelInfo getBestKernel(final KernelInfo kernelInfo) {
        final List<KernelInfo> infos = generatePossibleInfos(kernelInfo);
        if (infos.size() == 1) {
            return infos.get(0);
        }

        // find best performing kernel        
        final Map<KernelInfo, Map<Long, Map<Long, Long>>> TIME_DATA = WorkSizeManager.getTimeData();
        double performance;
        double bestPerformance = Double.NEGATIVE_INFINITY;
        KernelInfo bestKernel = null;
        for (KernelInfo ki : infos) {
            for (Map.Entry<Long, Map<Long, Long>> e : TIME_DATA.get(ki).entrySet()) {
                for (Map.Entry<Long, Long> e2 : e.getValue().entrySet()) {
                    performance = (e.getKey() * e2.getKey()) / (double) e2.getValue();
                    if (performance > bestPerformance) {
                        bestPerformance = performance;
                        bestKernel = ki;
                    }
                }
            }
        }
        return bestKernel;
    }

    public static List<KernelInfo> generateKernelInfos() {
        final ArrayList<KernelInfo> result = new ArrayList<>();
        KernelInfo kernelInfo;

        for (KernelInfo.Type kt : KernelInfo.Type.values()) {
            if (kt == KernelInfo.Type.BEST) {
                continue;
            }

            for (KernelInfo.Input in : KernelInfo.Input.values()) {
                if (in == KernelInfo.Input.BEST) {
                    continue;
                }

                for (KernelInfo.Correlation cor : KernelInfo.Correlation.values()) {
                    if (cor == KernelInfo.Correlation.BEST) {
                        continue;
                    }

                    for (KernelInfo.MemoryCoalescing mc : KernelInfo.MemoryCoalescing.values()) {
                        if (mc == KernelInfo.MemoryCoalescing.BEST) {
                            continue;
                        }

                        kernelInfo = new KernelInfo(kt, in, cor, mc);
                        if (!UNSUPPORTED_KERNELS.contains(kernelInfo)) {
                            result.add(kernelInfo);
                        }
                    }
                }
            }
        }

        return result;
    }

    public static List<KernelInfo> generatePossibleInfos(final KernelInfo requestedKernelInfo) {
        final ArrayList<KernelInfo> result = new ArrayList<>();

        final List<Type> kernels = new ArrayList<>();
        if (requestedKernelInfo.getType() == Type.BEST) {
            kernels.addAll(Arrays.asList(Type.values()));
            kernels.remove(KernelInfo.Type.BEST);
        } else {
            kernels.add(requestedKernelInfo.getType());
        }

        final List<KernelInfo.Input> inputs = new ArrayList<>();
        if (requestedKernelInfo.getInput() == KernelInfo.Input.BEST) {
            inputs.addAll(Arrays.asList(KernelInfo.Input.values()));
            inputs.remove(KernelInfo.Input.BEST);
        } else {
            inputs.add(requestedKernelInfo.getInput());
        }

        final List<KernelInfo.Correlation> correlations = new ArrayList<>();
        if (requestedKernelInfo.getCorrelation() == KernelInfo.Correlation.BEST) {
            correlations.addAll(Arrays.asList(KernelInfo.Correlation.values()));
            correlations.remove(KernelInfo.Correlation.BEST);
        } else {
            correlations.add(requestedKernelInfo.getCorrelation());
        }

        final List<KernelInfo.MemoryCoalescing> memoryCoalescing = new ArrayList<>();
        if (requestedKernelInfo.getMemoryCoalescing() == KernelInfo.MemoryCoalescing.BEST) {
            memoryCoalescing.addAll(Arrays.asList(KernelInfo.MemoryCoalescing.values()));
            memoryCoalescing.remove(KernelInfo.MemoryCoalescing.BEST);
        } else {
            memoryCoalescing.add(requestedKernelInfo.getMemoryCoalescing());
        }

        KernelInfo kernelInfo;
        for (Type kt : kernels) {
            for (KernelInfo.Input in : inputs) {
                for (KernelInfo.Correlation cor : correlations) {
                    for (KernelInfo.MemoryCoalescing mc : memoryCoalescing) {
                        kernelInfo = new KernelInfo(kt, in, cor, mc);
                        if (!UNSUPPORTED_KERNELS.contains(kernelInfo)) {
                            result.add(kernelInfo);
                        }
                    }
                }
            }
        }

        return result;
    }

    public static KernelInfo getBestKernel() {
        return DEFAULT_KERNEL;
    }

    public static boolean isInited() {
        return inited;
    }

}
