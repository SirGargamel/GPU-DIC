package cz.tul.dic.output.data;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.FpsManager;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportUtils;
import java.util.EnumMap;
import java.util.Map;

public class ExportModePoint implements IExportMode<Map<Direction, double[]>> {

    @Override
    public Map<Direction, double[]> exportData(TaskContainer tc, Direction direction, int[] dataParams) throws ComputationException {
        if (dataParams == null || dataParams.length < 2) {
            throw new IllegalArgumentException("Not enough input parameters (position [x, y] required).");
        }

        final int roundCount = TaskContainerUtils.getMaxRoundCount(tc);
        final int roundZero = TaskContainerUtils.getFirstRound(tc);
        final Map<Direction, double[]> result = new EnumMap<>(Direction.class);
        for (Direction d : Direction.values()) {
            result.put(d, new double[roundCount]);
        }

        final int x = dataParams[0];
        final int y = dataParams[1];

        final FpsManager fpsM = new FpsManager((int) tc.getParameter(TaskParameter.FPS));
        final double time = fpsM.getTickLength();

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
                        results = tc.getDisplacement(r - 1, r);
                        break;
                    case Dx:
                    case Dy:
                    case Dabs:
                        results = TaskContainerUtils.getDisplacement(tc, roundZero, r);
                        break;
                    case dExx:
                    case dEyy:
                    case dExy:
                    case dEabs:
                        results = tc.getStrain(r - 1, r);
                        break;
                    case Exx:
                    case Eyy:
                    case Exy:
                    case Eabs:
                        results = tc.getStrain(roundZero, r);
                        break;
                    default:
                        throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction - " + dir);
                }

                if (results == null || results.length < x || results[0].length < y || results[x][y] == null) {
                    data[r] = 0;
                } else {
                    switch (dir) {
                        case dDx:
                        case dDy:
                        case dDabs:
                        case Dx:
                        case Dy:
                        case Dabs:
                            data[r] = ExportUtils.calculateDisplacement(results[x][y], dir);
                            break;
                        case dExx:
                        case dEyy:
                        case dExy:
                        case dEabs:
                        case Exx:
                        case Eyy:
                        case Exy:
                        case Eabs:
                            data[r] = ExportUtils.calculateStrain(results[x][y], dir);
                            break;
                        case rDx:
                        case rDy:
                        case rDabs:
                            data[r] = ExportUtils.calculateSpeed(results[x][y], dir, time);
                            break;
                        default:
                            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction.");
                    }
                }
            }
        }

        return result;
    }
}
