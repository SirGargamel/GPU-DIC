package cz.tul.dic.data.task.splitter;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.data.task.TaskParameter;
import java.util.ArrayList;
import java.util.List;

public class StaticSplit extends TaskSplitter {

    private static final int SPLIT_DEFAULT = 10;

    @Override
    public void split(TaskContainer tc) {
        Object split = tc.getParameter(TaskParameter.TASK_SPLIT_VALUE);
        final int splitCount = split == null ? SPLIT_DEFAULT : (int) split;
        final int roundCount = TaskContainerUtils.getRoundCount(tc);

        int facetCount, facetBase, facetSubtaskCount;
        ComputationTask ct;
        List<Facet> sublist, facets;
        for (int round = 0; round < roundCount; round++) {
            facets = tc.getFacets(round);
            facetCount = facets.size();
            facetSubtaskCount = facetCount / splitCount;

            facetBase = 0;
            for (int i = 0; i < splitCount - 1; i++) {
                sublist = new ArrayList<>();
                for (int f = facetBase; f < facetBase + facetSubtaskCount; f++) {
                    sublist.add(facets.get(f));
                }
                ct = new ComputationTask(tc.getImage(round), tc.getImage(round + 1), sublist, tc.getDeformations());
                tc.addTask(ct, round);

                facetBase += facetSubtaskCount;
            }

            sublist = new ArrayList<>();
            for (int f = facetBase; f < facetCount; f++) {
                sublist.add(facets.get(f));
            }
            ct = new ComputationTask(tc.getImage(round), tc.getImage(round + 1), sublist, tc.getDeformations());
            tc.addTask(ct, round);
        }
    }
}
