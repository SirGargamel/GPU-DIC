package cz.tul.dic.data.task.splitter;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.ComputationTask;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Petr Ječmen
 */
public abstract class TaskSplitter implements Iterator<ComputationTask> {

    protected final Image image1, image2;
    protected final List<Facet> facets;
    protected final double[] deformationLimits;

    public TaskSplitter(final Image image1, Image image2, final List<Facet> facets, final double[] deformationLimits) {
        this.image1 = image1;
        this.image2 = image2;
        this.facets = facets;
        this.deformationLimits = deformationLimits;        
    }

    public static TaskSplitter prepareSplitter(Image image1, Image image2, final List<Facet> facets, final double[] deformationLimits, final ROI roi, final TaskSplitMethod ts, final Object taskSplitValue) throws ComputationException {
        switch (ts) {
            case NONE:
                return new NoSplit(image1, image2, facets, deformationLimits);
            case STATIC:
                return new StaticSplit(image1, image2, facets, deformationLimits, taskSplitValue);
            case DYNAMIC:
                return new OpenCLSplitter(image1, image2, facets, deformationLimits);
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported type of task splitting - " + ts);
        }
    }
    
    public abstract void signalTaskSizeTooBig();
    
    public abstract void resetTaskSize();
    
    public abstract boolean isSplitterReady();

}
