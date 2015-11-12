/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.data.Interpolation;
import cz.tul.dic.data.config.Config;
import cz.tul.dic.data.config.ConfigType;
import cz.tul.dic.data.deformation.DeformationDegree;
import cz.tul.dic.data.subset.generator.SubsetGeneratorMethod;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.data.task.splitter.TaskSplitMethod;
import cz.tul.dic.engine.displacement.DisplacementCalculation;
import cz.tul.dic.engine.opencl.kernels.info.KernelInfo;
import cz.tul.dic.engine.opencl.solvers.Solver;
import cz.tul.dic.engine.strain.StrainEstimationMethod;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import junit.framework.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Lenam s.r.o.
 */
public class ConfigTest {

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testConfigPersistence() throws IOException {
        final File testFile = testFolder.newFile("test.config");

        final Config config = new Config();
        config.put("testValue1", "0");
        config.put("testValue2", "test");
        config.save(testFile);

        final Config loadedConfig = new Config().load(testFile);
        Assert.assertEquals("0", loadedConfig.get("testValue1"));
        Assert.assertEquals("test", loadedConfig.get("testValue2"));
    }

    @Test
    public void testConfigRealFiles() throws IOException, URISyntaxException, ComputationException {
        final Config configTask = new Config().load(Paths.get(getClass().getResource("/resources/config/task.config").toURI()).toFile());
        Assert.assertEquals(ConfigType.TASK, configTask.getType());

        final TaskContainer task = TaskContainer.initTaskContainer(Paths.get(getClass().getResource("/resources/config/task.config").toURI()).toFile());

        Assert.assertEquals(new File("D:/temp/.test spacing/6203652m/6203652m.avi.config__15_02_15.task"), task.getParameter(TaskParameter.IN));
        Assert.assertEquals(SubsetGeneratorMethod.EQUAL, task.getParameter(TaskParameter.SUBSET_GENERATOR_METHOD));
        Assert.assertEquals(2, (int) task.getParameter(TaskParameter.SUBSET_GENERATOR_PARAM));
        Assert.assertEquals(15, (int) task.getParameter(TaskParameter.SUBSET_SIZE));
        Assert.assertEquals(5000, (int) task.getParameter(TaskParameter.FPS));
        Assert.assertEquals(new KernelInfo(KernelInfo.Type.CL1D, KernelInfo.Input.BEST, KernelInfo.Correlation.BEST, KernelInfo.MemoryCoalescing.BEST), task.getParameter(TaskParameter.KERNEL));
        Assert.assertEquals(TaskSplitMethod.DYNAMIC, task.getParameter(TaskParameter.TASK_SPLIT_METHOD));
        Assert.assertEquals(1000, (int) task.getParameter(TaskParameter.TASK_SPLIT_PARAM));
        Assert.assertEquals(Interpolation.BICUBIC, task.getParameter(TaskParameter.INTERPOLATION));
        Assert.assertEquals(1.0, (double) task.getParameter(TaskParameter.MM_TO_PX_RATIO));
        Assert.assertTrue(Arrays.equals(new int[]{13, 19}, (int[]) task.getParameter(TaskParameter.ROUND_LIMITS)));
        Assert.assertEquals(DisplacementCalculation.MAX_WEIGHTED_AVERAGE, task.getParameter(TaskParameter.DISPLACEMENT_CALCULATION_METHOD));
        Assert.assertEquals(2000, (int) task.getParameter(TaskParameter.DISPLACEMENT_CALCULATION_PARAM));
        Assert.assertEquals(0.5, (double) task.getParameter(TaskParameter.RESULT_QUALITY));
        Assert.assertEquals(StrainEstimationMethod.LOCAL_LEAST_SQUARES, task.getParameter(TaskParameter.STRAIN_ESTIMATION_METHOD));
        Assert.assertEquals(20.0, (double) task.getParameter(TaskParameter.STRAIN_ESTIMATION_PARAM));
        Assert.assertTrue(Arrays.equals(new double[]{-1.5, 1.5, 0.5, -1.0, 1.0, 0.5, -0.25, 0.05, 0.05, -0.4, 0.25, 0.05, -0.25, 0.45, 0.05, -0.25, 0.5, 0.1}, (double[]) task.getParameter(TaskParameter.DEFORMATION_LIMITS)));
        Assert.assertEquals(DeformationDegree.ZERO, task.getParameter(TaskParameter.DEFORMATION_ORDER));
        Assert.assertEquals(Solver.BRUTE_FORCE, task.getParameter(TaskParameter.SOLVER));

        final Config configSequence = new Config().load(Paths.get(getClass().getResource("/resources/config/sequence.config").toURI()).toFile());
        Assert.assertEquals(325, configSequence.keySet().size());

    }
}
