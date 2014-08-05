package cz.tul.dic.output;

import cz.tul.dic.output.data.ExportMode;
import cz.tul.dic.output.target.ExportTarget;
import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.data.ExportModeLine;
import cz.tul.dic.output.data.ExportModeMap;
import cz.tul.dic.output.data.ExportModeSequence;
import cz.tul.dic.output.data.IExportMode;
import cz.tul.dic.output.target.IExportTarget;
import cz.tul.dic.output.target.ExportTargetCsv;
import cz.tul.dic.output.target.ExportTargetFile;
import cz.tul.dic.output.target.ExportTargetGUI;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class Exporter {

    private static final Map<ExportMode, IExportMode> dataExporters;
    private static final Map<ExportTarget, IExportTarget> targetExporters;

    static {
        dataExporters = new HashMap<>();
        dataExporters.put(ExportMode.MAP, new ExportModeMap());
        dataExporters.put(ExportMode.LINE, new ExportModeLine());
        dataExporters.put(ExportMode.SEQUENCE, new ExportModeSequence());

        targetExporters = new HashMap<>();
        targetExporters.put(ExportTarget.FILE, new ExportTargetFile());
        targetExporters.put(ExportTarget.CSV, new ExportTargetCsv());
        targetExporters.put(ExportTarget.GUI, new ExportTargetGUI());
    }

    public static void export(final TaskContainer tc, final ExportTask et) throws IOException, ComputationException {
        IExportMode dataExporter;
        ExportMode mode;
        final Object data;                

        mode = et.getMode();
        if (dataExporters.containsKey(mode)) {
            dataExporter = dataExporters.get(mode);
        } else {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported export mode for this target - " + et.toString());
        }

        try {
            data = dataExporter.exportData(tc, et.getDirection(), et.getDataParams());
            exportData(et, tc, data);
        } catch (IndexOutOfBoundsException | NullPointerException ex) {
            Logger.warn(ex, "Export failed due to invalid input data.");
        }
    }

    public static void export(final TaskContainer tc) throws IOException, ComputationException {
        for (ExportTask et : tc.getExports()) {
            export(tc, et);
        }
    }

    public static void exportData(final ExportTask et, final TaskContainer tc, final Object data) throws ComputationException, IOException {
        final ExportTarget target = et.getTarget();
        IExportTarget targetExporter;
        if (targetExporters.containsKey(target)) {
            targetExporter = targetExporters.get(target);
        } else {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported export target - " + et.toString());
        }
        try {
            targetExporter.exportData(
                    data,
                    et.getDirection(),
                    et.getTargetParam(),
                    et.getDataParams(),
                    tc);
        } catch (IndexOutOfBoundsException | NullPointerException ex) {
            Logger.warn(ex, "Export failed due to invalid input data.");
        }
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
