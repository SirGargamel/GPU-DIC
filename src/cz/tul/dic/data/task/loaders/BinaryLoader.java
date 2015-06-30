/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task.loaders;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.task.TaskContainer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Locale;

public class BinaryLoader extends AbstractInputLoader {

    private static final String SUPPORTED_TYPES = "task";

    @Override
    public TaskContainer loadTask(final Object in) throws IOException, ComputationException {
        if (!(in instanceof File)) {
            throw new IllegalArgumentException("ImageLoader needs a list of files as input.");
        }        

        final File input = (File) in;
        TaskContainer result;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(input))) {
            result = (TaskContainer) ois.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            throw new ComputationException(ComputationExceptionCause.IO, ex);
        }
        return result;
    }

    @Override
    public boolean canLoad(Object in) {
        boolean result = false;
        if (in instanceof File) {
            final File input = (File) in;
            final String ext = input.getName().substring(input.getName().lastIndexOf('.') + 1).toLowerCase(Locale.getDefault());
            result = SUPPORTED_TYPES.contains(ext);
        }
        return result;
    }

}
