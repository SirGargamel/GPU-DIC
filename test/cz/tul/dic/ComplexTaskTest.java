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
import cz.tul.dic.input.InputLoader;
import java.io.File;
import java.io.IOException;
import static java.lang.System.in;
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
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        final TaskContainer tc = new TaskContainer(input);

        InputLoader.loadInput(tc);

        tc.setParameter(TaskParameter.ROUND_LIMITS, new int[]{0, 1});
        tc.setParameter(TaskParameter.FACET_SIZE, 2);

        final ComplexTaskSolver cts = new ComplexTaskSolver();
        assert (!cts.isValidComplexTask(tc));

        final CircularROI cRoi1 = new CircularROI(9, 9, 1);
        final CircularROI cRoi2 = new CircularROI(21, 9, 1);
        final CircularROI cRoi3 = new CircularROI(9, 21, 1);
        final CircularROI cRoi4 = new CircularROI(21, 21, 1);
        tc.addRoi(BASE_ROUND, cRoi1);
        tc.addRoi(BASE_ROUND, cRoi2);
        tc.addRoi(BASE_ROUND, cRoi3);
        tc.addRoi(BASE_ROUND, cRoi4);

        assert (cts.isValidComplexTask(tc));

        cts.solveComplexTask(tc);
        
        Assert.assertTrue("Illegal count of ROIs.", tc.getRois(BASE_ROUND).size() == 4);        
        for (ROI roi : tc.getRois(BASE_ROUND)) {
            if (!(roi instanceof CircularROI)) {
                Assert.fail("Noncircular ROI found");
            }
        }
        
        final double[][][] result = tc.getDisplacement(BASE_ROUND);
        for (int x = 10; x <= 20; x++) {
            for (int y = 8; y <= 22; y++) {
                Assert.assertNotNull("NULL result at [" + x + "; " + y + "]", result[x][y]);
            }
        }
    }
}
