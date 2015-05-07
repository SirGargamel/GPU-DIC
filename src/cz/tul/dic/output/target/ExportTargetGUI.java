package cz.tul.dic.output.target;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.gui.Context;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.data.ExportMode;
import cz.tul.dic.output.ExportUtils;
import cz.tul.dic.output.data.IExportMode;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ExportTargetGUI extends AbstractExportTarget {

    @Override
    void exportMap(final TaskContainer tc, final IExportMode<double[][]> exporter, final Direction direction, final Object targetParam, final int[] dataParams, final double[] limits) throws ComputationException {
        if (dataParams.length < 1) {
            throw new IllegalArgumentException("Not enough data parameters.");
        }

        final double[][] data = exporter.exportData(tc, direction, dataParams);

        final double[] minMax;
        if (Double.isNaN(limits[0]) || Double.isNaN(limits[1])) {
            minMax = ExportUtils.findMinMax(data);
            if (!Double.isNaN(limits[0])) {
                minMax[0] = limits[0];
            }
            if (!Double.isNaN(limits[1])) {
                minMax[1] = limits[1];
            }
        } else {
            minMax = new double[]{limits[0], limits[1]};
        }

        final int position = dataParams[0];
        final BufferedImage background = tc.getImage(position);
        final BufferedImage overlay;
        if (data == null) {
            overlay = background;
        } else {
            overlay = ExportUtils.overlayImage(background, ExportUtils.createImageFromMap((double[][]) data, direction, minMax));
        }
        final Context context = (Context) targetParam;
        context.storeMapExport(overlay, position, ExportMode.MAP, direction);
    }

    @Override
    void exportPoint(final TaskContainer tc, final IExportMode<Map<Direction, double[]>> exporter, final Object targetParam, final int[] dataParams) throws ComputationException {
        if (dataParams.length < 2) {
            throw new IllegalArgumentException("Not enough data parameters.");
        }
        final Map<Direction, double[]> data = exporter.exportData(tc, null, dataParams);
        final Context context = (Context) targetParam;
        context.storePointExport(data, dataParams[0], dataParams[1]);
    }

    @Override
    void exportDoublePoint(final TaskContainer tc, final IExportMode<Map<Direction, double[]>> exporter, final Object targetParam, final int[] dataParams) throws IOException, ComputationException {
        if (dataParams.length < 4) {
            throw new IllegalArgumentException("Not enough data parameters.");
        }
        final Map<Direction, double[]> data = exporter.exportData(tc, null, dataParams);
        final Context context = (Context) targetParam;
        context.storePointExport(data, dataParams[0], dataParams[1], dataParams[2], dataParams[3]);
    }

    @Override
    void exportSequence(final TaskContainer tc, final IExportMode<List<double[][]>> exporter, final Direction direction, final Object targetParam, final double[] limits) throws IOException, ComputationException {
        throw new UnsupportedOperationException("Unsupported mode.");
    }

    @Override
    void exportVideo(final TaskContainer tc, final IExportMode<List<double[][]>> exporter, final Direction direction, final Object targetParam, final double[] limits) throws IOException, ComputationException {
        throw new UnsupportedOperationException("Unsupported mode.");
    }

}
