package cz.tul.dic.output.data;

import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportUtils;
import cz.tul.dic.output.OutputUtils;

public class DataExportLine implements IDataExport<double[]> {

    @Override
    public double[] exportData(final TaskContainer tc, Direction direction, final int[] dataParams) {
        if (dataParams == null || dataParams.length < 3) {
            throw new IllegalArgumentException("Not enough input parameters (position, x, y required).");
        }

        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        final double[] result = new double[roundCount];

        final int x = dataParams[0];
        final int y = dataParams[1];

        double[][][] results;
        for (int r = 0; r < roundCount; r++) {
            results = tc.getPerPixelResult(r);
            if (results == null || results.length < x || results[0].length < y) {
                throw new IllegalArgumentException("Illegal result data.");
            }

            switch (direction) {
                case X:
                case Y:
                case ABS:
                    if (!(x < 0 || y < 0 || x >= results.length || y >= results[x].length)) {
                        result[r] = ExportUtils.calculateDisplacement(results[x][y], direction);
                    }
                    break;
                case DX:
                case DY:
                case DABS:
                    if (!(x < 0 || y < 0 || (x + 1) >= results.length || (y + 1) >= results[x].length)) {
                        result[r] = ExportUtils.calculateDeformation(results, x, y, direction);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported direction.");
            }
        }

        return result;
    }

    @Override
    public double[] exportData(TaskContainer tc, Direction direction, int[] dataParams, ROI... rois) {
        if (dataParams == null || dataParams.length < 3) {
            throw new IllegalArgumentException("Not enough input parameters (position, x, y required).");
        }

        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        final double[] result = new double[roundCount];

        final int x = dataParams[0];
        final int y = dataParams[1];

        // check if position is inside ROI        
        double[][][] results;
        for (int r = 0; r < roundCount; r++) {
            if (OutputUtils.isPointInsideROIs(x, y, rois, tc, r)) {
                results = tc.getPerPixelResult(r);
                if (results == null || results.length < x || results[0].length < y) {
                    throw new IllegalArgumentException("Illegal result data.");
                }

                switch (direction) {
                    case X:
                    case Y:
                    case ABS:
                        result[r] = ExportUtils.calculateDisplacement(results[x][y], direction);
                        break;
                    case DX:
                        if (OutputUtils.isPointInsideROIs(x + 1, y, rois, tc, r)) {
                            result[r] = ExportUtils.calculateDeformation(results, x, y, direction);
                        }
                        break;
                    case DY:
                        if (OutputUtils.isPointInsideROIs(x, y + 1, rois, tc, r)) {
                            result[r] = ExportUtils.calculateDeformation(results, x, y, direction);
                        }
                        break;
                    case DABS:
                        if (OutputUtils.isPointInsideROIs(x + 1, y, rois, tc, r) && OutputUtils.isPointInsideROIs(x, y + 1, rois, tc, r)) {
                            result[r] = ExportUtils.calculateDeformation(results, x, y, direction);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported direction.");
                }
            }
        }

        return result;
    }
}
