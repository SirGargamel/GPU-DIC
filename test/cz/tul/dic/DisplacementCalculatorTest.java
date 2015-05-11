/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.result.CorrelationResult;
import cz.tul.dic.engine.displacement.ResultCompilation;
import cz.tul.dic.engine.displacement.DisplacementCalculation;
import cz.tul.dic.engine.displacement.DisplacementCalculator;
import cz.tul.dic.data.result.DisplacementResult;
import cz.tul.dic.generators.facet.FacetGenerator;
import cz.tul.dic.generators.facet.FacetGeneratorMethod;
import cz.tul.dic.input.InputLoader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
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
        DisplacementResult result = prepareAndComputeDisplacement(new CorrelationResult(1, new double[]{2, 0, 0, 0, 0, 0}));
        checkResults(result, 2, 0);

        result = prepareAndComputeDisplacement(new CorrelationResult(1, new double[]{0, 2, 0, 0, 0, 0}));
        checkResults(result, 0, 2);

        result = prepareAndComputeDisplacement(new CorrelationResult(1, new double[]{2, -2, 0, 0, 0, 0}));
        checkResults(result, 2, -2);
    }

    private DisplacementResult prepareAndComputeDisplacement(final CorrelationResult deformation) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());

        TaskContainer tc = new TaskContainer(input);
        InputLoader.loadInput(tc);

        ROI roi = new RectangleROI(10, 10, 20, 20);

        tc.addRoi(ROUND, roi);
        tc.setDeformationLimits(ROUND, roi, deformation.getDeformation());

        tc.setParameter(TaskParameter.FACET_SIZE, 11);
        tc.setParameter(TaskParameter.FACET_GENERATOR_METHOD, FacetGeneratorMethod.CLASSIC);
        tc.setParameter(TaskParameter.FACET_GENERATOR_PARAM, 0);
        tc.setParameter(TaskParameter.DISPLACEMENT_CALCULATION_METHOD, DisplacementCalculation.FIND_MAX_AND_AVERAGE);
        tc.setParameter(TaskParameter.DISPLACEMENT_CALCULATION_PARAM, 2000);
        tc.setParameter(TaskParameter.RESULT_COMPILATION, ResultCompilation.MAJOR_AVERAGING);
        tc.setParameter(TaskParameter.RESULT_QUALITY, 0.5);

        final List<CorrelationResult> results = new ArrayList<>(1);
        results.add(deformation);
        final Map<ROI, List<CorrelationResult>> resultMap = new HashMap<>(1);
        resultMap.put(roi, results);        

        return DisplacementCalculator.computeDisplacement(resultMap, FacetGenerator.generateFacets(tc, ROUND), tc, ROUND);        
    }

    private void checkResults(final DisplacementResult result, final double dx, final double dy) {
        double[][][] results = result.getDisplacement();
        for (double[][] dAA : results) {
            for (double[] dA : dAA) {
                if (dA != null) {
                    assert (dA.length == 2);
                    assert (dA[0] == dx);
                    assert (dA[1] == dy);
                }
            }
        }
    }
}
