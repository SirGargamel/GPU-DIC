package cz.tul.dic.data.task.splitter;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.task.ComputationTask;
import java.util.ArrayList;
import java.util.List;

public class StaticSplit extends TaskSplitter {

    private final int split;
    private boolean hasNext;
    private int index;

    public StaticSplit(Image image1, Image image2, final List<Facet> facets, final double[] deformationLimits, final Object taskSplitValue) throws ComputationException {
        super(image1, image2, facets, deformationLimits);

        if (taskSplitValue != null) {
            split = (int) taskSplitValue;
        } else {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Missing split task value for static splitting.");
        }

        checkIfHasNext();
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    private void checkIfHasNext() {
        hasNext = index < facets.size();
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

        return new ComputationTask(image1, image2, sublist, deformationLimits, false);
    }
    
    @Override
    public void signalTaskSizeTooBig() {
        hasNext = false;
    }

    @Override
    public boolean isSplitterReady() {
        return hasNext;
    }
    
    @Override
    public void resetTaskSize() {        
    }
}
