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
import cz.tul.dic.engine.AbstractDeviceManager;
import java.util.List;

/**
 *
 * @author Petr Ječmen
 */
public abstract class AbstractTaskSplitter {

    protected Image image1, image2;
    protected List<AbstractSubset> subsets;
    protected List<Integer> subsetWeights;
    protected List<double[]> deformations;
    protected DeformationOrder order;
    protected boolean usesLimits;
    
    public void assignTask(final ComputationTask task, final Object taskSplitValue) {
        this.image1 = task.getImageA();
        this.image2 = task.getImageB();
        this.subsets = task.getSubsets();
        this.subsetWeights = task.getSubsetWeights();
        this.deformations = task.getDeformations();
        this.order = task.getOrder();
        this.usesLimits = task.usesLimits();
        
        prepareSplitter(taskSplitValue);
    }
    
    abstract void prepareSplitter(final Object taskSplitValue);     
    
    public abstract void assignDeviceManager(final AbstractDeviceManager deviceManager);
    
    public abstract boolean hasNext();
    
    public abstract ComputationTask next() throws ComputationException;

}
