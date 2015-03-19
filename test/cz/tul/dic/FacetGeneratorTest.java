/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.generators.facet.FacetGenerator;
import cz.tul.dic.generators.facet.FacetGeneratorMethod;
import cz.tul.dic.input.InputLoader;
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
    private static final ROI ROI_FULL = new RectangleROI(0, 0, 29, 29);
    private static final ROI ROI_CENTER = new RectangleROI(10, 10, 20, 20);
    
    @Test
    public void testClassic() throws IOException, URISyntaxException, ComputationException {        
        TaskContainer tc = prepareTask(ROI_FULL, 30, FacetGeneratorMethod.CLASSIC, 0);
        Map<ROI, List<Facet>> data = FacetGenerator.generateFacets(tc, ROUND);
        Assert.assertEquals(1, data.get(ROI_FULL).size());
        
        tc = prepareTask(ROI_FULL, 31, FacetGeneratorMethod.CLASSIC, 0);
        data = FacetGenerator.generateFacets(tc, ROUND);
        Assert.assertEquals(0, data.get(ROI_FULL).size());
        
        tc = prepareTask(ROI_FULL, 15, FacetGeneratorMethod.CLASSIC, 0);
        data = FacetGenerator.generateFacets(tc, ROUND);
        Assert.assertEquals(4, data.get(ROI_FULL).size());
        
        tc = prepareTask(ROI_FULL, 16, FacetGeneratorMethod.CLASSIC, 0);
        data = FacetGenerator.generateFacets(tc, ROUND);
        Assert.assertEquals(1, data.get(ROI_FULL).size());
        
        tc = prepareTask(ROI_FULL, 16, FacetGeneratorMethod.CLASSIC, 2);
        data = FacetGenerator.generateFacets(tc, ROUND);
        Assert.assertEquals(4, data.get(ROI_FULL).size());
        
        tc = prepareTask(ROI_FULL, 13, FacetGeneratorMethod.CLASSIC, 0);
        data = FacetGenerator.generateFacets(tc, ROUND);
        Assert.assertEquals(4, data.get(ROI_FULL).size());
        
        tc = prepareTask(ROI_CENTER, 10, FacetGeneratorMethod.CLASSIC, 0);
        data = FacetGenerator.generateFacets(tc, ROUND);
        Assert.assertEquals(1, data.get(ROI_CENTER).size());
        
        tc = prepareTask(ROI_CENTER, 3, FacetGeneratorMethod.CLASSIC, 0);
        data = FacetGenerator.generateFacets(tc, ROUND);
        Assert.assertEquals(9, data.get(ROI_CENTER).size());
        
        tc = prepareTask(ROI_CENTER, 3, FacetGeneratorMethod.CLASSIC, 1);
        data = FacetGenerator.generateFacets(tc, ROUND);
        Assert.assertEquals(25, data.get(ROI_CENTER).size());
    }
    
    private TaskContainer prepareTask(final ROI roi, final int facetSize, FacetGeneratorMethod mode, final int spacing) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());

        TaskContainer tc = new TaskContainer(input);
        InputLoader.loadInput(tc);        

        tc.addRoi(ROUND, roi);        

        tc.setParameter(TaskParameter.FACET_SIZE, facetSize);
        tc.setParameter(TaskParameter.FACET_GENERATOR_METHOD, mode);
        tc.setParameter(TaskParameter.FACET_GENERATOR_PARAM, spacing);        

        return tc;
    }
    
    @Test
    public void testTight() throws IOException, URISyntaxException, ComputationException {
        TaskContainer tc = prepareTask(ROI_FULL, 30, FacetGeneratorMethod.TIGHT, 1);
        Map<ROI, List<Facet>> data = FacetGenerator.generateFacets(tc, ROUND);
        Assert.assertEquals(1, data.get(ROI_FULL).size());
        
        tc = prepareTask(ROI_FULL, 29, FacetGeneratorMethod.TIGHT, 1);
        data = FacetGenerator.generateFacets(tc, ROUND);
        Assert.assertEquals(4, data.get(ROI_FULL).size());
        
        tc = prepareTask(ROI_FULL, 20, FacetGeneratorMethod.TIGHT, 1);
        data = FacetGenerator.generateFacets(tc, ROUND);
        Assert.assertEquals(121, data.get(ROI_FULL).size());
        
        tc = prepareTask(ROI_FULL, 20, FacetGeneratorMethod.TIGHT, 2);
        data = FacetGenerator.generateFacets(tc, ROUND);
        Assert.assertEquals(36, data.get(ROI_FULL).size());
        
        tc = prepareTask(ROI_CENTER, 20, FacetGeneratorMethod.TIGHT, 2);
        data = FacetGenerator.generateFacets(tc, ROUND);
        Assert.assertEquals(0, data.get(ROI_CENTER).size());
    }
}
