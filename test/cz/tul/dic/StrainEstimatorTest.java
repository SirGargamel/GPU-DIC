/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.data.Image;
import cz.tul.dic.data.result.DisplacementResult;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.result.Result;
import cz.tul.dic.engine.strain.StrainEstimator;
import cz.tul.dic.engine.strain.StrainEstimationMethod;
import cz.tul.dic.engine.strain.StrainResultDirection;
import java.io.File;
import java.io.IOException;
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
    public void testEstimatorNull() throws URISyntaxException, ComputationException, IOException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());
        final TaskContainer tc = TaskContainer.initTaskContainer(input);

        final StrainEstimationMethod method = StrainEstimationMethod.LOCAL_LEAST_SQUARES;
        tc.setParameter(TaskParameter.STRAIN_ESTIMATION_METHOD, method);
        tc.setParameter(TaskParameter.STRAIN_ESTIMATION_PARAM, 1.0);
        tc.setParameter(TaskParameter.MM_TO_PX_RATIO, 1.0);
        tc.setParameter(TaskParameter.RESULT_QUALITY, 0.5);

        // indexing is [x][y], but init is [y][x]
        final double[][][] displacement = new double[][][]{
            {null, null, null, null, null},
            {null, {0, 0}, null, null, null},
            {{0, 0}, null, {2, 0.5}, {2, 0.0}, null},
            {null, null, {0, 0}, null, null},
            {null, null, null, null, null},};
        tc.setResult(ROUND, ROUND + 1, new Result(new DisplacementResult(displacement, null)));

        StrainEstimator.initStrainEstimator(method).estimateStrain(tc, ROUND, ROUND + 1);
        final double[][][] strains = tc.getResult(ROUND, ROUND + 1).getStrainResult().getStrain();

        Assert.assertNotNull(strains[2][2]);

        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                if (x == 2 && y == 2) {
                    continue;
                }
                Assert.assertNull(strains[x][y]);
            }
        }
    }

    @Test
    public void testEstimatorZeroStrain() throws URISyntaxException, ComputationException, IOException {
        testStatic("out_0_0.bmp", 0, 0);
        testStatic("out_5_0.bmp", 5, 0);
    }

    private void testStatic(final String fileOut, final double dX, final double dY) throws ComputationException, URISyntaxException, IOException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/engine/" + fileOut).toURI()).toFile());
        final TaskContainer tc = TaskContainer.initTaskContainer(input);

        final StrainEstimationMethod method = StrainEstimationMethod.LOCAL_LEAST_SQUARES;
        tc.setParameter(TaskParameter.STRAIN_ESTIMATION_METHOD, method);
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
        tc.setResult(ROUND, ROUND + 1, new Result(new DisplacementResult(displacement, null)));

        StrainEstimator.initStrainEstimator(method).estimateStrain(tc, ROUND, ROUND + 1);

        final double[][][] strains = tc.getResult(ROUND, ROUND + 1).getStrainResult().getStrain();

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                Assert.assertNotNull(strains[x][y]);
                Assert.assertEquals("Exx", 0.0, strains[x][y][StrainResultDirection.E_XX], DELTA);
                Assert.assertEquals("Eyy", 0.0, strains[x][y][StrainResultDirection.E_YY], DELTA);
                Assert.assertEquals("Exy", 0.0, strains[x][y][StrainResultDirection.E_XY], DELTA);
            }
        }
    }

    @Test
    public void testEstimatorNonZeroXxYy() throws URISyntaxException, ComputationException, IOException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());
        final TaskContainer tc = TaskContainer.initTaskContainer(input);

        final StrainEstimationMethod method = StrainEstimationMethod.LOCAL_LEAST_SQUARES;
        tc.setParameter(TaskParameter.STRAIN_ESTIMATION_METHOD, method);
        tc.setParameter(TaskParameter.STRAIN_ESTIMATION_PARAM, 3.0);
        tc.setParameter(TaskParameter.MM_TO_PX_RATIO, 1.0);

        // indexing is [x][y], but init is [y][x]
        final double[][][] displacement = new double[][][]{
            {{1, 1}, {1, 0.5}, {1, 0.0}},
            {{2, 1}, {2, 0.5}, {2, 0.0}},
            {{3, 1}, {3, 0.5}, {3, 0.0}}
        };
        tc.setResult(ROUND, ROUND + 1, new Result(new DisplacementResult(displacement, null)));

        StrainEstimator.initStrainEstimator(method).estimateStrain(tc, ROUND, ROUND + 1);
        final double[][][] strains = tc.getResult(ROUND, ROUND + 1).getStrainResult().getStrain();

        for (double[][] strain : strains) {
            for (double[] strain1 : strain) {
                Assert.assertNotNull(strain1);
                Assert.assertEquals("Exx", 100.0, strain1[StrainResultDirection.E_XX], DELTA);
                Assert.assertEquals("Eyy", -50.0, strain1[StrainResultDirection.E_YY], DELTA);
                Assert.assertEquals("Exy", 0.0, strain1[StrainResultDirection.E_XY], DELTA);
            }
        }
    }

    @Test
    public void testEstimatorNonZeroXy() throws URISyntaxException, ComputationException, IOException {
        final List<File> input = new ArrayList<>(2);
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());
        input.add(Paths.get(getClass().getResource("/resources/engine/in.bmp").toURI()).toFile());
        final TaskContainer tc = TaskContainer.initTaskContainer(input);

        final StrainEstimationMethod method = StrainEstimationMethod.LOCAL_LEAST_SQUARES;
        tc.setParameter(TaskParameter.STRAIN_ESTIMATION_METHOD, method);
        tc.setParameter(TaskParameter.STRAIN_ESTIMATION_PARAM, 3.0);
        tc.setParameter(TaskParameter.MM_TO_PX_RATIO, 1.0);

        // indexing is [x][y], but init is [y][x]
        final double[][][] displacement = new double[][][]{
            {{1, 1}, {0, 1}, {-1, 1}},
            {{1, 0.5}, {0, 0.5}, {-1, 0.5}},
            {{1, 0.0}, {0, 0.0}, {-1, 0.0}}
        };
        tc.setResult(ROUND, ROUND + 1, new Result(new DisplacementResult(displacement, null)));

        StrainEstimator.initStrainEstimator(method).estimateStrain(tc, ROUND, ROUND + 1);
        final double[][][] strains = tc.getResult(ROUND, ROUND + 1).getStrainResult().getStrain();

        for (double[][] strain : strains) {
            for (double[] strain1 : strain) {
                Assert.assertNotNull(strain1);
                Assert.assertEquals("Exx", 0.0, strain1[StrainResultDirection.E_XX], DELTA);
                Assert.assertEquals("Eyy", 0.0, strain1[StrainResultDirection.E_YY], DELTA);
                Assert.assertEquals("Exy", -75.0, strain1[StrainResultDirection.E_XY], DELTA);
            }
        }
    }
}
