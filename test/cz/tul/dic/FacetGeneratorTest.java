/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.roi.AbstractROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.subset.generator.SubsetGenerator;
import cz.tul.dic.data.subset.generator.SubsetGeneratorMethod;
import cz.tul.dic.data.task.loaders.InputLoader;
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
public class FacetGeneratorTest {

    private static final int ROUND = 0;
    private static final AbstractROI ROI_FULL = new RectangleROI(0, 0, 28, 28);
    private static final AbstractROI ROI_CENTER = new RectangleROI(10, 10, 20, 20);

    @Test
    public void testEqual() throws IOException, URISyntaxException, ComputationException {
        TaskContainer tc = prepareTask(ROI_FULL, 14, SubsetGeneratorMethod.EQUAL, 1);
        Map<AbstractROI, List<AbstractSubset>> data = SubsetGenerator.generateSubsets(tc, ROUND);
        Assert.assertEquals(1, data.get(ROI_FULL).size());

        tc = prepareTask(ROI_FULL, 13, SubsetGeneratorMethod.EQUAL, 1);
        data = SubsetGenerator.generateSubsets(tc, ROUND);
        Assert.assertEquals(9, data.get(ROI_FULL).size());

        tc = prepareTask(ROI_FULL, 9, SubsetGeneratorMethod.EQUAL, 1);
        data = SubsetGenerator.generateSubsets(tc, ROUND);
        Assert.assertEquals(121, data.get(ROI_FULL).size());

        tc = prepareTask(ROI_FULL, 8, SubsetGeneratorMethod.EQUAL, 2);
        data = SubsetGenerator.generateSubsets(tc, ROUND);
        Assert.assertEquals(36, data.get(ROI_FULL).size());

        tc = prepareTask(ROI_CENTER, 6, SubsetGeneratorMethod.EQUAL, 1);
        data = SubsetGenerator.generateSubsets(tc, ROUND);
        Assert.assertEquals(0, data.get(ROI_CENTER).size());
    }

    private TaskContainer prepareTask(final AbstractROI roi, final int subsetSize, SubsetGeneratorMethod mode, final int spacing) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());

        TaskContainer tc = TaskContainer.initTaskContainer(input);        

        tc.addRoi(ROUND, roi);

        tc.setParameter(TaskParameter.FACET_SIZE, subsetSize);
        tc.setParameter(TaskParameter.FACET_GENERATOR_METHOD, mode);
        tc.setParameter(TaskParameter.FACET_GENERATOR_PARAM, spacing);

        return tc;
    }
}
