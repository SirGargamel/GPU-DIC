package cz.tul.dic.output.data;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.task.DisplacementResult;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.output.Direction;
import java.util.EnumMap;
import java.util.Map;

public class ExportModeDoublePoint implements IExportMode<Map<Direction, double[]>> {

    @Override
    public Map<Direction, double[]> exportData(TaskContainer tc, Direction direction, int[] dataParams) throws ComputationException {
        if (dataParams == null || dataParams.length < 4) {
            throw new IllegalArgumentException("Not enough input parameters (position [x1, y1, x2, y2] required).");
        }

        final int roundCount = TaskContainerUtils.getMaxRoundCount(tc);
        final int roundZero = TaskContainerUtils.getFirstRound(tc);
        final Map<Direction, double[]> result = new EnumMap<>(Direction.class);
        for (Direction d : Direction.values()) {
            result.put(d, new double[roundCount]);
        }

        final int x1 = dataParams[0];
        final int y1 = dataParams[1];
        final int x2 = dataParams[2];
        final int y2 = dataParams[3];

        DisplacementResult dr;
        double[][][] results;
        double[] data;
        for (Direction dir : Direction.values()) {
            data = result.get(dir);
            for (int r = 0; r < roundCount; r++) {
                switch (dir) {
                    case dDx:
                    case dDy:
                    case dDabs:
                    case rDx:
                    case rDy:
                    case rDabs:
                    case Dx:
                    case Dy:
                    case Dabs:
                        results = null;
                        break;
                    case dExx:
                    case dEyy:
                    case dExy:
                    case dEabs:
                        dr = tc.getDisplacement(r - 1, r);
                        results = dr != null ? dr.getDisplacement() : null;
                        break;
                    case Exx:
                    case Eyy:
                    case Exy:
                    case Eabs:
                        dr = TaskContainerUtils.getDisplacement(tc, roundZero, r);
                        results = dr != null ? dr.getDisplacement() : null;
                        break;
                    default:
                        throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction - " + dir);
                }

                if (results == null || results.length < x1 || results[0].length < y1 || results.length < x2 || results[0].length < y2 || results[x1][y1] == null || results[x2][y2] == null) {
                    data[r] = 0;
                } else {
                    switch (dir) {
                        case dExx:
                        case dEyy:
                        case dExy:
                        case dEabs:
                        case Exx:
                        case Eyy:
                        case Exy:
                        case Eabs:
                            data[r] = calculateStrain(results, dir, x1, y1, x2, y2);
                            break;
                        default:
                            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction.");
                    }
                }
            }
        }

        return result;
    }

    private double calculateStrain(final double[][][] displacement, final Direction dir, final int x1, final int y1, final int x2, final int y2) throws ComputationException {
        final double dx = x2 - x1;
        final double dy = y2 - y1;
        final double difX = displacement[x2][y2][0] - displacement[x1][y1][0];
        final double difY = displacement[x2][y2][1] - displacement[x1][y1][1];
        final double val;
        switch (dir) {
            case Exx:
            case dExx:
                val = difX / dx;
                break;
            case Eyy:
            case dEyy:
                val = difY / dy;
                break;
            case Exy:
            case dExy:
                double tmp = difY / dx;
                tmp += difX / dy;
                val = tmp / 2.0;
                break;
            case Eabs:
            case dEabs:
                final double val1 = difX / dx;
                final double val2 = difY / dy;
                final double val3 = (difY / dx + difX / dy) / 2.0;
                val = Math.sqrt(val1 * val1 + val2 * val2 + val3 * val3);
                break;
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction - " + dir);
        }
        return val * 100;
    }
}
