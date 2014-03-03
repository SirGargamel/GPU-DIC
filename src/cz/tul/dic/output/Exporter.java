package cz.tul.dic.output;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.data.DataExportLine;
import cz.tul.dic.output.data.DataExportMap;
import cz.tul.dic.output.data.DataExportSequence;
import cz.tul.dic.output.data.IDataExport;
import cz.tul.dic.output.target.ITargetExport;
import cz.tul.dic.output.target.TargetExportCsv;
import cz.tul.dic.output.target.TargetExportFile;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Petr Jecmen
 */
public class Exporter {

    private static final Map<ExportMode, IDataExport> dataExporters;
    private static final Map<ExportTarget, ITargetExport> targetExporters;

    static {
        dataExporters = new HashMap<>();
        dataExporters.put(ExportMode.MAP, new DataExportMap());
        dataExporters.put(ExportMode.LINE, new DataExportLine());
        dataExporters.put(ExportMode.SEQUENCE, new DataExportSequence());

        targetExporters = new HashMap<>();
        targetExporters.put(ExportTarget.FILE, new TargetExportFile());
        targetExporters.put(ExportTarget.CSV, new TargetExportCsv());
    }

    public static void export(final TaskContainer tc) throws IOException {
        for (ExportTask et : tc.getExportTasks()) {
            export(et, tc);
        }
    }

    public static void export(final ExportTask et, final TaskContainer tc) throws IOException {
        IDataExport dataExporter;
        ITargetExport targetExporter;
        ExportTarget target;
        ExportMode mode;
        Object data;

        target = et.getTarget();
        if (targetExporters.containsKey(target)) {
            targetExporter = targetExporters.get(target);
        } else {
            throw new IllegalArgumentException("Unsupported export target - " + et.toString());
        }

        mode = et.getMode();
        if (dataExporters.containsKey(mode)) {
            dataExporter = dataExporters.get(mode);
        } else {
            throw new IllegalArgumentException("Unsupported export mode for this target - " + et.toString());
        }

        data = dataExporter.exportData(tc, et.getDirection(), et.getDataParams());
        targetExporter.exportData(
                data,
                et.getTargetParam(),
                et.getDataParams(),
                tc);
    }

    public static boolean isExportSupported(final ExportTask et) {
        if (et == null) {
            return false;
        }
        final ExportMode mode = et.getMode();
        final ExportTarget target = et.getTarget();
        return dataExporters.containsKey(mode) && targetExporters.containsKey(target) && targetExporters.get(target).supportsMode(mode);
    }
}
