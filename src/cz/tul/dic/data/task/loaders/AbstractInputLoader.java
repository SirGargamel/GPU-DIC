/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task.loaders;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.task.TaskDefaultValues;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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

    public abstract TaskContainer loadTask(final Object in, final TaskContainer task) throws IOException, ComputationException;

    public abstract boolean canLoad(final Object in);

    protected void loadImages(final TaskContainer task, final List<File> inputs) throws ComputationException, IOException {
        final List<Image> images = new ArrayList<>(inputs.size());
        final Object in = task.getParameter(TaskParameter.IN);
        final File inputSource;
        if (in != null) {
            inputSource = (File) in;
        } else {
            inputSource = inputs.get(0);
            task.setParameter(TaskParameter.IN, inputSource);
        }
        for (File image : inputs) {
            if (!image.exists()) {
                image = new File(inputSource.getParent().concat(File.separator).concat(image.getName()));
                if (!image.exists()) {
                    throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Input file " + image.toString() + " not found.");
                }
            }

            images.add(Image.loadImageFromDisk(image));
        }
        task.setInput(inputs, images);
    }

    protected void loadUdaFile(final String inputName, final TaskContainer tc) throws IOException {
        final String udaFilename = inputName.substring(0, inputName.lastIndexOf('.')).concat(EXT_UDA);
        final File uda = new File(udaFilename);

        int fps = TaskDefaultValues.DEFAULT_FPS;
        if (udaFileExists(uda)) {
            final List<String> lines = Files.readAllLines(Paths.get(udaFilename));
            for (String s : lines) {
                if (s.contains(TEXT_SPEED)) {
                    final String val = s.replace(TEXT_SPEED, "").replace(TEXT_FPS, "").replaceAll(TEXT_EXTRA, "").trim();
                    try {
                        fps = Integer.parseInt(val);
                    } catch (NumberFormatException ex) {
                        Logger.warn("Failed to parse FPS value \"{0}\", using default FPS - ", val, fps);
                    }
                    break;
                }
            }
        } else {
            Logger.warn("Missing UDA file, using default FPS - {0}", fps);
        }
        tc.setParameter(TaskParameter.FPS, fps);
    }

    private static boolean udaFileExists(final File file) {
        return file.exists() && file.canRead();
    }

}
