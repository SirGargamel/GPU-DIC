/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic.data.task.splitter;

import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskContainer;

public class NoSplit extends TaskSplitter {

    private boolean hasNext;

    public NoSplit(TaskContainer tc, int round, final ROI roi) {
        super(tc, round, roi);

        hasNext = true;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public ComputationTask next() {        
        hasNext = false;
        return new ComputationTask(tc.getImage(round), tc.getImage(round + 1), tc.getFacets(round, roi), tc.getDeformations(round, roi));
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported.");
    }

}
