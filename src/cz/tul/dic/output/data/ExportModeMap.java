/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.output.data;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
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
    public double[][] exportData(final TaskContainer tc, final Direction direction, final int[] dataParams) throws ComputationException {
        if (dataParams == null || dataParams.length < 1) {
            throw new IllegalArgumentException("Not wnough input parameters (position required).");
        }

        Result res;
        final int round = dataParams[0];
        final int roundZero = TaskContainerUtils.getFirstRound(tc);
        final double[][][] results;
        switch (direction) {
            case dDx:
            case dDy:
            case dDabs:
            case rDx:
            case rDy:
            case rDabs:
                res = tc.getResult(round - 1, round);
                results = res == null ? null : res.getDisplacementResult().getDisplacement();
                break;
            case Dx:
            case Dy:
            case Dabs:
                res = tc.getResult(roundZero, round);
                results = res == null ? null : res.getDisplacementResult().getDisplacement();
                break;
            case dExx:
            case dEyy:
            case dExy:
            case dEabs:
                res = tc.getResult(round - 1, round);
                results = res == null ? null : res.getStrainResult().getStrain();
                break;
            case Exx:
            case Eyy:
            case Exy:
            case Eabs:
                res = tc.getResult(roundZero, round);
                results = res == null ? null : res.getStrainResult().getStrain();
                break;
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction.");
        }
        if (results == null || results.length == 0 || results[0].length == 0) {
            return null;
        }

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
                    case rDx:
                    case rDy:
                    case rDabs:
                        result[x][y] = ExportUtils.calculateSpeed(results[x][y], direction, time);
                        break;
                    default:
                        throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction.");
                }

                if (direction.isMm()) {
                    result[x][y] *= pxToMm;
                }
            }
        }

        // stretch result to ending ROI
        if (direction.isStretch()) {
            final DisplacementResult dr = tc.getResult(round - 1, round).getDisplacementResult();
            if (dr != null && dr.getDisplacement() != null) {
                final double stretchFactor = TaskContainerUtils.getStretchFactor(tc, round);

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

    private double interpolate(final double val1, final double val2, final double ratio) {
        if (Double.isNaN(val1) || Double.isNaN(val2)) {
            return Double.NaN;
        } else {
            return (int) ((val1 * ratio) + (val2 * (1 - ratio)));
        }
    }

}
