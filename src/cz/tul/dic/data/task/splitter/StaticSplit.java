package cz.tul.dic.data.task.splitter;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.util.ArrayList;
import java.util.List;

public class StaticSplit extends TaskSplitter {

    private static final int SPLIT_DEFAULT = 50;
    private final int split;
    private boolean hasNext;
    private int index;

    public StaticSplit(final TaskContainer tc, final int index1, final int index2, final List<Facet> facets, final double[] deformations) {
        super(tc, index1, index2, facets, deformations);

        final Object o = this.tc.getParameter(TaskParameter.TASK_SPLIT_VALUE);
        split = o == null ? SPLIT_DEFAULT : (int) o;

        checkIfHasNext();
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    private void checkIfHasNext() {
        final int facetCount = facets.size();
        hasNext = index < facetCount - 1;
    }

    @Override
    public ComputationTask next() {
        final List<Facet> sublist = new ArrayList<>(split);
        final int facetCount = facets.size();

        int count = 0;
        while (count < split && index < facetCount) {
            sublist.add(facets.get(index));

            count++;
            index++;
        }

        checkIfHasNext();

        return new ComputationTask(tc.getImage(index1), tc.getImage(index2), sublist, deformations);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported.");
    }
}
