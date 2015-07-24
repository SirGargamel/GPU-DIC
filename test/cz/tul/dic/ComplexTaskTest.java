/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.complextask.ComplexTaskSolver;
import cz.tul.dic.data.roi.CircularROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.subset.generator.SubsetGeneratorMethod;
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
        Assert.assertEquals(1.0, computeTask("ComplexOut-2.0.bmp"), 0.01);
        Assert.assertEquals(3.0, computeTask("ComplexOut-4.0.bmp"), 0.5);
        Assert.assertEquals(27.0, computeTask("ComplexOut-30.0.bmp"), 0.5);
    }

    private double computeTask(final String fileOut) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/complex/ComplexIn.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/complex/" + fileOut).toURI()).toFile());
        final TaskContainer tc = TaskContainer.initTaskContainer(input);

        tc.setParameter(TaskParameter.IN, input.get(0));
        tc.setParameter(TaskParameter.ROUND_LIMITS, new int[]{0, 1});
        tc.setParameter(TaskParameter.FACET_SIZE, 18);
        tc.setParameter(TaskParameter.FACET_GENERATOR_METHOD, SubsetGeneratorMethod.EQUAL);
        tc.setParameter(TaskParameter.FACET_GENERATOR_PARAM, 40);

        final ComplexTaskSolver cts = new ComplexTaskSolver();
        assert (!cts.isValidComplexTask(tc));

        final CircularROI cRoi1 = new CircularROI(46, 16, 19);
        final CircularROI cRoi2 = new CircularROI(125, 14, 19);
        final CircularROI cRoi3 = new CircularROI(46, 88, 19);
        final CircularROI cRoi4 = new CircularROI(124, 88, 19);
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
