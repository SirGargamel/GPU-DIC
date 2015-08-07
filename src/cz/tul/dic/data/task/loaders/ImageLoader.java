/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task.loaders;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.TaskContainer;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

public class ImageLoader extends AbstractInputLoader {

    @Override
    public TaskContainer loadTask(final Object in, final TaskContainer task) throws ComputationException {
        if (!(in instanceof List<?>)) {
            throw new IllegalArgumentException("ImageLoader needs a list of files as input.");
        }

        @SuppressWarnings("unchecked")
        final List<File> data = (List<File>) in;
        if (data.isEmpty()) {
            throw new IllegalArgumentException("No images.");
        }
        
        loadImages(task, data);
        loadUdaFile(data.get(0).getAbsolutePath(), task);

        return task;
    }

    @Override
    public boolean canLoad(final Object in) {
        boolean result = true;
        if (in instanceof List<?>) {
            @SuppressWarnings("unchecked")
            final List<File> data = (List<File>) in;
            final List<String> supportFormats = Arrays.asList(ImageIO.getReaderFileSuffixes());
            String ext;
            for (File f : data) {
                ext = f.getName().substring(f.getName().lastIndexOf('.') + 1).toLowerCase(Locale.getDefault());
                if (!supportFormats.contains(ext)) {
                    result = false;
                    break;
                }
            }
        } else {
            result = false;
        }
        return result;
    }

}
