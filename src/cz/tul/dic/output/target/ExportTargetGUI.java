package cz.tul.dic.output.target;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.gui.Context;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.data.ExportMode;
import cz.tul.dic.output.ExportUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class ExportTargetGUI implements IExportTarget {

    @Override
    public void exportData(Object data, Direction direction, Object targetParam, int[] dataParams, TaskContainer tc) throws IOException, ComputationException {
        if (!(targetParam instanceof Context)) {
            throw new IllegalArgumentException("Illegal type of target parameter - " + targetParam.getClass());
        }
        if (data instanceof double[][]) {
            exportImage((double[][]) data, direction, targetParam, dataParams, tc);
        } else if (data instanceof double[]) {
            exportLine((double[]) data, direction, targetParam, dataParams, tc);
        } else if (data != null) {
            throw new IllegalArgumentException("Illegal type of data - " + targetParam.getClass());
        }

    }

    private void exportImage(final double[][] data, Direction direction, final Object targetParam, int[] dataParams, final TaskContainer tc) throws ComputationException {
        if (dataParams.length < 1) {
            throw new IllegalArgumentException("Not enough data parameters.");
        }

        final int position = dataParams[0];
        final BufferedImage background = tc.getImage(position);
        final Context context = (Context) targetParam;
        final BufferedImage overlay;
        if (data != null) {
            overlay = ExportUtils.overlayImage(background, ExportUtils.createImageFromMap((double[][]) data, direction));
        } else {
            overlay = background;
        }
        context.storeMapExport(overlay, position, ExportMode.MAP, direction);
    }

    private void exportLine(final double[] data, Direction direction, final Object targetParam, int[] dataParams, final TaskContainer tc) {
        if (dataParams.length < 2) {
            throw new IllegalArgumentException("Not enough data parameters.");
        }
        final Context context = (Context) targetParam;
        context.storeLineExport(data, dataParams[0], dataParams[1], ExportMode.POINT, direction);
    }

    @Override
    public boolean supportsMode(ExportMode mode) {
        return !ExportMode.SEQUENCE.equals(mode);
    }

}
