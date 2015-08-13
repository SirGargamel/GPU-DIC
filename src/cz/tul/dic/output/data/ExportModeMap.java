/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.output.data;

import cz.tul.dic.FpsManager;
import cz.tul.dic.data.result.DisplacementResult;
import cz.tul.dic.data.result.Result;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportUtils;

public class ExportModeMap implements IExportMode<double[][]> {

    @Override
    public double[][] exportData(final TaskContainer tc, final Direction direction, final int[] dataParams) {
        if (dataParams == null || dataParams.length < 1) {
            throw new IllegalArgumentException("Not wnough input parameters (position required).");
        }

        final int round = dataParams[0];
        final int roundZero = TaskContainerUtils.getFirstRound(tc);
        double[][] result = extractData(direction, tc, round, roundZero);

        // stretch result to ending ROI
        if (direction.isStretch() && result != null) {
            final DisplacementResult dr = tc.getResult(round - 1, round).getDisplacementResult();
            if (dr != null && dr.getDisplacement() != null) {
                final double stretchFactor = TaskContainerUtils.getStretchFactor(tc, round);

                final int width = result.length;
                final int height = result[0].length;
                final double[][] stretchedResult = new double[width][height];
                double newY;
                for (int y = 0; y < height - 1; y++) {
                    newY = y / stretchFactor;
                    if (newY > height - 1) {
                        continue;
                    }
                    for (int x = 0; x < width; x++) {
                        stretchedResult[x][y] = interpolate(
                                result[x][(int) Math.floor(newY)],
                                result[x][(int) Math.ceil(newY)],
                                newY % 1);
                    }
                }
                result = stretchedResult;
            }
        }

        return result;
    }
    
    private static double[][] extractData(final Direction direction, final TaskContainer tc, final int round, final int roundZero) {
        final double[][] result;
        switch (direction) {
            case D_DX:
            case D_DY:
            case D_DABS:
            case R_DX:
            case R_DY:
            case R_DABS:
            case DX:
            case DY:
            case DABS:
            case D_EXX:
            case D_EYY:
            case D_EXY:
            case D_EABS:
            case EXX:
            case EYY:
            case EXY:
            case EABS:
                final double[][][] results = extractDataResult(direction, tc, round, roundZero);
                if (results == null || results.length == 0 || results[0].length == 0) {
                    result = null;
                    break;
                }

                result = reduceDataDimension(tc, results, direction);
                break;
            case Q_D:
            case Q_D_D:
            case Q_EX:
            case Q_EY:
            case Q_D_EX:
            case Q_D_EY:
                result = extractQualityResult(direction, tc, round, roundZero);
                break;
            default:
                throw new IllegalArgumentException("Unsupported direction - " + direction);
        }

        return result;
    }

    private static double[][][] extractDataResult(final Direction direction, final TaskContainer tc, final int round, final int roundZero) {
        Result res;
        final double[][][] results;
        switch (direction) {
            case D_DX:
            case D_DY:
            case D_DABS:
            case R_DX:
            case R_DY:
            case R_DABS:
                res = tc.getResult(round - 1, round);
                results = res == null ? null : res.getDisplacementResult().getDisplacement();
                break;
            case DX:
            case DY:
            case DABS:
                res = tc.getResult(roundZero, round);
                results = res == null ? null : res.getDisplacementResult().getDisplacement();
                break;
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
                throw new IllegalArgumentException("Unsupported direction - " + direction);
        }
        return results;
    }

    private static double[][] reduceDataDimension(final TaskContainer tc, final double[][][] results, final Direction direction) {
        final int width = results.length;
        final int height = results[0].length;

        double[][] result = new double[width][height];
        final FpsManager fpsM = new FpsManager(tc);
        final double time = fpsM.getTickLength();
        final double pxToMm = 1 / (double) tc.getParameter(TaskParameter.MM_TO_PX_RATIO);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (results[x][y] == null) {
                    result[x][y] = Double.NaN;
                    continue;
                }

                switch (direction) {
                    case D_DX:
                    case D_DY:
                    case D_DABS:
                    case DX:
                    case DY:
                    case DABS:
                        result[x][y] = ExportUtils.calculateDisplacement(results[x][y], direction);
                        break;
                    case D_EXX:
                    case D_EYY:
                    case D_EXY:
                    case D_EABS:
                    case EXX:
                    case EYY:
                    case EXY:
                    case EABS:
                        result[x][y] = ExportUtils.calculateStrain(results[x][y], direction);
                        break;
                    case R_DX:
                    case R_DY:
                    case R_DABS:
                        result[x][y] = ExportUtils.calculateSpeed(results[x][y], direction, time);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported direction - " + direction);
                }

                if (direction.isMm()) {
                    result[x][y] *= pxToMm;
                }
            }
        }
        return result;
    }

    private static double[][] extractQualityResult(final Direction direction, final TaskContainer tc, final int round, final int roundZero) {
        Result res;
        final double[][] results;
        switch (direction) {
            case Q_D_D:
                res = tc.getResult(round - 1, round);
                results = res == null ? null : res.getDisplacementResult().getQuality();
                break;
            case Q_D:
                res = tc.getResult(roundZero, round);
                results = res == null ? null : res.getDisplacementResult().getQuality();
                break;
            case Q_D_EX:
                res = tc.getResult(round - 1, round);
                results = res == null ? null : res.getStrainResult().getQualityX();
                break;
            case Q_D_EY:
                res = tc.getResult(round - 1, round);
                results = res == null ? null : res.getStrainResult().getQualityY();
                break;
            case Q_EX:
                res = tc.getResult(roundZero, round);
                results = res == null ? null : res.getStrainResult().getQualityX();
                break;
            case Q_EY:
                res = tc.getResult(roundZero, round);
                results = res == null ? null : res.getStrainResult().getQualityY();
                break;
            default:
                throw new IllegalArgumentException("Unsupported direction - " + direction);
        }
        return results;
    }

    private double interpolate(final double val1, final double val2, final double ratio) {
        if (Double.isNaN(val1) || Double.isNaN(val2)) {
            return Double.NaN;
        } else {
            return (int) ((val1 * ratio) + (val2 * (1 - ratio)));
        }
    }

}
