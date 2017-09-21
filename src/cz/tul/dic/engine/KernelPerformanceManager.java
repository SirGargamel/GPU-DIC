/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine;

import cz.tul.dic.engine.platform.PlatformType;
import cz.tul.dic.engine.platform.PlatformManager;
import cz.tul.dic.engine.platform.Platform;
import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationUtils;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.subset.SquareSubset2D;
import cz.tul.dic.data.task.FullTask;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskDefaultValues;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.engine.KernelInfo.Type;
import cz.tul.dic.engine.platform.PlatformDefinition;
import cz.tul.dic.engine.solvers.AbstractTaskSolver;
import cz.tul.dic.engine.solvers.SolverType;
import cz.tul.pj.journal.Journal;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class KernelPerformanceManager {

    private static final Object SYNC_LOCK;
    private static final String PERFORMANCE_TEST_LABEL = "performance.time";
    private static final double[][] PERFORMANCE_TEST_LIMITS = new double[][]{
        // deformation counts - BruteForce - 441, SPGD - 3, NRC - 21, 160, NRCHE - 5, 13, NRF - 6, 28, NRFHE - 3, 7        
        {-1, 1, 1, 0, 0, 0}, // 3
        {-3, 3, 1, -1, 1, 1}, // 21
        {0, 1, 1, 0, 1, 1, 0.0, 0.1, 0.1, 0.0, 0.1, 0.1, 0.00, 0.01, 0.01, -0.02, 0.02, 0.01}, // 160
        {-10.0, 10.0, 1, -10, 10, 1}, // 441
    };
    private static final int[] PERFORMANCE_TEST_SUBSET_COUNT = new int[]{1, 32};
    private static final int[] PERFORMANCE_TEST_SUBSET_SIZES = new int[]{5, 15, 35};
    private static final int PERFORMANCE_TEST_BLANK_COUNT = 5;
    private static final KernelPerformanceManager INSTANCE;
    private final Map<PlatformType, Map<DeviceType, List<KernelInfo>>> unsupportedKernels;
    private final Map<PlatformType, Map<DeviceType, PerformanceData>> performanceData;
    private final PlatformDefinition bestPlatform;

    static {
        SYNC_LOCK = new Object();
        synchronized (SYNC_LOCK) {
            INSTANCE = new KernelPerformanceManager();
        }
    }

    public static KernelPerformanceManager getInstance() {
        synchronized (SYNC_LOCK) {
            return INSTANCE;
        }
    }

    private KernelPerformanceManager() {
        unsupportedKernels = generateUnsupportedKernels();

        final long lastCheck = Preferences.userNodeForPackage(KernelPerformanceManager.class).getLong(PERFORMANCE_TEST_LABEL, 0);
        final long currentTime = System.currentTimeMillis();
        final long diff = currentTime - lastCheck;

        Map<PlatformType, Map<DeviceType, PerformanceData>> data = null;
        boolean loaded = false;
        if (TimeUnit.MILLISECONDS.toDays(diff) < 7) {
            try {
                data = loadPerformanceData();
                loaded = true;
            } catch (IOException | ClassNotFoundException ex) {
                Logger.warn("Error loading performance assesment - " + ex.getLocalizedMessage());
            }
        }

        if (!loaded) {
            performanceData = runPerformanceTest();
            try {
                savePerformanceData();
                Preferences.userNodeForPackage(KernelPerformanceManager.class).putLong(PERFORMANCE_TEST_LABEL, System.currentTimeMillis());
            } catch (IOException ex) {
                Logger.warn(ex, "Error saving performance assesment.");
            }
            Journal.getInstance().addEntry("Kernel performance assesment completed.");
        } else {
            performanceData = data;
            Journal.getInstance().addEntry("Kernel performance assesment loaded.");
        }

        bestPlatform = findBestPlatform();
    }

    private Map<PlatformType, Map<DeviceType, List<KernelInfo>>> generateUnsupportedKernels() {
        final Map<PlatformType, Map<DeviceType, List<KernelInfo>>> result = new EnumMap<>(PlatformType.class);
        result.put(PlatformType.JAVA, new EnumMap<>(DeviceType.class));
        result.put(PlatformType.OPENCL, new EnumMap<>(DeviceType.class));

        List<KernelInfo> uInfos;
        // Java - CPU
        uInfos = new ArrayList<>();
        uInfos.addAll(generateKernelInfos(new KernelInfo(Type.CL1D, KernelInfo.Input.ANY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.ANY, KernelInfo.UseLimits.ANY)));
        uInfos.addAll(generateKernelInfos(new KernelInfo(Type.CL15D_pF, KernelInfo.Input.ANY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.ANY, KernelInfo.UseLimits.ANY)));
        uInfos.addAll(generateKernelInfos(new KernelInfo(Type.CL2D, KernelInfo.Input.ANY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.ANY, KernelInfo.UseLimits.ANY)));
        uInfos.addAll(generateKernelInfos(new KernelInfo(Type.JavaKernel, KernelInfo.Input.IMAGE, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.ANY, KernelInfo.UseLimits.ANY)));
        uInfos.addAll(generateKernelInfos(new KernelInfo(Type.JavaKernel, KernelInfo.Input.ANY, KernelInfo.Correlation.WZNSSD, KernelInfo.MemoryCoalescing.ANY, KernelInfo.UseLimits.ANY)));
        uInfos.addAll(generateKernelInfos(new KernelInfo(Type.JavaKernel, KernelInfo.Input.ANY, KernelInfo.Correlation.ZNSSD, KernelInfo.MemoryCoalescing.ANY, KernelInfo.UseLimits.ANY)));
        uInfos.addAll(generateKernelInfos(new KernelInfo(Type.JavaKernel, KernelInfo.Input.ANY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.YES, KernelInfo.UseLimits.ANY)));
        uInfos.addAll(generateKernelInfos(new KernelInfo(Type.JavaKernel, KernelInfo.Input.ANY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.ANY, KernelInfo.UseLimits.NO)));
        result.get(PlatformType.JAVA).put(DeviceType.CPU, uInfos);
        // Java - GPU + iGPU
        uInfos = new ArrayList<>();
        uInfos.addAll(generateKernelInfos());   // no kernels for Java + iGPU / GPU
        result.get(PlatformType.JAVA).put(DeviceType.iGPU, uInfos);
        result.get(PlatformType.JAVA).put(DeviceType.GPU, uInfos);

        // OpenCL CPU
        uInfos = new ArrayList<>();
        uInfos.addAll(generateKernelInfos(new KernelInfo(Type.JavaKernel, KernelInfo.Input.ANY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.ANY, KernelInfo.UseLimits.ANY)));
        uInfos.addAll(generateKernelInfos(new KernelInfo(Type.CL2D, KernelInfo.Input.ANY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.YES, KernelInfo.UseLimits.ANY)));
        uInfos.addAll(generateKernelInfos(new KernelInfo(Type.CL15D_pF, KernelInfo.Input.ANY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.ANY, KernelInfo.UseLimits.ANY)));
        uInfos.addAll(generateKernelInfos(new KernelInfo(Type.CL1D, KernelInfo.Input.ARRAY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.ANY, KernelInfo.UseLimits.YES)));
        result.get(PlatformType.OPENCL).put(DeviceType.CPU, uInfos);
        // -----
        // OpenCL GPU
        uInfos = new ArrayList<>();
        uInfos.addAll(generateKernelInfos(new KernelInfo(Type.JavaKernel, KernelInfo.Input.ANY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.ANY, KernelInfo.UseLimits.ANY)));
        uInfos.addAll(generateKernelInfos(new KernelInfo(Type.CL2D, KernelInfo.Input.ANY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.YES, KernelInfo.UseLimits.ANY)));
        uInfos.addAll(generateKernelInfos(new KernelInfo(Type.CL15D_pF, KernelInfo.Input.ANY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.YES, KernelInfo.UseLimits.ANY)));
        result.get(PlatformType.OPENCL).put(DeviceType.GPU, uInfos);
        // OpenCL iGPU
        uInfos = new ArrayList<>();
        uInfos.addAll(generateKernelInfos(new KernelInfo(Type.JavaKernel, KernelInfo.Input.ANY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.ANY, KernelInfo.UseLimits.ANY)));
        uInfos.addAll(generateKernelInfos(new KernelInfo(Type.CL2D, KernelInfo.Input.ANY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.YES, KernelInfo.UseLimits.ANY)));
        uInfos.addAll(generateKernelInfos(new KernelInfo(Type.CL15D_pF, KernelInfo.Input.ANY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.YES, KernelInfo.UseLimits.ANY)));
        result.get(PlatformType.OPENCL).put(DeviceType.iGPU, uInfos);

        // DEBUG !!! - use OpenCL only for GPU
        result.get(PlatformType.OPENCL).get(DeviceType.CPU).addAll(generateKernelInfos(new KernelInfo(Type.ANY, KernelInfo.Input.ANY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.ANY, KernelInfo.UseLimits.ANY)));
//        result.get(PlatformType.OPENCL).get(DeviceType.GPU).addAll(generateKernelInfos(new KernelInfo(Type.ANY, KernelInfo.Input.ANY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.ANY, KernelInfo.UseLimits.ANY)));
        result.get(PlatformType.OPENCL).get(DeviceType.iGPU).addAll(generateKernelInfos(new KernelInfo(Type.ANY, KernelInfo.Input.ANY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.ANY, KernelInfo.UseLimits.ANY)));
        return result;
    }

    private Map<PlatformType, Map<DeviceType, PerformanceData>> runPerformanceTest() throws RuntimeException {
        final Map<PlatformType, Map<DeviceType, PerformanceData>> result = new EnumMap<>(PlatformType.class);

        final Random rnd = new Random();

        List<KernelInfo> kernels;
        AbstractTaskSolver solver;
        Platform platform;
        for (PlatformType platformType : PlatformType.values()) {
            result.put(platformType, new EnumMap<>(DeviceType.class));

            for (DeviceType deviceType : DeviceType.values()) {
                kernels = generateKernelInfos(platformType, deviceType);
                if (kernels.isEmpty()) {
                    continue;
                }

                Logger.debug("Running performance test for {}:{}, total {} kernels.", platformType, deviceType, kernels.size());

                // run blank test for device preparation
                try {
                    final int testCount = Math.min(kernels.size(), PERFORMANCE_TEST_BLANK_COUNT);
                    for (int i = 0; i < testCount; i++) {
                        KernelInfo kernel = kernels.get(rnd.nextInt(kernels.size()));
                        platform = PlatformManager.getInstance().initPlatform(platformType, deviceType, kernel);
                        Logger.info("Blank - " + platform);
                        solver = AbstractTaskSolver.initSolver(SolverType.BRUTE_FORCE, platform);
                        solver.setInterpolation(TaskDefaultValues.DEFAULT_INTERPOLATION);
                        solver.setTaskSplitVariant(TaskSplitMethod.NONE, null);
                        testKernelInfo(solver, platform);
                    }
                } catch (Exception ex) {
                    Logger.warn(ex, "Blank test failed for {}.", deviceType.toString());
                }

                PerformanceData testPerformanceData;
                for (KernelInfo kernel : kernels) {
                    platform = PlatformManager.getInstance().initPlatform(platformType, deviceType, kernel);
                    Logger.info("Test - " + platform);
                    solver = AbstractTaskSolver.initSolver(SolverType.BRUTE_FORCE, platform);
                    solver.setInterpolation(TaskDefaultValues.DEFAULT_INTERPOLATION);
                    solver.setTaskSplitVariant(TaskSplitMethod.NONE, null);
                    // run test
                    testPerformanceData = testKernelInfo(solver, platform);
                    result.get(platformType).put(deviceType, testPerformanceData);
                }
            }
        }

        return result;
    }

    private static PerformanceData testKernelInfo(final AbstractTaskSolver solver, final Platform platform) {
        final PerformanceData result = new PerformanceData();
        final Image img = Image.createImage(new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY));

        List<double[]> deformationLimits;
        List<AbstractSubset> subsets;
        List<Integer> weights;
        AbstractSubset subset;
        long start;
        long[] counts;
        for (int ss : PERFORMANCE_TEST_SUBSET_SIZES) {
            subset = new SquareSubset2D(ss, ss + 1, ss + 1);
            for (int sc : PERFORMANCE_TEST_SUBSET_COUNT) {
                for (double[] limits : PERFORMANCE_TEST_LIMITS) {
                    subsets = Collections.nCopies(sc, subset);
                    deformationLimits = Collections.nCopies(sc, limits);
                    weights = Collections.nCopies(sc, TaskContainerUtils.computeCorrelationWeight(ss, TaskDefaultValues.DEFAULT_CORRELATION_WEIGHT));
                    counts = DeformationUtils.generateDeformationCounts(limits);
                    try {
                        start = System.currentTimeMillis();
                        solver.solve(new FullTask(
                                img, img, subsets, weights, deformationLimits));
                        result.store(sc, counts[counts.length - 1], platform.getPlatformDefinition().getKernelInfo(), System.currentTimeMillis() - start);
                    } catch (ComputationException ex) {
                        Logger.debug(ex, "Device test exception", "KernelInfo {} on {}-{}.", platform.getPlatformDefinition().getKernelInfo(), platform.getPlatformDefinition().getPlatform(), platform.getPlatformDefinition().getDevice());
                        result.store(sc, sc, platform.getPlatformDefinition().getKernelInfo(), Long.MAX_VALUE);
                    }
                }
            }
        }

        return result;
    }

    private void savePerformanceData() throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(PERFORMANCE_TEST_LABEL))) {
            out.writeObject(performanceData);
        }
    }

    private Map<PlatformType, Map<DeviceType, PerformanceData>> loadPerformanceData() throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(PERFORMANCE_TEST_LABEL))) {
            final Object o = in.readObject();
            final Map<PlatformType, Map<DeviceType, PerformanceData>> result = (Map<PlatformType, Map<DeviceType, PerformanceData>>) o;
            return result;
        }
    }

    private PlatformDefinition findBestPlatform() {
        // prefer platforms that can compute all inputs
        // the performance score is computed by ordering platforms using computation time
        //  - simply sum all times (compare only platforms with same number of occurences)
        //  - platform with lowest sum wins

        // input counting
        final Map<PlatformDefinition, Integer> counts = new HashMap<>();
        Map<KernelInfo, Integer> occurences;
        int count;
        int max = 0;
        for (PlatformType pt : PlatformType.values()) {
            for (DeviceType dt : DeviceType.values()) {
                count = 0;
                if (performanceData.containsKey(pt) && performanceData.get(pt).containsKey(dt)) {
                    occurences = performanceData.get(pt).get(dt).generateOccurences();
                    for (Entry<KernelInfo, Integer> e : occurences.entrySet()) {
                        count = e.getValue();
                        counts.put(new PlatformDefinition(pt, dt, e.getKey()), count);

                        if (count > max) {
                            max = count;
                        }
                    }
                }
            }
        }

        if (max == 0) {
            throw new IllegalStateException("No best platform, performance data missing.");
        }

        PlatformDefinition result = null, current;
        TreeMap<Long, PlatformDefinition> scores = new TreeMap<>();
        while (result == null && max > 0) {
            scores.clear();
            for (Entry<PlatformDefinition, Integer> e : counts.entrySet()) {
                if (e.getValue() == max) {
                    // compute score
                    current = e.getKey();
                    scores.put(
                            performanceData.get(current.getPlatform()).get(current.getDevice()).sumTimes(current.getKernelInfo()),
                            current);
                }
            }

            if (!scores.isEmpty()) {
                result = scores.firstEntry().getValue();
            } else {
                max--;
            }
        }

        if (result == null) {
            throw new IllegalStateException("No best platform found.");
        }

        return result;
    }

    public PlatformDefinition getBestPlatform() {
        return bestPlatform;
    }

    public List<KernelInfo> generateKernelInfos() {
        final List<KernelInfo> result = new ArrayList<>();

        for (KernelInfo.Type kt : KernelInfo.Type.values()) {
            if (kt == KernelInfo.Type.ANY) {
                continue;
            }

            for (KernelInfo.Input in : KernelInfo.Input.values()) {
                if (in == KernelInfo.Input.ANY) {
                    continue;
                }

                for (KernelInfo.Correlation cor : KernelInfo.Correlation.values()) {
                    if (cor == KernelInfo.Correlation.ANY || cor == KernelInfo.Correlation.NO_WEIGHTS) {
                        continue;
                    }

                    for (KernelInfo.MemoryCoalescing mc : KernelInfo.MemoryCoalescing.values()) {
                        if (mc == KernelInfo.MemoryCoalescing.ANY) {
                            continue;
                        }

                        for (KernelInfo.UseLimits lim : KernelInfo.UseLimits.values()) {
                            if (lim == KernelInfo.UseLimits.ANY) {
                                continue;
                            }

                            result.add(new KernelInfo(kt, in, cor, mc, lim));
                        }
                    }
                }
            }
        }

        return result;
    }

    public List<KernelInfo> generateKernelInfos(final PlatformType platform, final DeviceType device) {
        final List<KernelInfo> result = generateKernelInfos();
        final List<KernelInfo> unsupported = unsupportedKernels.get(platform).get(device);

        final Iterator<KernelInfo> it = result.iterator();
        while (it.hasNext()) {
            if (unsupported.contains(it.next())) {
                it.remove();
            }
        }

        return result;
    }

    public List<KernelInfo> generateKernelInfos(final PlatformType platform, final DeviceType device, final KernelInfo requestedKernelInfo) {
        final ArrayList<KernelInfo> result = new ArrayList<>();

        final List<KernelInfo> all = generateKernelInfos(requestedKernelInfo);
        final List<KernelInfo> unsuported = unsupportedKernels.get(platform).get(device);
        for (KernelInfo kernelInfo : all) {
            if (!unsuported.contains(kernelInfo)) {
                result.add(kernelInfo);
            }
        }

        return result;
    }

    public List<KernelInfo> generateKernelInfos(final KernelInfo requestedKernelInfo) {
        final ArrayList<KernelInfo> result = new ArrayList<>();

        final List<Type> kernels = new ArrayList<>();
        if (requestedKernelInfo.getType() == Type.ANY) {
            kernels.addAll(Arrays.asList(Type.values()));
            kernels.remove(KernelInfo.Type.ANY);
        } else {
            kernels.add(requestedKernelInfo.getType());
        }

        final List<KernelInfo.Input> inputs = new ArrayList<>();
        if (requestedKernelInfo.getInput() == KernelInfo.Input.ANY) {
            inputs.addAll(Arrays.asList(KernelInfo.Input.values()));
            inputs.remove(KernelInfo.Input.ANY);
        } else {
            inputs.add(requestedKernelInfo.getInput());
        }

        final List<KernelInfo.Correlation> correlations = new ArrayList<>();
        if (null != requestedKernelInfo.getCorrelation()) {
            switch (requestedKernelInfo.getCorrelation()) {
                case ANY:
                    correlations.addAll(Arrays.asList(KernelInfo.Correlation.values()));
                    correlations.remove(KernelInfo.Correlation.ANY);
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
        if (requestedKernelInfo.getMemoryCoalescing() == KernelInfo.MemoryCoalescing.ANY) {
            memoryCoalescing.addAll(Arrays.asList(KernelInfo.MemoryCoalescing.values()));
            memoryCoalescing.remove(KernelInfo.MemoryCoalescing.ANY);
        } else {
            memoryCoalescing.add(requestedKernelInfo.getMemoryCoalescing());
        }

        final List<KernelInfo.UseLimits> useLimits = new ArrayList<>();
        if (requestedKernelInfo.getUseLimits() == KernelInfo.UseLimits.ANY) {
            useLimits.addAll(Arrays.asList(KernelInfo.UseLimits.values()));
            useLimits.remove(KernelInfo.UseLimits.ANY);
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
                            result.add(kernelInfo);
                        }
                    }
                }
            }
        }

        return result;
    }

    public KernelInfo getBestKernel(final boolean usesWeights) {
        if (usesWeights) {
            return new KernelInfo(Type.ANY, KernelInfo.Input.ANY, KernelInfo.Correlation.ANY, KernelInfo.MemoryCoalescing.ANY, KernelInfo.UseLimits.ANY);
        } else {
            return new KernelInfo(Type.ANY, KernelInfo.Input.ANY, KernelInfo.Correlation.NO_WEIGHTS, KernelInfo.MemoryCoalescing.ANY, KernelInfo.UseLimits.ANY);
        }
    }

    private static class PerformanceData implements Serializable {

        private final Map<Long, Map<Long, Map<KernelInfo, Long>>> data;

        public PerformanceData() {
            data = new TreeMap<>();
        }

        public Map<Long, Map<Long, Map<KernelInfo, Long>>> getData() {
            return data;
        }

        public void store(final long subsetCount, final long deformationCount, final KernelInfo kernel, final long time) {
            Map<Long, Map<KernelInfo, Long>> m1 = data.get(subsetCount);
            if (m1 == null) {
                m1 = new TreeMap<>();
                data.put(subsetCount, m1);
            }

            Map<KernelInfo, Long> m2 = m1.get(deformationCount);
            if (m2 == null) {
                m2 = new HashMap<>();
                m1.put(deformationCount, m2);
            }

            m2.put(kernel, time);
        }

        public Map<KernelInfo, Integer> generateOccurences() {
            Map<KernelInfo, Integer> result = new HashMap<>();
            for (Map<Long, Map<KernelInfo, Long>> m : data.values()) {
                for (Map<KernelInfo, Long> m2 : m.values()) {
                    for (KernelInfo ki : m2.keySet()) {
                        if (result.containsKey(ki)) {
                            result.put(ki, result.get(ki) + 1);
                        } else {
                            result.put(ki, 1);
                        }
                    }
                }
            }
            return result;
        }

        public long sumTimes(final KernelInfo kernelInfo) {
            long result = 0;

            for (Map<Long, Map<KernelInfo, Long>> m : data.values()) {
                for (Map<KernelInfo, Long> m2 : m.values()) {
                    if (m2.containsKey(kernelInfo)) {
                        result += m2.get(kernelInfo);
                    }
                }
            }

            return result;
        }
    }

}
