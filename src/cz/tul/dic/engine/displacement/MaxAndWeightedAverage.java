/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.displacement;

import cz.tul.dic.ComputationException;
import cz.tul.dic.Utils;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.subset.SubsetUtils;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.AbstractROI;
import cz.tul.dic.data.result.DisplacementResult;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.debug.DebugControl;
import cz.tul.dic.debug.Stats;
import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.data.subset.SubsetDeformator;
import cz.tul.dic.engine.cluster.Analyzer2D;
import cz.tul.dic.output.NameGenerator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.pmw.tinylog.Logger;

public class MaxAndWeightedAverage extends DisplacementCalculator {

    private static final double PRECISION = 0.5;
    private double[][][] finalDisplacement;
    private double[][] finalQuality;

    @Override
    public DisplacementResult buildFinalResults(final Map<AbstractROI, List<CorrelationResult>> correlationResults, Map<AbstractROI, List<AbstractSubset>> allSubsets, final TaskContainer tc, final int round) throws ComputationException {
        final Image img = tc.getImage(round);
        final int width = img.getWidth();
        final int height = img.getHeight();
        final double resultQuality = (double) tc.getParameter(TaskParameter.RESULT_QUALITY);

        final int linesPerGroup = (int) tc.getParameter(TaskParameter.DISPLACEMENT_CALCULATION_PARAM) / width;
        final int groupCount = (int) Math.ceil(height / (double) linesPerGroup);

        finalDisplacement = new double[width][height][];
        finalQuality = Utils.generateNaNarray(width, height);
        final Map<Integer, Map<Integer, Analyzer2D>> counters = new HashMap<>();
        int lowerBound, upperBound = 0;
        for (int g = 0; g < groupCount; g++) {
            lowerBound = upperBound;
            upperBound += linesPerGroup;
            upperBound = Math.min(upperBound, height - 1);
            counters.clear();

            prepareDeformedSubsetsToCounters(correlationResults, allSubsets, resultQuality, lowerBound, upperBound, counters);

            calculateDisplacementFromCounters(counters, tc, round);
        }

        return new DisplacementResult(finalDisplacement, finalQuality);
    }

    private void prepareDeformedSubsetsToCounters(
            final Map<AbstractROI, List<CorrelationResult>> correlationResults, 
            final Map<AbstractROI, List<AbstractSubset>> allSubsets, final double resultQuality, 
            final int lowerBound, final int upperBound, 
            final Map<Integer, Map<Integer, Analyzer2D>> counters) throws ComputationException {
        final SubsetDeformator deformator = new SubsetDeformator();
        
        List<AbstractSubset> susbets;
        List<CorrelationResult> results;
        CorrelationResult cr;
        double[] d;
        double qualitySum;
        AbstractSubset f;
        Map<int[], double[]> deformedSubset;
        int x;
        int y;
        for (AbstractROI roi : correlationResults.keySet()) {
            susbets = allSubsets.get(roi);
            results = correlationResults.get(roi);

            for (int i = 0; i < susbets.size(); i++) {
                if (results.get(i) == null) {
                    continue;
                }
                cr = results.get(i);
                if (cr.getValue() < resultQuality) {
                    continue;
                }

                d = cr.getDeformation();
                qualitySum = cr.getValue();

                f = susbets.get(i);
                if (f == null) {
                    Logger.warn("No subset - {0}", f);
                    continue;
                }
                if (!SubsetUtils.areLinesInsideSubset(f, lowerBound, upperBound)) {
                    continue;
                }

                deformedSubset = deformator.computePixelDeformationValues(f, d);
                for (Map.Entry<int[], double[]> e : deformedSubset.entrySet()) {
                    x = e.getKey()[Coordinates.X];
                    y = e.getKey()[Coordinates.Y];

                    if (y >= lowerBound && y <= upperBound) {
                        getAnalyzer(counters, x, y).addValue(new double[]{e.getValue()[0], e.getValue()[1], qualitySum});
                    }
                }
            }
        }
    }

    private void calculateDisplacementFromCounters(
            final Map<Integer, Map<Integer, Analyzer2D>> counters, 
            final TaskContainer tc, final int round) {
        int x;
        int y;
        Analyzer2D counter;
        double qualitySum;
        double qualitySumWeighed;
        double[] majorVal, val = new double[2];
        double maxDist2 = 4 * PRECISION * PRECISION;
        for (Entry<Integer, Map<Integer, Analyzer2D>> eX : counters.entrySet()) {
            x = eX.getKey();
            for (Entry<Integer, Analyzer2D> eY : eX.getValue().entrySet()) {
                y = eY.getKey();
                counter = eY.getValue();
                if (counter != null) {
                    majorVal = counter.findMajorValue();

                    val[0] = 0;
                    val[1] = 0;
                    qualitySum = 0;
                    qualitySumWeighed = 0;
                    for (double[] vals : counter.listValues()) {
                        if (dist2(vals, majorVal) <= maxDist2) {
                            qualitySum += vals[2];
                            qualitySumWeighed += vals[2] * vals[2];
                            val[0] += vals[0] * vals[2];
                            val[1] += vals[1] * vals[2];
                        }
                    }

                    finalDisplacement[x][y] = new double[]{val[0] / qualitySum, val[1] / qualitySum};
                    finalQuality[x][y] = qualitySum / qualitySumWeighed;

                    if (DebugControl.isDebugMode()) {
                        Stats.getInstance().exportPointSubResultsStatistics(counter, NameGenerator.generate2DValueHistogram(tc, round, x, y));
                    }
                }
            }
        }
    }

    private Analyzer2D getAnalyzer(final Map<Integer, Map<Integer, Analyzer2D>> maps, final int x, final int y) {
        Map<Integer, Analyzer2D> m = maps.get(x);
        if (m == null) {
            m = new HashMap<>(1);
            maps.put(x, m);
        }

        Analyzer2D result = m.get(y);
        if (result == null) {
            result = new Analyzer2D();
            m.put(y, result);
        }

        return result;
    }

    private static double dist2(final double[] val1, final double[] val2) {
        double a = val2[0] - val1[0];
        double b = val2[1] - val1[1];
        return a * a + b * b;
    }

}
