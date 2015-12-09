/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task.splitter;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.subset.AbstractSubset;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.deformation.DeformationOrder;
import cz.tul.dic.data.task.ComputationTask;
import java.util.List;

/**
 *
 * @author Petr Ječmen
 */
public abstract class AbstractTaskSplitter {

    protected final Image image1, image2;
    protected final List<AbstractSubset> subsets;
    protected final List<Integer> subsetWeights;
    protected final List<double[]> deformations;
    protected final DeformationOrder order;
    protected final boolean usesLimits;

    public AbstractTaskSplitter(final ComputationTask task) {
        this.image1 = task.getImageA();
        this.image2 = task.getImageB();
        this.subsets = task.getSubsets();
        this.subsetWeights = task.getSubsetWeights();
        this.deformations = task.getDeformations();
        this.order = task.getOrder();
        this.usesLimits = task.usesLimits();
    }

    public static AbstractTaskSplitter prepareSplitter(final ComputationTask task, final TaskSplitMethod ts, final Object taskSplitValue) {
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
    
    public abstract boolean hasNext();
    
    public abstract ComputationTask next() throws ComputationException;

}
