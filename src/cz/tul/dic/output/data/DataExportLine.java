package cz.tul.dic.output.data;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportUtils;

public class DataExportLine implements IDataExport<double[]> {

    @Override
    public double[] exportData(final TaskContainer tc, Direction direction, final int... params) {
        if (params == null || params.length < 3) {
            throw new IllegalArgumentException("Not enough input parameters (position, x, y required).");
        }

        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        final double[] result = new double[roundCount];

        final int x = params[0];
        final int y = params[1];

        double[][][] results;
        for (int r = 0; r < roundCount; r++) {
            results = tc.getFinalResults(r);
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
                case DY:
                case DABS:
                    result[r] = ExportUtils.calculateDeformation(results, x, y, direction);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported direction.");
            }
        }

        return result;
    }
}
