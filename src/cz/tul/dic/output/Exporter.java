package cz.tul.dic.output;

import cz.tul.dic.data.task.TaskContainer;
import java.io.IOException;
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

        IExporter exporter = new ExportImageFile();
        addExporter(exporter);
    }

    private static void addExporter(final IExporter exporter) {
        final ExportMode mode = exporter.getMode();
        final ExportTarget target = exporter.getTarget();

        Map<ExportMode, IExporter> m = exporters.get(target);
        if (m == null) {
            m = new EnumMap<>(ExportMode.class);
            exporters.put(target, m);
        }

        m.put(mode, exporter);
    }

    public static void export(final TaskContainer tc) throws IOException {
        Map<ExportMode, IExporter> m;
        ExportMode mode;
        ExportTarget target;
        for (ExportTask et : tc.getExportTasks()) {
            target = et.getTarget();
            if (exporters.containsKey(target)) {
                m = exporters.get(target);
                mode = et.getMode();
                if (m.containsKey(mode)) {
                    m.get(mode).exportResult(et, tc);
                } else {
                    throw new IllegalArgumentException("Unsupported export mode for this target - " + et.toString());
                }
            } else {
                throw new IllegalArgumentException("Unsupported export target - " + et.toString());
            }
        }
    }

    public static int deformationToRGB(final double val) {
        int result;
        if (val == 0) {
            result = 0;
        } else if (val < 0) {
            result = ((int) (val)) << 16;
        } else {
            result = ((int) (-val)) & 0xff;
        }
        return result;
    }

}
