/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.data.result.DisplacementResult;
import cz.tul.dic.data.result.Result;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.roi.AbstractROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.subset.generator.AbstractSubsetGenerator;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.subset.generator.SubsetGenerator;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Petr Ječmen
 */
public class SubsetGeneratorTest {

    private static final int ROUND = 0;
    private static final AbstractROI ROI_FULL = new RectangleROI(75, 75, 103, 103);
    private static final AbstractROI ROI_CENTER = new RectangleROI(85, 85, 95, 95);

    @Test
    public void testEqual() throws IOException, URISyntaxException, ComputationException {
        TaskContainer tc = prepareTask(ROI_FULL, 14, SubsetGenerator.EQUAL, 1);
        final AbstractSubsetGenerator generator = AbstractSubsetGenerator.initGenerator((SubsetGenerator) tc.getParameter(TaskParameter.SUBSET_GENERATOR_METHOD));

        Map<AbstractROI, List<AbstractSubset>> data = generator.generateSubsets(tc, ROUND);
        Assert.assertEquals(1, data.get(ROI_FULL).size());

        tc = prepareTask(ROI_FULL, 13, SubsetGenerator.EQUAL, 1);
        data = generator.generateSubsets(tc, ROUND);
        Assert.assertEquals(9, data.get(ROI_FULL).size());

        tc = prepareTask(ROI_FULL, 9, SubsetGenerator.EQUAL, 1);
        data = generator.generateSubsets(tc, ROUND);
        Assert.assertEquals(121, data.get(ROI_FULL).size());

        tc = prepareTask(ROI_FULL, 8, SubsetGenerator.EQUAL, 2);
        data = generator.generateSubsets(tc, ROUND);
        Assert.assertEquals(36, data.get(ROI_FULL).size());

        tc = prepareTask(ROI_CENTER, 6, SubsetGenerator.EQUAL, 1);
        data = generator.generateSubsets(tc, ROUND);
        Assert.assertEquals(0, data.get(ROI_CENTER).size());
    }

    private TaskContainer prepareTask(final AbstractROI roi, final int subsetSize, SubsetGenerator mode, final int spacing) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());

        TaskContainer tc = TaskContainer.initTaskContainer(input);

        tc.addRoi(ROUND, roi);

        tc.setParameter(TaskParameter.SUBSET_SIZE, subsetSize);
        tc.setParameter(TaskParameter.SUBSET_GENERATOR_METHOD, mode);
        tc.setParameter(TaskParameter.SUBSET_GENERATOR_PARAM, spacing);

        return tc;
    }

    @Test
    public void testDynamic() throws IOException, URISyntaxException, ComputationException {
        // none previous results
        TaskContainer tc = prepareTask(ROI_FULL, 14, SubsetGenerator.DYNAMIC, 1);
        final AbstractSubsetGenerator generator = AbstractSubsetGenerator.initGenerator((SubsetGenerator) tc.getParameter(TaskParameter.SUBSET_GENERATOR_METHOD));

        Map<AbstractROI, List<AbstractSubset>> data = generator.generateSubsets(tc, ROUND);
        Assert.assertEquals(1, data.get(ROI_FULL).size());

        tc = prepareTask(ROI_FULL, 13, SubsetGenerator.DYNAMIC, 1);
        data = generator.generateSubsets(tc, ROUND);
        Assert.assertEquals(9, data.get(ROI_FULL).size());

        tc = prepareTask(ROI_FULL, 9, SubsetGenerator.DYNAMIC, 1);
        data = generator.generateSubsets(tc, ROUND);
        Assert.assertEquals(121, data.get(ROI_FULL).size());

        tc = prepareTask(ROI_FULL, 8, SubsetGenerator.DYNAMIC, 2);
        data = generator.generateSubsets(tc, ROUND);
        Assert.assertEquals(36, data.get(ROI_FULL).size());

        tc = prepareTask(ROI_CENTER, 6, SubsetGenerator.DYNAMIC, 1);
        data = generator.generateSubsets(tc, ROUND);
        Assert.assertEquals(0, data.get(ROI_CENTER).size());

        // dynamic generations with results
        tc = prepareTask(ROI_FULL, 8, SubsetGenerator.DYNAMIC, 2);        
        Result result = new Result(new DisplacementResult(generateDisplacement(0.25), null));
        tc.setResult(ROUND, ROUND + 1, result);
        data = generator.generateSubsets(tc, ROUND + 1);
        Assert.assertEquals(36, data.get(ROI_FULL).size());                
        
        tc = prepareTask(ROI_FULL, 8, SubsetGenerator.DYNAMIC, 2);        
        result = new Result(new DisplacementResult(generateDisplacement(2), null));
        tc.setResult(ROUND, ROUND + 1, result);
        data = generator.generateSubsets(tc, ROUND + 1);
        Assert.assertEquals(66, data.get(ROI_FULL).size());
    }

    private static double[][][] generateDisplacement(final double step) {
        final double[][][] displacement = new double[180][180][2];
        for (int x = 90; x < 180; x++) {
            for (int y = 0; y < 180; y++) {
                displacement[x][y][0] = step;
            }
        }
        return displacement;
    }
}
