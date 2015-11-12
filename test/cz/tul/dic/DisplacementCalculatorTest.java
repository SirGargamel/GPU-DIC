/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.data.Coordinates;
import cz.tul.dic.data.roi.AbstractROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.engine.displacement.DisplacementCalculation;
import cz.tul.dic.engine.displacement.DisplacementCalculator;
import cz.tul.dic.data.result.DisplacementResult;
import cz.tul.dic.data.result.Result;
import cz.tul.dic.data.subset.generator.AbstractSubsetGenerator;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.subset.generator.SubsetGenerator;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 *
 * @author Petr Ječmen
 */
public class DisplacementCalculatorTest {

    private static final int ROUND = 0;

    @Test
    public void testDisplacementCalculator() throws IOException, URISyntaxException, ComputationException {
        DisplacementResult result = prepareAndComputeDisplacement(new CorrelationResult(1.0, new double[]{2, 0, 0, 0, 0, 0}));
        checkResults(result, 2, 0, 100.0);

        result = prepareAndComputeDisplacement(new CorrelationResult(0.5, new double[]{0, 2, 0, 0, 0, 0}));
        checkResults(result, 0, 2, 75.0);

        result = prepareAndComputeDisplacement(new CorrelationResult(0.25, new double[]{2, -2, 0, 0, 0, 0}));
        checkResults(result, 2, -2, 62.5);
    }

    private DisplacementResult prepareAndComputeDisplacement(final CorrelationResult deformation) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());

        final TaskContainer tc = TaskContainer.initTaskContainer(input);

        final AbstractROI roi = new RectangleROI(10, 10, 20, 20);

        tc.addRoi(ROUND, roi);
        tc.setDeformationLimits(ROUND, roi, deformation.getDeformation());

        tc.setParameter(TaskParameter.SUBSET_SIZE, 5);
        tc.setParameter(TaskParameter.SUBSET_GENERATOR_METHOD, SubsetGenerator.EQUAL);
        tc.setParameter(TaskParameter.SUBSET_GENERATOR_PARAM, 11);
        tc.setParameter(TaskParameter.DISPLACEMENT_CALCULATION_METHOD, DisplacementCalculation.MAX_WEIGHTED_AVERAGE);
        tc.setParameter(TaskParameter.DISPLACEMENT_CALCULATION_PARAM, 2000);
        tc.setParameter(TaskParameter.RESULT_QUALITY, 0.25);

        final List<CorrelationResult> results = new ArrayList<>(1);
        results.add(deformation);
        final Map<AbstractROI, List<CorrelationResult>> resultMap = new HashMap<>(1);
        resultMap.put(roi, results);

        final AbstractSubsetGenerator generator = AbstractSubsetGenerator.initGenerator((SubsetGenerator) tc.getParameter(TaskParameter.SUBSET_GENERATOR_METHOD));
        return DisplacementCalculator.computeDisplacement(resultMap, generator.generateSubsets(tc, ROUND), tc, ROUND);
    }

    private void checkResults(final DisplacementResult result, final double dx, final double dy, final double q) {
        final double[][][] results = result.getDisplacement();
        final double[][] quality = result.getQuality();

        for (int x = 0; x < results.length; x++) {
            for (int y = 0; y < results[x].length; y++) {
                if (results[x][y] != null) {
                    assert (results[x][y].length == 2);
                    assert (results[x][y][0] == dx);
                    assert (results[x][y][1] == dy);
                    assert (quality[x][y] == q);
                }
            }
        }
    }

    @Test
    public void testCumulativeResultCounter() throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(4);
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());
        final TaskContainer tc = TaskContainer.initTaskContainer(input);
        tc.setParameter(TaskParameter.IN, input.get(0));
        TaskContainerUtils.checkTaskValidity(tc);

        final int width = tc.getImage(ROUND).getWidth();
        final int height = tc.getImage(ROUND).getHeight();
        tc.setResult(0, 1, new Result(new DisplacementResult(prepareArray3D(width, height, 0), prepareArray2D(width, height, 100))));
        tc.setResult(1, 2, new Result(new DisplacementResult(prepareArray3D(width, height, 0), prepareArray2D(width, height, 100))));
        tc.setResult(2, 3, new Result(new DisplacementResult(prepareArray3D(width, height, 1), prepareArray2D(width, height, 50))));
        tc.setResult(3, 4, new Result(new DisplacementResult(prepareArray3D(width, height, 1), prepareArray2D(width, height, 50))));

        tc.setResult(0, 2, new Result(DisplacementCalculator.computeCumulativeDisplacement(tc, 0, 2)));
        tc.setResult(0, 3, new Result(DisplacementCalculator.computeCumulativeDisplacement(tc, 0, 3)));
        tc.setResult(0, 4, new Result(DisplacementCalculator.computeCumulativeDisplacement(tc, 0, 4)));

        assert equals(tc.getResult(0, 1).getDisplacementResult().getDisplacement(), prepareArray3D(width, height, 0), 0);
        assert equals(tc.getResult(0, 1).getDisplacementResult().getQuality(), prepareArray2D(width, height, 100.0), 0);
        assert equals(tc.getResult(0, 2).getDisplacementResult().getDisplacement(), prepareArray3D(width, height, 0), 0);
        assert equals(tc.getResult(0, 2).getDisplacementResult().getQuality(), prepareArray2D(width, height, 100.0), 0);
        assert equals(tc.getResult(0, 3).getDisplacementResult().getDisplacement(), prepareArray3D(width, height, 1), 0);
        assert equals(tc.getResult(0, 3).getDisplacementResult().getQuality(), prepareArray2D(width, height, 150 / 2.0), 0);
        assert equals(tc.getResult(0, 4).getDisplacementResult().getDisplacement(), prepareArray3D(width, height, 2), 1);
        assert equals(tc.getResult(0, 4).getDisplacementResult().getQuality(), prepareArray2D(width, height, 125 / 2.0), 1);
    }

    private double[][][] prepareArray3D(final int width, final int height, final double val) {
        final double[][][] result = new double[width][height][Coordinates.DIMENSION];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Arrays.fill(result[x][y], val);
            }
        }
        return result;
    }

    private double[][] prepareArray2D(final int width, final int height, final double val) {
        final double[][] result = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                result[x][y] = val;
            }
        }
        return result;
    }

    private boolean equals(final double[][][] A, final double[][][] B, final int gap) {
        boolean result = true;

        if (A != null && B != null) {
            loop:
            for (int x = 0; x < A.length - gap; x++) {
                for (int y = 0; y < A[x].length - gap; y++) {
                    if (A[x][y] == null || B[x][y] == null) {
                        if (!(A[x][y] == null && B[x][y] == null)) {
                            result = false;
                            break loop;
                        }
                    } else {
                        for (int z = 0; z < A[x][y].length; z++) {
                            if (A[x][y][z] != B[x][y][z]) {
                                result = false;
                                break loop;
                            }
                        }
                    }
                }
            }
        } else {
            result = false;
        }

        return result;
    }

    private boolean equals(final double[][] A, final double[][] B, final int gap) {
        boolean result = true;

        if (A != null && B != null) {
            loop:
            for (int x = 0; x < A.length - gap; x++) {
                for (int y = 0; y < A[x].length - gap; y++) {
                    if (A[x] == null || B[x] == null) {
                        if (!(A[x] == null && B[x] == null)) {
                            result = false;
                            break loop;
                        }
                    } else {
                        if (A[x][y] != B[x][y]) {
                            result = false;
                            break loop;
                        }

                    }
                }
            }
        } else {
            result = false;
        }

        return result;
    }
}
