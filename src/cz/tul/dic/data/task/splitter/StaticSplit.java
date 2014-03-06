package cz.tul.dic.data.task.splitter;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import java.util.ArrayList;
import java.util.List;

public class StaticSplit extends TaskSplitter {

    private static final int SPLIT_DEFAULT = 50;

    @Override
    public void split(TaskContainer tc) {
        final Object split = tc.getParameter(TaskParameter.TASK_SPLIT_VALUE);
        final int splitCount = split == null ? SPLIT_DEFAULT : (int) split;
        final int roundCount = TaskContainerUtils.getRoundCount(tc);

        int facetCount, index;
        ComputationTask ct;
        List<Facet> sublist, facets;
        for (int round = 0; round < roundCount; round++) {
            facets = tc.getFacets(round);
            facetCount = facets.size();

            index = 0;
            sublist = new ArrayList<>();
            while (index < facetCount) {
                sublist.add(facets.get(index));
                index++;

                if (index % splitCount == 0) {
                    ct = new ComputationTask(tc.getImage(round), tc.getImage(round + 1), sublist, tc.getDeformations());
                    tc.addTask(ct, round);
                    sublist = new ArrayList<>();
                }
            }
            ct = new ComputationTask(tc.getImage(round), tc.getImage(round + 1), sublist, tc.getDeformations());
            tc.addTask(ct, round);
        }
    }
}
