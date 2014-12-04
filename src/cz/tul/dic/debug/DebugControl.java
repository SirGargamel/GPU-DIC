package cz.tul.dic.debug;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.engine.displacement.DisplacementCalculator;
import cz.tul.dic.engine.strain.StrainEstimation;
import cz.tul.dic.generators.facet.FacetGenerator;
import cz.tul.dic.output.ExportUtils;
import cz.tul.dic.output.NameGenerator;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
            Stats.dumpDeformationsStatisticsUsage(e.getKey());
            Stats.dumpDeformationsStatisticsPerQuality(e.getKey());
            Stats.drawPointResultStatistics(e.getKey(), e.getValue());
        }

        Stats.dumpDeformationsStatisticsPerQuality();
        Stats.dumpDeformationsStatisticsUsage();
        new StrainEstimation().computeStrain(tc);
    }

}
