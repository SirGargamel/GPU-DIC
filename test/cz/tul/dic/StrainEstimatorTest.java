package cz.tul.dic;

import cz.tul.dic.data.Image;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.engine.strain.StrainEstimation;
import cz.tul.dic.engine.strain.StrainEstimationMethod;
import cz.tul.dic.engine.strain.StrainResult;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author Petr Ječmen
 */
public class StrainEstimatorTest {

    private static final int ROUND = 0;
    private static final double DELTA = 0.001;

    @Test
    public void testEstimator() throws URISyntaxException, ComputationException {
        testStatic("out_0_0.bmp", 0, 0);
        testStatic("out_5_0.bmp", 5, 0);
    }

    private void testStatic(final String fileOut, final double dX, final double dY) throws ComputationException, URISyntaxException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/" + fileOut).toURI()).toFile());
        final TaskContainer tc = new TaskContainer(input);

        tc.setParameter(TaskParameter.STRAIN_ESTIMATION_METHOD, StrainEstimationMethod.LOCAL_LEAST_SQUARES);
        tc.setParameter(TaskParameter.STRAIN_ESTIMATION_PARAM, 5.0);
        tc.setParameter(TaskParameter.MM_TO_PX_RATIO, 1.0);

        final Image img = tc.getImage(ROUND);
        final double[][][] displacement = new double[img.getWidth()][img.getHeight()][2];
        for (double[][] dAA : displacement) {
            for (double[] dA : dAA) {
                dA[0] = dX;
                dA[1] = dY;
            }
        }
        tc.setDisplacement(ROUND, ROUND + 1, displacement);

        new StrainEstimation().computeStrain(tc, ROUND, ROUND + 1);

        final double[][][] strains = tc.getStrain(ROUND, ROUND + 1);

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                Assert.assertNotNull(strains[x][y]);
                Assert.assertEquals("Exx", 0.0, strains[x][y][StrainResult.Exx], DELTA);
                Assert.assertEquals("Eyy", 0.0, strains[x][y][StrainResult.Eyy], DELTA);
                Assert.assertEquals("Exy", 0.0, strains[x][y][StrainResult.Exy], DELTA);
            }
        }
    }
}
