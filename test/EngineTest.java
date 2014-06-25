
import cz.tul.dic.ComputationException;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.roi.RectangleROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.Engine;
import cz.tul.dic.engine.EngineUtils;
import cz.tul.dic.input.InputLoader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Petr Jecmen
 */
public class EngineTest {

    private static final int ROUND = 0;        

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
    
    @Test
    public void testShifts() throws URISyntaxException, IOException, ComputationException {
        TaskContainer tc = prepareAndComputeTask("out_0_0");
        checkResults(tc, 0, 0);

        tc = prepareAndComputeTask("out_5_0");
        checkResults(tc, 5, 0);
        
        tc = prepareAndComputeTask("out_0_-5");
        checkResults(tc, 0, -5);
        
        tc = prepareAndComputeTask("out_-5_5");
        checkResults(tc, -5, 5);
    }

    private TaskContainer prepareAndComputeTask(final String outFilename) throws IOException, URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/" + outFilename + ".bmp").toURI()).toFile());

        TaskContainer tc = new TaskContainer(input);
        InputLoader.loadInput(tc);

        ROI roi = new RectangleROI(10, 10, 20, 20);

        tc.addRoi(ROUND, roi);        
        tc.setDeformationLimits(ROUND, roi, new double[]{-6, 6, 1, -6, 6, 1});
        
        tc.setParameter(TaskParameter.FACET_SIZE, 10);        

        EngineUtils.getInstance().computeTask(tc);

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
