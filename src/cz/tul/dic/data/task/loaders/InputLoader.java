/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task.loaders;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.pj.journal.Journal;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Petr Jecmen
 */
public final class InputLoader {

    private static final List<AbstractInputLoader> LOADERS;

    static {
        LOADERS = new ArrayList<>(4);
        LOADERS.add(new ImageLoader());
        LOADERS.add(new VideoLoader());
        LOADERS.add(new ConfigLoader());
        LOADERS.add(new BinaryLoader());
        LOADERS.add(new TaskContainerLoader());
    }

    private InputLoader() {
    }

    public static TaskContainer loadInput(final Object in, TaskContainer task) throws ComputationException {
        AbstractInputLoader loader = null;
        for (AbstractInputLoader ail : LOADERS) {
            if (ail.canLoad(in)) {
                loader = ail;
                break;
            }
        }

        final TaskContainer innerTask;
        if (task == null) {
            innerTask = new TaskContainer();
        } else {
            innerTask = task;
        }

        TaskContainer result = null;
        if (loader != null) {
            result = loader.loadTask(in, innerTask);
        } else {
            throw new IllegalArgumentException("Unsupported type of input data - " + in.getClass().toString() + " - " + in.toString());
        }
        
        Journal.getInstance().addEntry("Input loaded", "Loaded file {0} using {1}.", in.toString(), loader.getClass().getSimpleName());

        return result;
    }

}
