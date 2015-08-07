/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task.splitter;

import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.FullTask;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Petr Ječmen
 */
public abstract class AbstractTaskSplitter implements Iterator<ComputationTask> {

    protected final Image image1, image2;
    protected final List<AbstractSubset> subsets;
    protected final List<double[]> deformationLimits;

    public AbstractTaskSplitter(final FullTask task) {
        this.image1 = task.getImageA();
        this.image2 = task.getImageB();
        this.subsets = task.getSubsets();
        this.deformationLimits = task.getDeformationLimits();
    }

    public static AbstractTaskSplitter prepareSplitter(final FullTask task, final TaskSplitMethod ts, final Object taskSplitValue) {
        AbstractTaskSplitter result;
        switch (ts) {
            case NONE:
                result = new NoSplit(task);
                break;
            case STATIC:
                result = new StaticSplit(task, taskSplitValue);
                break;
            case DYNAMIC:
                result = new OpenCLSplitter(task);
                break;
            default:
                throw new IllegalArgumentException("Unsupported type of task splitting - " + ts);
        }
        return result;
    }

    public abstract void signalTaskSizeTooBig();

    public abstract boolean isSplitterReady();

}
