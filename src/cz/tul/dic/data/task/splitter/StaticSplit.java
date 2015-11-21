/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task.splitter;

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

    public StaticSplit(final FullTask task, final Object taskSplitValue) {
        super(task);

        if (taskSplitValue != null) {
            split = (int) taskSplitValue;
        } else {
            throw new IllegalArgumentException("Missing split task value for static splitting.");
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

        final List<AbstractSubset> sublistS = new ArrayList<>(split);
        final List<Integer> sublistW = new ArrayList<>(split);
        final int subsetCount = subsets.size();

        int count = 0;
        while (count < split && index < subsetCount) {
            sublistS.add(subsets.get(index));
            sublistW.add(subsetWeights.get(index));
            
            count++;
            index++;
        }

        checkIfHasNext();

        return new ComputationTask(image1, image2, sublistS, sublistW, deformationLimits);
    }
}
