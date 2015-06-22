/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task.splitter;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.FullTask;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class StaticSplit extends AbstractTaskSplitter {

    private final int split;
    private boolean hasNextElement;
    private int index;

    public StaticSplit(final FullTask task, final Object taskSplitValue) throws ComputationException {
        super(task);

        if (taskSplitValue != null) {
            split = (int) taskSplitValue;
        } else {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Missing split task value for static splitting.");
        }

        checkIfHasNext();
    }

    @Override
    public boolean hasNext() {
        return hasNextElement;
    }

    private void checkIfHasNext() {
        hasNextElement = index < subsets.size();
    }

    @Override
    public ComputationTask next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        final List<AbstractSubset> sublist = new ArrayList<>(split);
        final int subsetCount = subsets.size();

        int count = 0;
        while (count < split && index < subsetCount) {
            sublist.add(subsets.get(index));

            count++;
            index++;
        }

        checkIfHasNext();

        return new ComputationTask(image1, image2, sublist, deformationLimits, false);
    }

    @Override
    public void signalTaskSizeTooBig() {
        hasNextElement = false;
    }

    @Override
    public boolean isSplitterReady() {
        return hasNextElement;
    }
}
