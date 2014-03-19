package cz.tul.dic.output.data;

import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportUtils;
import cz.tul.dic.output.OutputUtils;

public class DataExportMap implements IDataExport<double[][]> {

    @Override
    public double[][] exportData(final TaskContainer tc, Direction direction, final int[] dataParams) {
        if (dataParams == null || dataParams.length < 1) {
            throw new IllegalArgumentException("Not wnough input parameters (position required).");
        }
        final double[][][] results = tc.getPerPixelResult(dataParams[0]);
        if (results == null || results.length == 0 || results[0].length == 0) {
            throw new IllegalArgumentException("Illegal result data.");
        }

        final int width = results.length;
        final int height = results[0].length;

        final double[][] result = new double[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                switch (direction) {
                    case X:
                    case Y:
                    case ABS:
                        if (!(x < 0 || y < 0 || x >= results.length || y >= results[x].length)) {
                            result[x][y] = ExportUtils.calculateDisplacement(results[x][y], direction);
                        }
                        break;
                    case DX:
                    case DY:
                    case DABS:
                        if (!(x < 0 || y < 0 || (x + 1) >= results.length || (y + 1) >= results[x].length)) {
                            result[x][y] = ExportUtils.calculateDeformation(results, x, y, direction);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported direction.");
                }
            }
        }

        return result;
    }

    @Override
    public double[][] exportData(TaskContainer tc, Direction direction, int[] dataParams, ROI... rois) {
        if (dataParams == null || dataParams.length < 1) {
            throw new IllegalArgumentException("Not wnough input parameters (position required).");
        }
        final double[][][] results = tc.getPerPixelResult(dataParams[0]);
        if (results == null || results.length == 0 || results[0].length == 0) {
            throw new IllegalArgumentException("Illegal result data.");
        }

        final int width = results.length;
        final int height = results[0].length;

        final double[][] result = new double[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (!OutputUtils.isPointsInsideROIs(x, y, rois)) {
                    continue;
                }

                switch (direction) {
                    case X:
                    case Y:
                    case ABS:
                        result[x][y] = ExportUtils.calculateDisplacement(results[x][y], direction);
                        break;
                    case DX:
                    case DY:
                    case DABS:
                        result[x][y] = ExportUtils.calculateDeformation(results, x, y, direction);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported direction.");
                }
            }
        }

        return result;
    }

}
