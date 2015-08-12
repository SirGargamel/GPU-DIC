/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.output.data;

import cz.tul.dic.FpsManager;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.result.Result;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportUtils;
import java.util.EnumMap;
import java.util.Map;

public class ExportModePoint implements IExportMode<Map<Direction, double[]>> {

    @Override
    public Map<Direction, double[]> exportData(final TaskContainer tc, final Direction direction, final int[] dataParams) {
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

        final FpsManager fpsM = new FpsManager(tc);
        final double time = fpsM.getTickLength();

        final double pxToMm = 1 / (double) tc.getParameter(TaskParameter.MM_TO_PX_RATIO);

        Result res;
        double[][][] results3D = null;
        double[][] results2D = null;
        double[] data;
        for (Direction dir : Direction.values()) {
            data = result.get(dir);
            for (int round = 0; round < roundCount; round++) {
                switch (dir) {
                    case D_DX:
                    case D_DY:
                    case D_DABS:
                    case R_DX:
                    case R_DY:
                    case R_DABS:
                        res = tc.getResult(round - 1, round);
                        results3D = res == null ? null : res.getDisplacementResult().getDisplacement();
                        break;
                    case DX:
                    case DY:
                    case DABS:
                        res = tc.getResult(roundZero, round);
                        results3D = res == null ? null : res.getDisplacementResult().getDisplacement();
                        break;
                    case D_EXX:
                    case D_EYY:
                    case D_EXY:
                    case D_EABS:
                        res = tc.getResult(round - 1, round);
                        results3D = res == null ? null : res.getStrainResult().getStrain();
                        break;
                    case EXX:
                    case EYY:
                    case EXY:
                    case EABS:
                        res = tc.getResult(roundZero, round);
                        results3D = res == null ? null : res.getStrainResult().getStrain();
                        break;
                    case Q_D_D:
                        res = tc.getResult(round - 1, round);
                        results2D = res == null ? null : res.getDisplacementResult().getQuality();
                        break;
                    case Q_D:
                        res = tc.getResult(roundZero, round);
                        results2D = res == null ? null : res.getDisplacementResult().getQuality();
                        break;
                    case Q_D_EX:
                        res = tc.getResult(round - 1, round);
                        results2D = res == null ? null : res.getStrainResult().getQuality()[0];
                        break;
                    case Q_D_EY:
                        res = tc.getResult(round - 1, round);
                        results2D = res == null ? null : res.getStrainResult().getQuality()[1];
                        break;
                    case Q_EX:
                        res = tc.getResult(roundZero, round);
                        results2D = res == null ? null : res.getStrainResult().getQuality()[0];
                        break;
                    case Q_EY:
                        res = tc.getResult(roundZero, round);
                        results2D = res == null ? null : res.getStrainResult().getQuality()[1];
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported direction - " + dir);
                }

                if ((results3D == null && results2D == null) || results3D.length < x || results3D[0].length < y || results3D[x][y] == null) {
                    data[round] = 0;
                } else {
                    switch (dir) {
                        case D_DX:
                        case D_DY:
                        case D_DABS:
                        case DX:
                        case DY:
                        case DABS:
                            data[round] = ExportUtils.calculateDisplacement(results3D[x][y], dir);
                            break;
                        case D_EXX:
                        case D_EYY:
                        case D_EXY:
                        case D_EABS:
                        case EXX:
                        case EYY:
                        case EXY:
                        case EABS:
                            data[round] = ExportUtils.calculateStrain(results3D[x][y], dir);
                            break;
                        case R_DX:
                        case R_DY:
                        case R_DABS:
                            data[round] = ExportUtils.calculateSpeed(results3D[x][y], dir, time);
                            break;
                        case Q_D_D:
                        case Q_D:
                        case Q_D_EX:
                        case Q_D_EY:
                        case Q_EX:
                        case Q_EY:
                            data[round] = results2D[x][y];
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported direction - " + dir);
                    }

                    if (dir.isMm()) {
                        data[round] *= pxToMm;
                    }
                }
            }
        }

        return result;
    }
}
