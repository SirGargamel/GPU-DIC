package cz.tul.dic.debug;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.engine.strain.StrainEstimation;
import cz.tul.dic.output.ExportUtils;
import cz.tul.dic.output.NameGenerator;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Petr Ječmen
 */
public class DebugControl {

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

    public static void performStatisticsDump(final TaskContainer tc) throws IOException, ComputationException {
        for (Entry<Integer, Integer> e : TaskContainerUtils.getRounds(tc).entrySet()) {
            Stats.getInstance().dumpDeformationsStatisticsUsage(e.getKey());
            Stats.getInstance().dumpDeformationsStatisticsPerQuality(e.getKey());
            Stats.getInstance().drawPointResultStatistics(e.getKey(), e.getValue());
        }

        Stats.getInstance().dumpDeformationsStatisticsPerQuality();
        Stats.getInstance().dumpDeformationsStatisticsUsage();
        new StrainEstimation().computeStrain(tc);
    }

}
