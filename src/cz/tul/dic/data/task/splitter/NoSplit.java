/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.tul.dic.data.task.splitter;

import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;


public class NoSplit extends TaskSplitter {

    @Override
    public void split(TaskContainer tc) {
        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        for (int round = 0; round < roundCount; round++) {
            tc.addTask(new ComputationTask(tc.getImage(round), tc.getImage(round + 1), tc.getFacets(round), tc.getDeformations()), round);
        }
    }
    
}
