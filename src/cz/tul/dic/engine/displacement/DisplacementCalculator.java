/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.displacement;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.AbstractROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.data.result.DisplacementResult;
import cz.tul.dic.data.result.Result;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public abstract class DisplacementCalculator {

    private static final int INTERPOLATION_DIM = 2;
    private static final Map<DisplacementCalculation, DisplacementCalculator> DATA;

    static {
        DATA = new HashMap<>();
        DATA.put(DisplacementCalculation.MAX_WEIGHTED_AVERAGE, new MaxAndWeightedAverage());
    }

    public static DisplacementResult computeDisplacement(final Map<AbstractROI, List<CorrelationResult>> correlationResults, Map<AbstractROI, List<AbstractSubset>> allSubsets, final TaskContainer tc, final int round) throws ComputationException {
        final Object o = tc.getParameter(TaskParameter.DISPLACEMENT_CALCULATION_METHOD);
        if (o == null) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "NULL displacement calculation type.");
        }
        final DisplacementCalculation type = (DisplacementCalculation) o;

        if (DATA.containsKey(type)) {
            Logger.trace("Calculationg displacement for round {0} using {1}.", round, type);
            return DATA.get(type).buildFinalResults(correlationResults, allSubsets, tc, round);
        } else {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported displacement calculation - " + type.toString());
        }
    }

    abstract DisplacementResult buildFinalResults(final Map<AbstractROI, List<CorrelationResult>> correlationResults, Map<AbstractROI, List<AbstractSubset>> allSubsets, final TaskContainer tc, final int round) throws ComputationException;

    public static DisplacementResult computeCumulativeDisplacement(final TaskContainer tc, final int roundFrom, final int roundTo) {
        if (roundFrom >= roundTo) {
            throw new IllegalArgumentException("Source round must be lower than target round.");
        }

        DisplacementResult displacement = null;
        Result tempResult = tc.getResult(roundFrom, roundTo);
        if (tempResult != null) {
            displacement = tempResult.getDisplacementResult();
        }

        if (displacement == null) {
            final Image img = tc.getImage(roundFrom);
            final int width = img.getWidth();
            final int height = img.getHeight();
            final double[][][] resultData = new double[width][height][];

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    computeDisplacement(roundFrom, roundTo, tc, resultData, x, y);
                }
            }

            displacement = new DisplacementResult(resultData, null);
        }

        return displacement;
    }

    private static void computeDisplacement(int roundFrom, final int roundTo, final TaskContainer tc, final double[][][] resultData, int x, int y) {
        Result tempResult;
        double[][][] data;
        double[] val;
        int indexFrom = roundFrom;
        int indexTo = roundTo;
        double posX = x;
        double posY = y;
        while (indexFrom != roundTo) {
            do {
                tempResult = tc.getResult(indexFrom, indexTo);

                if (tempResult != null) {
                    data = tempResult.getDisplacementResult().getDisplacement();
                } else {
                    data = null;
                }
                if (data == null) {
                    indexTo--;
                }
            } while (data == null && indexTo >= 0);
            if (data == null) {
                break;
            }

            val = interpolate(posX, posY, data);
            posX += val[Coordinates.X];
            posY += val[Coordinates.Y];

            indexFrom = indexTo;
            indexTo = roundTo;

            if (posX < 0 || posY < 0 || posX > data.length - 1 || posY > data[0].length - 1) {
                break;
            }
        }
        resultData[x][y] = new double[]{posX - x, posY - y};
    }

    private static double[] interpolate(final double x, final double y, final double[][][] data) {
        if (data.length == 0 || data[0].length == 0) {
            throw new IllegalArgumentException("Zero length data not supported.");
        }
        if (x > data.length - 1 || y > data[0].length - 1) {
            throw new IllegalArgumentException("Indexes out of bounds.");
        }
        final double[] result = new double[INTERPOLATION_DIM];

        final int intX = (int) x;
        final double dX = x - intX;
        final int intY = (int) y;
        final double dY = y - intY;

        if (data[intX][intY] != null) {
            calculateValue(result, data, intX, intY, dX, dY);
        }

        return result;
    }

    private static void calculateValue(final double[] result, final double[][][] data, final int intX, final int intY, final double dX, final double dY) {
        for (int dim = 0; dim < INTERPOLATION_DIM; dim++) {
            result[dim] += data[intX][intY][dim] * (1 - dX) * (1 - dY);
            if (intX < data.length - 1) {
                result[dim] += data[intX + 1][intY][dim] * dX * (1 - dY);
                if (intY < data[intX].length - 1) {
                    result[dim] += data[intX + 1][intY + 1][dim] * dX * dY;
                }
            }
            if (intY < data[intX].length - 1) {
                result[dim] += data[intX][intY + 1][dim] * (1 - dX) * dY;
            }
        }
    }

}
