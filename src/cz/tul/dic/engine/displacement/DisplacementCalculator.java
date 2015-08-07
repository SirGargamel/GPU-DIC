/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.displacement;

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
import java.util.LinkedList;
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

    public static DisplacementResult computeDisplacement(final Map<AbstractROI, List<CorrelationResult>> correlationResults, Map<AbstractROI, List<AbstractSubset>> allSubsets, final TaskContainer tc, final int round) {
        final Object o = tc.getParameter(TaskParameter.DISPLACEMENT_CALCULATION_METHOD);
        if (o == null) {
            throw new IllegalArgumentException("NULL displacement calculation type.");
        }
        final DisplacementCalculation type = (DisplacementCalculation) o;

        if (DATA.containsKey(type)) {
            Logger.trace("Calculationg displacement for round {0} using {1}.", round, type);
            return DATA.get(type).buildFinalResults(correlationResults, allSubsets, tc, round);
        } else {
            throw new IllegalArgumentException("Unsupported displacement calculation - " + type.toString());
        }
    }

    abstract DisplacementResult buildFinalResults(final Map<AbstractROI, List<CorrelationResult>> correlationResults, Map<AbstractROI, List<AbstractSubset>> allSubsets, final TaskContainer tc, final int round);

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

            final List<double[][][]> resultsCascade = findResultsCascade(tc, roundFrom, roundTo);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    computeDisplacement(resultsCascade, resultData, x, y);
                }
            }

            displacement = new DisplacementResult(resultData, null);
        }

        return displacement;
    }

    private static List<double[][][]> findResultsCascade(final TaskContainer tc, final int roundFrom, final int roundTo) {
        final List<double[][][]> result = new LinkedList<>();
        int indexFrom = roundFrom;
        int indexTo = roundTo;
        Result tempResult;
        while (indexFrom != roundTo) {
            do {
                tempResult = tc.getResult(indexFrom, indexTo);
                if (tempResult != null) {
                    result.add(tempResult.getDisplacementResult().getDisplacement());
                    indexFrom = indexTo;
                    indexTo = roundTo;
                } else {
                    indexTo--;
                }
            } while (indexTo >= 0);
        }

        return result;
    }

    private static void computeDisplacement(final List<double[][][]> resultsCascade, final double[][][] resultData, int x, int y) {
        double posX = x;
        double posY = y;

        double[] val;
        boolean found = false;
        for (double[][][] data : resultsCascade) {
            val = interpolate(posX, posY, data);
            if (val != null) {
                found = true;
                posX += val[Coordinates.X];
                posY += val[Coordinates.Y];
            } else {
                break;
            }
        }

        if (found) {
            resultData[x][y] = new double[]{posX - x, posY - y};
        }
    }

    private static double[] interpolate(final double x, final double y, final double[][][] data) {
        double[] result = new double[INTERPOLATION_DIM];

        final int intX = (int) x;
        final double dX = x - intX;
        final int intY = (int) y;
        final double dY = y - intY;

        try {
            calculateValue(result, data, intX, intY, dX, dY);
        } catch (ArrayIndexOutOfBoundsException | NullPointerException ex) {
            result = null;
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
