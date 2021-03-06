/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.output.data;

import cz.tul.dic.data.result.Result;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.output.Direction;
import java.util.EnumMap;
import java.util.Map;

public class ExportModeDoublePoint implements IExportMode<Map<Direction, double[]>> {

    @Override
    public Map<Direction, double[]> exportData(TaskContainer tc, Direction direction, int[] dataParams) {
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

        Result res;
        double[][][] results;
        double[] data;
        for (Direction dir : Direction.values()) {
            data = result.get(dir);
            for (int round = 0; round < roundCount; round++) {
                switch (dir) {
                    case D_EXX:
                    case D_EYY:
                    case D_EXY:
                    case D_EABS:
                        res = tc.getResult(round - 1, round);
                        results = res == null ? null : res.getStrainResult().getStrain();
                        break;
                    case EXX:
                    case EYY:
                    case EXY:
                    case EABS:
                        res = tc.getResult(roundZero, round);
                        results = res == null ? null : res.getStrainResult().getStrain();
                        break;
                    default:
                        results = null;
                }

                if (isResultValid(results, x1, y1) && isResultValid(results, x2, y2)) {
                    switch (dir) {
                        case D_EXX:
                        case D_EYY:
                        case D_EXY:
                        case D_EABS:
                        case EXX:
                        case EYY:
                        case EXY:
                        case EABS:
                            data[round] = calculateStrain(results, dir, x1, y1, x2, y2);
                            break;
                        default:
                        // ignore other directions
                    }
                } else {
                    data[round] = 0;
                }
            }
        }

        return result;
    }

    private static boolean isResultValid(double[][][] results, final int x, final int y) {
        return results != null && results.length >= x && results[0].length >= y && results[x][y] != null;
    }

    private double calculateStrain(final double[][][] displacement, final Direction dir, final int x1, final int y1, final int x2, final int y2) {
        final double dx = (double) x2 - x1;
        final double dy = (double) y2 - y1;
        final double difX = displacement[x2][y2][0] - displacement[x1][y1][0];
        final double difY = displacement[x2][y2][1] - displacement[x1][y1][1];
        final double val;
        switch (dir) {
            case EXX:
            case D_EXX:
                val = difX / dx;
                break;
            case EYY:
            case D_EYY:
                val = difY / dy;
                break;
            case EXY:
            case D_EXY:
                double tmp = difY / dx;
                tmp += difX / dy;
                val = tmp / 2.0;
                break;
            case EABS:
            case D_EABS:
                final double val1 = difX / dx;
                final double val2 = difY / dy;
                final double val3 = (difY / dx + difX / dy) / 2.0;
                val = Math.sqrt(val1 * val1 + val2 * val2 + val3 * val3);
                break;
            default:
                throw new IllegalArgumentException("Unsupported direction - " + dir);
        }
        return val * 100;
    }
}
