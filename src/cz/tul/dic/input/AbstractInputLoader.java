/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.input;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.task.TaskDefaultValues;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public abstract class AbstractInputLoader {

    private static final String EXT_UDA = ".uda";
    private static final String TEXT_SPEED = "Speed";
    private static final String TEXT_FPS = "fps";
    private static final String TEXT_EXTRA = "[=]";

    public abstract List<Image> loadData(final Object in, final TaskContainer tc) throws IOException, ComputationException;

    public abstract Class getSupporteType();

    void loadUdaFile(final String inputName, final TaskContainer tc) throws IOException {
        final String udaFilename = inputName.substring(0, inputName.lastIndexOf('.')).concat(EXT_UDA);
        final File uda = new File(udaFilename);

        int fps = TaskDefaultValues.DEFAULT_FPS;
        if (!uda.exists() || !uda.canRead()) {
            Logger.warn("Missing UDA file, using default FPS - {0}", fps);
        } else {
            final List<String> lines = Files.readAllLines(Paths.get(udaFilename));
            for (String s : lines) {
                if (s.contains(TEXT_SPEED)) {
                    String val = s.replace(TEXT_SPEED, "").replace(TEXT_FPS, "").replaceAll(TEXT_EXTRA, "").trim();
                    try {
                        fps = Integer.parseInt(val);
                    } catch (NumberFormatException ex) {
                        Logger.warn("Failed to parse FPS value \"{0}\", using default FPS - ", val, fps);
                    }
                    break;
                }
            }
        }
        tc.setParameter(TaskParameter.FPS, fps);
    }

}
