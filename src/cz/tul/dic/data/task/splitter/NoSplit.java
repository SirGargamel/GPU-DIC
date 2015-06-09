/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task.splitter;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.task.ComputationTask;
import java.util.List;
import java.util.NoSuchElementException;

public class NoSplit extends AbstractTaskSplitter {

    private boolean hasNextElement;

    public NoSplit(final Image image1, final Image image2, final List<Facet> facets, final List<double[]> deformationLimits) {
        super(image1, image2, facets, deformationLimits);
        hasNextElement = true;
    }

    @Override
    public boolean hasNext() {
        return hasNextElement;
    }

    @Override
    public ComputationTask next() {
        if (hasNext()) {
            throw new NoSuchElementException();
        }
        hasNextElement = false;
        return new ComputationTask(image1, image2, facets, deformationLimits, false);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Not available since the task size is always maximal.
     */
    @Override
    public void signalTaskSizeTooBig() {
        throw new UnsupportedOperationException("No task division available.");
    }

    @Override
    public boolean isSplitterReady() {
        return hasNextElement;
    }

    /**
     * Not available since the task size is always maximal.
     */
    @Override
    public void resetTaskSize() {
        throw new UnsupportedOperationException("No task division available.");
    }

}
