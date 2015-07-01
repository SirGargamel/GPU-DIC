/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task.loaders;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.TaskContainer;
import java.io.IOException;
import java.util.ArrayList;

public class TaskContainerLoader extends AbstractInputLoader {

    @Override
    public TaskContainer loadTask(final Object in, final TaskContainer task) throws IOException, ComputationException {
        if (!(in instanceof TaskContainer)) {
            throw new IllegalArgumentException("TaskContainerLoader needs a TaskContainer as input.");
        }        
        
        loadImages(task, new ArrayList<>(task.getInput()));
        loadUdaFile(task.getInput().get(0).getAbsolutePath(), task);

        return task;
    }

    @Override
    public boolean canLoad(final Object in) {        
        return (in instanceof TaskContainer);
    }

}
