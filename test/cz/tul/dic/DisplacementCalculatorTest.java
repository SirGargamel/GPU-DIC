package cz.tul.dic;

import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.ResultCompilation;
import cz.tul.dic.engine.displacement.DisplacementCalculation;
import cz.tul.dic.engine.displacement.DisplacementCalculator;
import cz.tul.dic.generators.facet.FacetGenerator;
import cz.tul.dic.generators.facet.FacetGeneratorMethod;
import cz.tul.dic.input.InputLoader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 *
 * @author Petr Ječmen
 */
public class DisplacementCalculatorTest {

    private static final int ROUND = 0;

    @Test
    public void testDisplacementCalculator() throws IOException, URISyntaxException, ComputationException {
        TaskContainer tc = prepareAndComputeDisplacement(new double[]{2, 0, 0, 0, 0, 0});
        checkResults(tc, 2, 0);

        tc = prepareAndComputeDisplacement(new double[]{0, 2, 0, 0, 0, 0});
        checkResults(tc, 0, 2);

        tc = prepareAndComputeDisplacement(new double[]{2, -2, 0, 0, 0, 0});
        checkResults(tc, 2, -2);
    }

    private TaskContainer prepareAndComputeDisplacement(final double[] deformations) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());

        TaskContainer tc = new TaskContainer(input);
        InputLoader.loadInput(tc);

        ROI roi = new RectangleROI(10, 10, 20, 20);

        tc.addRoi(ROUND, roi);
        tc.setDeformationLimits(ROUND, roi, deformations);

        tc.setParameter(TaskParameter.FACET_SIZE, 11);
        tc.setParameter(TaskParameter.FACET_GENERATOR_METHOD, FacetGeneratorMethod.CLASSIC);
        tc.setParameter(TaskParameter.FACET_GENERATOR_PARAM, 0);
        tc.setParameter(TaskParameter.DISPLACEMENT_CALCULATION_METHOD, DisplacementCalculation.FIND_MAX_AND_AVERAGE);
        tc.setParameter(TaskParameter.DISPLACEMENT_CALCULATION_PARAM, 2000);
        tc.setParameter(TaskParameter.RESULT_COMPILATION, ResultCompilation.MAJOR_AVERAGING);

        final List<double[][]> results = new ArrayList<>(1);
        final double[][] result = new double[1][];
        result[0] = deformations;
        results.add(result);
        tc.setResult(ROUND, roi, results);

        DisplacementCalculator.computeDisplacement(tc, ROUND, FacetGenerator.generateFacets(tc, ROUND));

        return tc;
    }

    private void checkResults(final TaskContainer tc, final double dx, final double dy) {
        double[][][] results = tc.getDisplacement(ROUND);
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
