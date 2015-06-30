/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task.loaders;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.task.TaskContainer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Petr Jecmen
 */
public final class InputLoader {

    private static final List<AbstractInputLoader> loaders;

    static {
        loaders = new ArrayList<>(4);
        loaders.add(new ImageLoader());
        loaders.add(new VideoLoader());
        loaders.add(new ConfigLoader());
        loaders.add(new BinaryLoader());
    }

    private InputLoader() {
    }

    public static TaskContainer loadInput(final Object in) throws IOException, ComputationException {
        AbstractInputLoader loader = null;
        for (AbstractInputLoader ail : loaders) {
            if (ail.canLoad(in)) {
                loader = ail;
                break;
            }
        }

        TaskContainer result = null;
        if (loader != null) {
            try {
                result = loader.loadTask(in);
            } catch (ComputationException ex) {
                if (ex.getExceptionCause().equals(ComputationExceptionCause.ILLEGAL_TASK_DATA)) {
                    if (result != null && result.getImages().isEmpty()) {
                        throw ex;
                    }
                } else {
                    throw ex;
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported type of input data - " + in.getClass().toString() + " - " + in.toString());
        }

        return result;
    }

}
