package cz.tul.dic.debug;

import cz.tul.dic.output.ExportUtils;
import cz.tul.dic.output.NameGenerator;

/**
 *
 * @author Petr Ječmen
 */
public class DebugControl {
    
    private static boolean debugMode;
    
    static {
        debugMode = false;
        
        ExportUtils.enableDebugMode();
        NameGenerator.enableDebugMode();
    }

    public static boolean isDebugMode() {
        return debugMode;
    }

    public static void enableDebugMode() {
        debugMode = true;
    }
    
}
