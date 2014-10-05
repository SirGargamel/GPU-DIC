package cz.tul.dic.debug;

import cz.tul.dic.output.ExportUtils;
import cz.tul.dic.output.NameGenerator;
import java.util.HashMap;
import java.util.HashSet;
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

}
