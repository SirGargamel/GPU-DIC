/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernel;

import com.jogamp.opencl.CLDevice;
import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.subset.SquareSubset2D;
import cz.tul.dic.data.task.FullTask;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskDefaultValues;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.engine.opencl.DeviceManager;
import cz.tul.dic.engine.opencl.kernel.KernelInfo.Type;
import cz.tul.dic.engine.opencl.solvers.AbstractTaskSolver;
import cz.tul.dic.engine.opencl.solvers.Solver;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class KernelManager {

    private static final KernelInfo DEFAULT_KERNEL, DEFAULT_KERNEL_WEIGHED;
    private static final List<KernelInfo> UNSUPPORTED_KERNELS;
    private static final String PERFORMANCE_TEST_TIME = "performance.time";
    private static final String PERFORMANCE_DEVICE = "performance.device";
    private static final Map<Long, Map<Long, Map<KernelInfo, Long>>> PERFORMANCE_TIME_DATA;
    private static final double[][] PERFORMANCE_TEST_LIMITS = new double[][]{
        // deformation counts - BruteForce - 441, SPGD - 3, NRC - 21, 160, NRCHE - 5, 13, NRF - 6, 28, NRFHE - 3, 7        
        {-1, 1, 1, 0, 0, 0}, // 3
        {-3, 3, 1, -1, 1, 1}, // 21
        {0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, -2, 2, 1}, // 160
        {-10.0, 10.0, 1, -10, 10, 1}, // 441
    };
    private static final int[] PERFORMANCE_TEST_SUBSET_COUNT = new int[]{1, 32};
    private static final int PERFORMANCE_TEST_SUBSET_SIZE = 10;
    private static final int PERFORMANCE_TEST_BLANK_COUNT = 10;
    private static boolean inited;

    static {
        inited = false;

        DEFAULT_KERNEL = new KernelInfo(Type.BEST, KernelInfo.Input.BEST, KernelInfo.Correlation.BEST, KernelInfo.MemoryCoalescing.BEST, KernelInfo.UseLimits.BEST);
        DEFAULT_KERNEL_WEIGHED = new KernelInfo(Type.BEST, KernelInfo.Input.BEST, KernelInfo.Correlation.WZNSSD, KernelInfo.MemoryCoalescing.BEST, KernelInfo.UseLimits.BEST);

        UNSUPPORTED_KERNELS = new ArrayList<>(2);
        UNSUPPORTED_KERNELS.addAll(generatePossibleInfos(new KernelInfo(Type.CL2D, KernelInfo.Input.BEST, KernelInfo.Correlation.BEST, KernelInfo.MemoryCoalescing.YES, KernelInfo.UseLimits.BEST)));
        UNSUPPORTED_KERNELS.addAll(generatePossibleInfos(new KernelInfo(Type.CL15D_pF, KernelInfo.Input.BEST, KernelInfo.Correlation.BEST, KernelInfo.MemoryCoalescing.YES, KernelInfo.UseLimits.BEST)));

        final long lastCheck = Preferences.userNodeForPackage(KernelManager.class).getLong(PERFORMANCE_TEST_TIME, 0);
        final long currentTime = System.currentTimeMillis();
        final long diff = currentTime - lastCheck;
        if (TimeUnit.MILLISECONDS.toDays(diff) < 7) {
            loadPerformanceAndPrepareDevice();
        }

        if (!inited) {
            runDevicePerformanceTest(currentTime);
        }

        PERFORMANCE_TIME_DATA = TimeDataStorage.getInstance().getFullDataBySize();
    }

    private static void loadPerformanceAndPrepareDevice() {
        inited = TimeDataStorage.getInstance().loadTimeDataFromFile();
        if (inited) {
            final String deviceName = Preferences.userNodeForPackage(KernelManager.class).get(PERFORMANCE_DEVICE, null);
            if (deviceName == null) {
                inited = false;
            } else {
                boolean deviceFound = false;
                for (CLDevice device : DeviceManager.listAllDevices()) {
                    if (device.getName().trim().equals(deviceName)) {
                        DeviceManager.initContext(device);
                        deviceFound = true;
                        break;
                    }
                }
                inited = deviceFound;
            }
        }
    }

    private static void runDevicePerformanceTest(final long current) throws RuntimeException {
        Logger.debug("Initializing best kernel.");
        final AbstractTaskSolver solver = AbstractTaskSolver.initSolver(Solver.BRUTE_FORCE);
        solver.setInterpolation(TaskDefaultValues.DEFAULT_INTERPOLATION);
        solver.setTaskSplitVariant(TaskSplitMethod.NONE, null);

        final List<CLDevice> devices = DeviceManager.listAllDevices();
        final SortedMap<Map<KernelInfo, Long>, CLDevice> times = new TreeMap<>((Map<KernelInfo, Long> o1, Map<KernelInfo, Long> o2) -> {
            if (o1.isEmpty()) {
                return 1;
            }
            if (o2.isEmpty()) {
                return -1;
            }

            int countO1 = 0;
            int countO2 = 0;
            for (KernelInfo ki : o1.keySet()) {
                if (o1.get(ki) < o2.get(ki)) {
                    countO1++;
                } else if (o1.get(ki) > o2.get(ki)) {
                    countO2++;
                }
            }

            return Long.compare(countO2, countO1);
        });

        try {
            final Random rnd = new Random();
            final List<KernelInfo> infos = generateKernelInfos();

            Map<KernelInfo, Long> timeTable;
            long time;
            for (CLDevice device : devices) {
                System.out.println("Testing " + device.toString());
                TimeDataStorage.getInstance().reset();
                DeviceManager.initContext(device);

                // run blank test for device preparation
                for (int i = 0; i < PERFORMANCE_TEST_BLANK_COUNT; i++) {
                    testKernelInfo(solver, infos.get(rnd.nextInt(infos.size())));
                }

                // commence testing
                time = System.currentTimeMillis();
                timeTable = runDeviceTest(solver);
                times.put(timeTable, device);

                time = System.currentTimeMillis() - time;
                System.out.println("Test for " + device.toString() + " took " + time + "ms.");
            }
        } catch (ComputationException ex) {
            Logger.debug(ex);
            throw new RuntimeException("Error initializing OpenCL.", ex);
        }
        solver.endTask();
        // pick best device
        final CLDevice bestDevice = times.values().toArray(new CLDevice[0])[0];

        // store results
        if (bestDevice != devices.get(devices.size() - 1)) {
            // run test again if the device was not the last on tested
            try {
                TimeDataStorage.getInstance().reset();
                DeviceManager.initContext(bestDevice);
                runDeviceTest(solver);
            } catch (ComputationException ex) {
                Logger.debug(ex);
                throw new RuntimeException("Error running retest for best device.", ex);
            }
        }
        TimeDataStorage.getInstance().storeTimeDataToFile();
        Preferences.userNodeForPackage(KernelManager.class).putLong(PERFORMANCE_TEST_TIME, current);
        Preferences.userNodeForPackage(KernelManager.class).put(PERFORMANCE_DEVICE, bestDevice.getName().trim());
        Logger.debug("Kernel performance assesment completed.");

        inited = true;
    }

    private static Map<KernelInfo, Long> runDeviceTest(final AbstractTaskSolver solver) throws ComputationException {
        final List<KernelInfo> infos = generateKernelInfos();
        final Map<KernelInfo, Long> result = new HashMap<>(infos.size());
        long time;
        for (KernelInfo ki : infos) {
            time = System.nanoTime();
            testKernelInfo(solver, ki);
            result.put(ki, System.nanoTime() - time);
        }

        return result;
    }

    private static void testKernelInfo(final AbstractTaskSolver solver, final KernelInfo kernelInfo) throws ComputationException {
        solver.setKernel(kernelInfo);
        final Image img = Image.createImage(new BufferedImage(50, 50, BufferedImage.TYPE_BYTE_GRAY));
        final AbstractSubset subset = new SquareSubset2D(PERFORMANCE_TEST_SUBSET_SIZE, 15, 15);

        List<double[]> deformationLimits;
        List<AbstractSubset> subsets;
        List<Integer> weights;
        for (int sc : PERFORMANCE_TEST_SUBSET_COUNT) {
            for (double[] limits : PERFORMANCE_TEST_LIMITS) {
                subsets = Collections.nCopies(sc, subset);
                deformationLimits = Collections.nCopies(sc, limits);
                weights = Collections.nCopies(sc, TaskContainerUtils.computeCorrelationWeight(PERFORMANCE_TEST_SUBSET_SIZE, TaskDefaultValues.DEFAULT_CORRELATION_WEIGHT));
                solver.solve(new FullTask(
                        img, img, subsets, weights, deformationLimits));
            }
        }
    }

    public static KernelInfo getBestKernel(final KernelInfo kernelInfo, final long deformationCount) {
        final List<KernelInfo> infos = generatePossibleInfos(kernelInfo);
        if (infos.size() == 1) {
            return infos.get(0);
        }

        final long subsetCount = 1;
        final Map<Long, Map<KernelInfo, Long>> data = PERFORMANCE_TIME_DATA.get(subsetCount);
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
        KernelInfo bestKernel = null, kernel;
        for (Entry<KernelInfo, Long> e : data.get(defCount).entrySet()) {
            kernel = e.getKey();
            if (!infos.contains(kernel)) {
                continue;
            }

            time = e.getValue();
            if (time < bestTime) {
                bestTime = time;
                bestKernel = kernel;
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
                    if (cor == KernelInfo.Correlation.BEST || cor == KernelInfo.Correlation.NO_WEIGHTS) {
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
        if (null != requestedKernelInfo.getCorrelation()) {
            switch (requestedKernelInfo.getCorrelation()) {
                case BEST:
                    correlations.addAll(Arrays.asList(KernelInfo.Correlation.values()));
                    correlations.remove(KernelInfo.Correlation.BEST);
                    correlations.remove(KernelInfo.Correlation.NO_WEIGHTS);
                    break;
                case NO_WEIGHTS:
                    correlations.add(KernelInfo.Correlation.ZNCC);
                    correlations.add(KernelInfo.Correlation.ZNSSD);
                    break;
                default:
                    correlations.add(requestedKernelInfo.getCorrelation());
                    break;
            }
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

    public static KernelInfo getBestKernel(final boolean usesWeights) {
        if (usesWeights) {
            return DEFAULT_KERNEL_WEIGHED;
        } else {
            return DEFAULT_KERNEL;
        }
    }

    public static boolean isInited() {
        return inited;
    }

}
