package cz.tul.dic.output.target;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.data.ExportMode;
import cz.tul.dic.output.data.ExportModeDoublePoint;
import cz.tul.dic.output.data.ExportModeMap;
import cz.tul.dic.output.data.ExportModePoint;
import cz.tul.dic.output.data.ExportModeSequence;
import cz.tul.dic.output.data.IExportMode;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Petr Jecmen
 */
public abstract class AbstractExportTarget {

    private static final Map<ExportMode, IExportMode> dataExporters;

    static {
        dataExporters = new HashMap<>();
        dataExporters.put(ExportMode.MAP, new ExportModeMap());
        dataExporters.put(ExportMode.POINT, new ExportModePoint());
        dataExporters.put(ExportMode.DOUBLE_POINT, new ExportModeDoublePoint());
        dataExporters.put(ExportMode.SEQUENCE, new ExportModeSequence());
        dataExporters.put(ExportMode.VIDEO, new ExportModeSequence());
    }

    public void exportData(final ExportTask et, final TaskContainer tc) throws IOException, ComputationException {
        final ExportMode mode = et.getMode();
        switch (mode) {
            case POINT:
                exportPoint(tc, getExporter(mode), et.getTargetParam(), et.getDataParams());
                break;
            case DOUBLE_POINT:
                exportDoublePoint(tc, getExporter(mode), et.getTargetParam(), et.getDataParams());
                break;
            case MAP:
                exportMap(tc, getExporter(mode), et.getDirection(), et.getTargetParam(), et.getDataParams(), et.getLimits());
                break;
            case SEQUENCE:
                exportSequence(tc, getExporter(mode), et.getDirection(), et.getTargetParam(), et.getLimits());
                break;
            case VIDEO:
                exportVideo(tc, getExporter(mode), et.getDirection(), et.getTargetParam(), et.getLimits());
                break;
            default:
                throw new UnsupportedOperationException(mode.toString());
        }
    }

    abstract void exportMap(final TaskContainer tc, final IExportMode<double[][]> exporter, final Direction direction, final Object targetParam, final int[] dataParams, final double[] limits) throws IOException, ComputationException;

    abstract void exportPoint(final TaskContainer tc, final IExportMode<Map<Direction, double[]>> exporter, final Object targetParam, final int[] dataParams) throws IOException, ComputationException;

    abstract void exportDoublePoint(final TaskContainer tc, final IExportMode<Map<Direction, double[]>> exporter, final Object targetParam, final int[] dataParams) throws IOException, ComputationException;

    abstract void exportSequence(final TaskContainer tc, final IExportMode<List<double[][]>> exporter, Direction direction, Object targetParam, double[] limits) throws IOException, ComputationException;

    abstract void exportVideo(final TaskContainer tc, final IExportMode<List<double[][]>> exporter, Direction direction, final Object targetParams, double[] limits) throws IOException, ComputationException;           

    @SuppressWarnings("unchecked")
    private static <T> IExportMode<T> getExporter(final ExportMode em) {
        return dataExporters.get(em);
    }

}
