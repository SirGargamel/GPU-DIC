package cz.tul.dic.data.task.splitter;

import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.ComputationTask;
import java.util.ArrayList;
import java.util.List;

public class StaticSplit extends TaskSplitter {

    private static final int SPLIT_DEFAULT = 50;
    private final int split;
    private boolean hasNext;
    private int index;

    public StaticSplit(Image image1, Image image2, final List<Facet> facets, final double[] deformations, final ROI roi, final Object taskSplitValue) {
        super(image1, image2, facets, deformations, roi);
        
        split = taskSplitValue == null ? SPLIT_DEFAULT : (int) taskSplitValue;

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

        return new ComputationTask(image1, image2, sublist, deformations);
    }    
}
