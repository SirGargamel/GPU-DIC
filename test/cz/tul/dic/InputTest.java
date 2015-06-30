/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.data.task.TaskContainer;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Lenam s.r.o.
 */
public class InputTest {

    @Test
    public void testImageLoader() throws URISyntaxException, IOException, ComputationException {
        final List<File> inputs = new ArrayList<>(5);
        inputs.add(Paths.get(getClass().getResource("/resources/input/image.avi00000.bmp").toURI()).toFile());
        inputs.add(Paths.get(getClass().getResource("/resources/input/image.avi00001.bmp").toURI()).toFile());
        inputs.add(Paths.get(getClass().getResource("/resources/input/image.avi00002.bmp").toURI()).toFile());
        inputs.add(Paths.get(getClass().getResource("/resources/input/image.avi00003.bmp").toURI()).toFile());
        inputs.add(Paths.get(getClass().getResource("/resources/input/image.avi00004.bmp").toURI()).toFile());
        final TaskContainer task = TaskContainer.initTaskContainer(inputs);
        assertEquals(5, task.getImages().size());

    }

    @Test
    public void testVideoLoader() throws URISyntaxException, IOException, ComputationException {
        final TaskContainer task = TaskContainer.initTaskContainer(Paths.get(getClass().getResource("/resources/input/image.avi").toURI()).toFile());
        assertEquals(5, task.getImages().size());
    }

}
