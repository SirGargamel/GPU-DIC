package cz.tul.dic.output;

import cz.tul.dic.data.TaskContainer;
import java.util.EnumMap;
import java.util.Map;

/**
 *
 * @author Petr Jecmen
 */
public class Exporter {
    
    private static final Map<ExportTarget, Map<ExportMode, IExporter>> exporters;
    
    static {
        exporters = new EnumMap<>(ExportTarget.class);
    }

    public static void export(final TaskContainer tc) {
        Map<ExportMode, IExporter> m;
        ExportMode mode;
        ExportTarget target;
        for (ExportTask et : tc.getExportTasks()) {
            target = et.getTarget();
            if (exporters.containsKey(target)) {
                m = exporters.get(target);
                mode = et.getMode();
                if (m.containsKey(mode)) {
                    m.get(mode).exportResult(et, tc.getResult());
                } else {
                    throw new IllegalArgumentException("Unsupported export mode for this target - " + et.toString());
                }
            } else {
                throw new IllegalArgumentException("Unsupported export target - " + et.toString());
            }
        }
    }        
    
}
