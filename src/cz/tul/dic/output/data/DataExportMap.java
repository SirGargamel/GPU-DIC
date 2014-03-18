package cz.tul.dic.output.data;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportUtils;

public class DataExportMap implements IDataExport<double[][]> {

    @Override
    public double[][] exportData(final TaskContainer tc, Direction direction, final int... params) {
        if (params == null || params.length < 1) {
            throw new IllegalArgumentException("Not wnough input parameters (position required).");
        }
        final double[][][] results = tc.getPerPixelResult(params[0]);
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
