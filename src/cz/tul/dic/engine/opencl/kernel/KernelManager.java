/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernel;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.subset.SquareSubset2D;
import cz.tul.dic.data.task.FullTask;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskDefaultValues;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.engine.opencl.kernel.KernelInfo.Type;
import cz.tul.dic.engine.opencl.solvers.AbstractTaskSolver;
import cz.tul.dic.engine.opencl.solvers.Solver;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class KernelManager {

    private static final String PREF_TIME_CREATION = "time.test";
    private static final KernelInfo DEFAULT_KERNEL = new KernelInfo(Type.BEST, KernelInfo.Input.BEST, KernelInfo.Correlation.BEST, KernelInfo.MemoryCoalescing.BEST, KernelInfo.UseLimits.BEST);
    private static final List<KernelInfo> UNSUPPORTED_KERNELS;
    private static final Map<Long, Map<Long, Map<KernelInfo, Long>>> TIME_DATA;
    private static final double[][] TEST_LIMITS = new double[][]{
        {-10.0, 10.0, 1, -10, 10, 1}, // BruteForce - 11*11
        {-1, 1, 1, 0, 0, 0}, // SPGD - 3
        {-3, 3, 1, -1, 1, 1}, {0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, -2, 2, 1}, // NRC - 21, 160
        {-2, 2, 1, 0, 0, 0}, {0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, -1, 1, 1, -1, 1, 1}, // NRCHE - 5, 13
        {-1, 1, 1, -1, 1, 1}, {0, 1, 1, 0, 1, 1, -3, 3, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // NRF - 6, 28
        {-3, 3, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // NRFHE - 3, 7
    };
    private static final int[] TEST_SUBSET_COUNT = new int[]{1, 32};
    private static final int TEST_SUBSET_SIZE = 10;
    private static boolean inited;

    static {
        inited = false;

        UNSUPPORTED_KERNELS = new ArrayList<>(2);
        UNSUPPORTED_KERNELS.addAll(generatePossibleInfos(new KernelInfo(Type.CL2D, KernelInfo.Input.BEST, KernelInfo.Correlation.BEST, KernelInfo.MemoryCoalescing.YES, KernelInfo.UseLimits.BEST)));
        UNSUPPORTED_KERNELS.addAll(generatePossibleInfos(new KernelInfo(Type.CL15D_pF, KernelInfo.Input.BEST, KernelInfo.Correlation.BEST, KernelInfo.MemoryCoalescing.YES, KernelInfo.UseLimits.BEST)));

        final long lastCheck = Preferences.userNodeForPackage(KernelManager.class).getLong(PREF_TIME_CREATION, 0);
        final long current = System.currentTimeMillis();
        final long diff = current - lastCheck;
        if (TimeUnit.MILLISECONDS.toDays(diff) == 0) {
            inited = TimeDataStorage.getInstance().loadTimeDataFromFile();
        }
        
        if (!inited) {
            Logger.debug("Initializing best kernel.");
            final AbstractTaskSolver solver = AbstractTaskSolver.initSolver(Solver.BRUTE_FORCE);
            solver.setInterpolation(TaskDefaultValues.DEFAULT_INTERPOLATION);
            solver.setTaskSplitVariant(TaskSplitMethod.NONE, null);

            try {
                for (KernelInfo ki : generateKernelInfos()) {
                    testKernelInfo(solver, ki);
                }
            } catch (ComputationException ex) {
                Logger.debug(ex);
                throw new RuntimeException("Error initializing OpenCL.", ex);
            }
            solver.endTask();
            TimeDataStorage.getInstance().storeTimeDataToFile();
            Preferences.userNodeForPackage(KernelManager.class).putLong(PREF_TIME_CREATION, current);
            Logger.debug("Kernel performance assesment completed.");

            inited = true;
        }

        TIME_DATA = TimeDataStorage.getInstance().getFullDataBySize();
    }

    private static void testKernelInfo(final AbstractTaskSolver solver, final KernelInfo kernelInfo) throws ComputationException {
        solver.setKernel(kernelInfo);
        final Image img = Image.createImage(new BufferedImage(50, 50, BufferedImage.TYPE_BYTE_GRAY));
        final AbstractSubset subset = new SquareSubset2D(TEST_SUBSET_SIZE, 15, 15);

        List<double[]> deformationLimits;
        List<AbstractSubset> subsets;
        List<Integer> weights;
        for (int sc : TEST_SUBSET_COUNT) {            
            for (double[] limits : TEST_LIMITS) {
                subsets = Collections.nCopies(sc, subset);
                deformationLimits = Collections.nCopies(sc, limits);
                weights = Collections.nCopies(sc, TaskContainerUtils.computeCorrelationWeight(TEST_SUBSET_SIZE, TaskDefaultValues.DEFAULT_CORRELATION_WEIGHT));
                solver.solve(
                        new FullTask(img, img, subsets, weights, deformationLimits),
                        TEST_SUBSET_SIZE);
            }
        }
    }

    public static KernelInfo getBestKernel(final KernelInfo kernelInfo, final long deformationCount) {
        final List<KernelInfo> infos = generatePossibleInfos(kernelInfo);
        if (infos.size() == 1) {
            return infos.get(0);
        }

        final long subsetCount = 1;
        final Map<Long, Map<KernelInfo, Long>> data = TIME_DATA.get(subsetCount);
        long defCount = 1;
        for (Long dc : data.keySet()) {
            if (dc < deformationCount) {
                defCount = dc;
            } else if (dc == deformationCount) {
                defCount = dc;
                break;
            } else {
                double difA = deformationCount - defCount;
                double difB = dc - deformationCount;
                if (difB < difA) {
                    defCount = dc;
                }
                break;
            }
        }

        // find best performing kernel                
        double time;
        double bestTime = Double.MAX_VALUE;
        KernelInfo bestKernel = null;
        for (Entry<KernelInfo, Long> e : data.get(defCount).entrySet()) {
            time = e.getValue();
            if (time < bestTime) {
                bestTime = time;
                bestKernel = e.getKey();
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

                        for (KernelInfo.UseLimits lim : KernelInfo.UseLimits.values()) {
                            if (lim == KernelInfo.UseLimits.BEST) {
                                continue;
                            }

                            kernelInfo = new KernelInfo(kt, in, cor, mc, lim);
                            if (!UNSUPPORTED_KERNELS.contains(kernelInfo)) {
                                result.add(kernelInfo);
                            }
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

        final List<KernelInfo.UseLimits> useLimits = new ArrayList<>();
        if (requestedKernelInfo.getUseLimits() == KernelInfo.UseLimits.BEST) {
            useLimits.addAll(Arrays.asList(KernelInfo.UseLimits.values()));
            useLimits.remove(KernelInfo.UseLimits.BEST);
        } else {
            useLimits.add(requestedKernelInfo.getUseLimits());
        }

        KernelInfo kernelInfo;
        for (Type kt : kernels) {
            for (KernelInfo.Input in : inputs) {
                for (KernelInfo.Correlation cor : correlations) {
                    for (KernelInfo.MemoryCoalescing mc : memoryCoalescing) {
                        for (KernelInfo.UseLimits lim : useLimits) {
                            kernelInfo = new KernelInfo(kt, in, cor, mc, lim);
                            if (!UNSUPPORTED_KERNELS.contains(kernelInfo)) {
                                result.add(kernelInfo);
                            }
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
