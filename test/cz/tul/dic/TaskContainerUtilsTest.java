/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.input.InputLoader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Lenam s.r.o.
 */
public class TaskContainerUtilsTest {

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void testUtils() throws URISyntaxException, ComputationException, IOException {
        final List<File> inputs = new ArrayList<>(5);
        inputs.add(Paths.get(getClass().getResource("/resources/input/image.avi00000.bmp").toURI()).toFile());
        inputs.add(Paths.get(getClass().getResource("/resources/input/image.avi00001.bmp").toURI()).toFile());
        inputs.add(Paths.get(getClass().getResource("/resources/input/image.avi00002.bmp").toURI()).toFile());
        inputs.add(Paths.get(getClass().getResource("/resources/input/image.avi00003.bmp").toURI()).toFile());
        inputs.add(Paths.get(getClass().getResource("/resources/input/image.avi00004.bmp").toURI()).toFile());
        final TaskContainer task = new TaskContainer(inputs);
        InputLoader.loadInput(task);
        TaskContainerUtils.checkTaskValidity(task);
        assertEquals(0, (int) TaskContainerUtils.getFirstRound(task));
        assertEquals(4, (int) TaskContainerUtils.getMaxRoundCount(task));
        assertEquals(4, TaskContainerUtils.getRounds(task).size());

        final File testFile = testFolder.newFile();
        TaskContainerUtils.serializeTaskToBinary(task, testFile);
        final TaskContainer task2 = TaskContainerUtils.deserializeTaskFromBinary(testFile);
    }

}
