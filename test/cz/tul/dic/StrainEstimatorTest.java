/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

    public StrainEstimatorTest() {
    }

    @Test
    public void testEstimator() throws URISyntaxException, ComputationException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/in.bmp").toURI()).toFile());
        final TaskContainer tc = new TaskContainer(input);

        tc.setParameter(TaskParameter.STRAIN_ESTIMATION_METHOD, StrainEstimationMethod.LOCAL_LEAST_SQUARES);
        tc.setParameter(TaskParameter.STRAIN_ESTIMATION_PARAM, 5.0);
        tc.setParameter(TaskParameter.MM_TO_PX_RATIO, 1.0);

        final Image img = tc.getImage(ROUND);
        final double[][][] displacement = new double[img.getWidth()][img.getHeight()][2];
        tc.setDisplacement(ROUND, displacement);

        new StrainEstimation().computeStrain(tc, ROUND);

        final double[][][] strains = tc.getStrain(ROUND);

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                Assert.assertNotNull(strains[x][y]);
                Assert.assertEquals(0.0, Math.abs(strains[x][y][StrainResult.Exx]));
                Assert.assertEquals(0.0, Math.abs(strains[x][y][StrainResult.Eyy]));
                Assert.assertEquals(0.0, Math.abs(strains[x][y][StrainResult.Exy]));
            }
        }
    }
}
