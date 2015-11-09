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
import cz.tul.dic.engine.opencl.solvers.AbstractTaskSolver;
import cz.tul.dic.engine.opencl.solvers.Solver;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class KernelManager {

    private static final KernelInfo BEST_KERNEL;
    private static boolean inited;

    static {
        inited = false;

        Logger.debug("Initializing best kernel.");

        final AbstractTaskSolver solver = AbstractTaskSolver.initSolver(Solver.COARSE_FINE);
        solver.setInterpolation(TaskDefaultValues.DEFAULT_INTERPOLATION);
        solver.setTaskSplitVariant(TaskDefaultValues.DEFAULT_TASK_SPLIT_METHOD, TaskDefaultValues.DEFAULT_TASK_SPLIT_PARAMETER);

        try {
            for (KernelType kt : KernelType.values()) {
                if (kt.isSafeToUse()) {
                    testKernelInfo(solver, new KernelInfo(kt, true));
                    testKernelInfo(solver, new KernelInfo(kt, false));
                }
            }
        } catch (ComputationException ex) {
            Logger.debug(ex);
            throw new RuntimeException("Error initializing OpenCL.", ex);
        }
        solver.endTask();
        // find best performing kernel        
        final Map<KernelInfo, Map<Long, Map<Long, Long>>> TIME_DATA = WorkSizeManager.getTimeData();
        double performance;
        double bestPerformance = Double.NEGATIVE_INFINITY;
        KernelInfo bestKernel = null;
        for (KernelInfo ki : TIME_DATA.keySet()) {
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
        BEST_KERNEL = bestKernel;
        Logger.debug("{} selected as best kernel.", BEST_KERNEL);

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

    public static List<KernelInfo> generateKernelInfos() {
        final ArrayList<KernelInfo> result = new ArrayList<>();

        for (KernelType kt : KernelType.values()) {
            result.add(new KernelInfo(kt, true));
            result.add(new KernelInfo(kt, false));
        }

        return result;
    }

    public static KernelInfo getBestKernel() {
        return BEST_KERNEL;
    }

    public static boolean isInited() {
        return inited;
    }

}
