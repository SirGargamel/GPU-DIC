/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic;

import cz.tul.dic.complextask.ComplexTaskSolver;
import cz.tul.dic.data.roi.CircularROI;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.generators.facet.FacetGeneratorMethod;
import cz.tul.dic.input.InputLoader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Petr Ječmen
 */
public class ComplexTaskTest {

    private static final int BASE_ROUND = 0;

    public ComplexTaskTest() {
    }

    @Test
    public void testComplexTask() throws URISyntaxException, ComputationException, IOException {
        Assert.assertEquals(0.0, computeTask("ComplexIn.bmp"), 0.01);
        Assert.assertEquals(1.0, computeTask("ComplexOut-1.0.bmp"), 0.5);
        Assert.assertEquals(3.0, computeTask("ComplexOut-3.0.bmp"), 0.5);
    }
    
    private double computeTask(final String fileOut) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/ComplexIn.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/" + fileOut).toURI()).toFile());
        final TaskContainer tc = new TaskContainer(input);

        InputLoader.loadInput(tc);

        tc.setParameter(TaskParameter.ROUND_LIMITS, new int[]{0, 1});
        tc.setParameter(TaskParameter.FACET_SIZE, 40);
        tc.setParameter(TaskParameter.FACET_GENERATOR_METHOD, FacetGeneratorMethod.CLASSIC);
        tc.setParameter(TaskParameter.FACET_GENERATOR_PARAM, 0);

        final ComplexTaskSolver cts = new ComplexTaskSolver();
        assert (!cts.isValidComplexTask(tc));

        final CircularROI cRoi1 = new CircularROI(46, 16, 15);
        final CircularROI cRoi2 = new CircularROI(125, 14, 15);
        final CircularROI cRoi3 = new CircularROI(46, 88, 15);
        final CircularROI cRoi4 = new CircularROI(124, 88, 15);
        tc.addRoi(BASE_ROUND, cRoi1);
        tc.addRoi(BASE_ROUND, cRoi2);
        tc.addRoi(BASE_ROUND, cRoi3);
        tc.addRoi(BASE_ROUND, cRoi4);

        assert (cts.isValidComplexTask(tc));

        cts.solveComplexTask(tc);

        Assert.assertNotNull(cts.getBottomShifts());
        Assert.assertSame(cts.getBottomShifts().size(), 1);        
        return cts.getBottomShifts().get(0);
    }
}
