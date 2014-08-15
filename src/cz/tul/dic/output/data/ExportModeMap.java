package cz.tul.dic.output.data;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportUtils;

public class ExportModeMap implements IExportMode<double[][]> {

    @Override
    public double[][] exportData(TaskContainer tc, Direction direction, int[] dataParams) throws ComputationException {
        if (dataParams == null || dataParams.length < 1) {
            throw new IllegalArgumentException("Not wnough input parameters (position required).");
        }
        final int round = dataParams[0];
        final double[][][] results;
        switch (direction) {
            case dDx:
            case dDy:
            case dDabs:
                results = tc.getDisplacement(round);
                break;
            case dExx:
            case dEyy:
            case dExy:
            case dEabs:
                results = tc.getStrain(round);
                break;
            case Dx:
            case Dy:
            case Dabs:
                results = tc.getCumulativeDisplacement(round);
                break;
            case Exx:
            case Eyy:
            case Exy:
            case Eabs:
                results = tc.getCumulativeStrain(round);
                break;
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction.");
        }
        if (results == null || results.length == 0 || results[0].length == 0) {
            return null;
        }

        final int width = results.length;
        final int height = results[0].length;

        final double[][] result = new double[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (results[x][y] == null) {
                    continue;
                }

                switch (direction) {
                    case dDx:
                    case dDy:
                    case dDabs:
                    case Dx:
                    case Dy:
                    case Dabs:
                        result[x][y] = ExportUtils.calculateDisplacement(results[x][y], direction);
                        break;
                    case dExx:
                    case dEyy:
                    case dExy:
                    case dEabs:
                    case Exx:
                    case Eyy:
                    case Exy:
                    case Eabs:
                        result[x][y] = ExportUtils.calculateStrain(results[x][y], direction);
                        break;
                    default:
                        throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction.");
                }
            }
        }

        return result;
    }

}
