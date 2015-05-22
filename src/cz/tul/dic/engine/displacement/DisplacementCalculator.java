/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.displacement;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.ROI;
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

    private static final Map<DisplacementCalculation, DisplacementCalculator> DATA;

    static {
        DATA = new HashMap<>();
        DATA.put(DisplacementCalculation.MAX_WEIGHTED_AVERAGE, new FindMaxAndAverage());
    }

    public static DisplacementResult computeDisplacement(final Map<ROI, List<CorrelationResult>> correlationResults, Map<ROI, List<Facet>> facetMap, final TaskContainer tc, final int round) throws ComputationException {
        final Object o = tc.getParameter(TaskParameter.DISPLACEMENT_CALCULATION_METHOD);
        if (o == null) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "NULL displacement calculation type.");
        }
        final DisplacementCalculation type = (DisplacementCalculation) o;

        if (DATA.containsKey(type)) {
            Logger.trace("Calculationg displacement for round {0} using {1}.", round, type);
            return DATA.get(type).buildFinalResults(correlationResults, facetMap, tc, round);
        } else {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported strain estimation - " + type.toString());
        }
    }

    abstract DisplacementResult buildFinalResults(final Map<ROI, List<CorrelationResult>> correlationResults, Map<ROI, List<Facet>> facetMap, final TaskContainer tc, final int round) throws ComputationException;

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

            double posX, posY;
            int iX, iY;
            double[][][] data;
            double[] val;
            int indexFrom, indexTo;
            boolean notNull, inited;
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    notNull = false;
                    inited = false;
                    indexFrom = roundFrom;
                    indexTo = roundTo;
                    posX = x;
                    posY = y;
                    iX = x;
                    iY = y;

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

                        val = data[iX][iY];
                        if (val != null) {
                            notNull = true;
                            posX += val[Coordinates.X];
                            posY += val[Coordinates.Y];
                        } else if (!inited) {
                            break;
                        }
                        inited = true;

                        indexFrom = indexTo;
                        indexTo = roundTo;

                        iX = (int) Math.round(posX);
                        iY = (int) Math.round(posY);
                        if (posX < 0 || posY < 0 || iX >= data.length || iY >= data[x].length) {
                            break;
                        }
                    }

                    if (notNull) {
                        resultData[x][y] = new double[]{posX - x, posY - y};
                    }
                }
            }

            displacement = new DisplacementResult(resultData, null);
        }

        return displacement;
    }

}
