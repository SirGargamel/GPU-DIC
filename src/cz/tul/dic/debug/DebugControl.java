/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.debug;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.engine.strain.StrainEstimator;
import cz.tul.dic.output.ExportUtils;
import cz.tul.dic.output.NameGenerator;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Petr Ječmen
 */
public final class DebugControl {

    private static boolean debugMode, debugModeStarted;
    private static final Set<ValueCounter> counters;

    static {
        debugMode = false;
        debugModeStarted = false;
        counters = new HashSet<>();
    }

    public static boolean isDebugMode() {
        return debugMode;
    }

    public static void startDebugMode() {
        debugMode = true;
        debugModeStarted = true;
        ExportUtils.enableDebugMode();
        NameGenerator.enableDebugMode();
    }

    public static void addCounter(ValueCounter rc) {
        counters.add(rc);
    }

    public static void pauseDebugMode() {
        debugMode = false;
        handleCounters();
    }

    public static void resumeDebugMode() {
        if (debugModeStarted) {
            debugMode = true;
            handleCounters();
        }
    }

    private static void handleCounters() {
        for (ValueCounter rc : counters) {
            rc.setEnabled(debugMode);
        }
    }

    public static void performStatisticsDump(final TaskContainer tc) throws ComputationException {
        for (Entry<Integer, Integer> e : TaskContainerUtils.getRounds(tc).entrySet()) {
            Stats.getInstance().dumpDeformationsStatisticsUsage(e.getKey());
            Stats.getInstance().dumpDeformationsStatisticsPerQuality(e.getKey());
            Stats.getInstance().drawPointResultStatistics(e.getKey(), e.getValue());
        }

        Stats.getInstance().dumpDeformationsStatisticsPerQuality();
        Stats.getInstance().dumpDeformationsStatisticsUsage();
        StrainEstimator.computeStrain(tc);
    }

    private DebugControl() {
    }

}
