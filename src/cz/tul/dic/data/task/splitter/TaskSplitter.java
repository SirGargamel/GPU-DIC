package cz.tul.dic.data.task.splitter;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Facet;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Petr Ječmen
 */
public abstract class TaskSplitter implements Iterator<ComputationTask> {

    protected final TaskContainer tc;
    protected final int index1, index2;
    protected final List<Facet> facets;
    protected final double[] deformations;

    public TaskSplitter(final TaskContainer tc, final int index1, final int index2, final List<Facet> facets, final double[] deformations) {
        this.tc = tc;
        this.index1 = index1;
        this.index2 = index2;
        this.facets = facets;
        this.deformations = deformations;
    }

    public static Iterator<ComputationTask> prepareSplitter(final TaskContainer tc, final int index1, final int index2, final List<Facet> facets, final double[] deformations) throws ComputationException {
        final TaskSplit ts = (TaskSplit) tc.getParameter(TaskParameter.TASK_SPLIT_VARIANT);

        switch (ts) {
            case NONE:
                return new NoSplit(tc, index1, index2, facets, deformations);
            case STATIC:
                return new StaticSplit(tc, index1, index2, facets, deformations);
            case DYNAMIC_MEMORY:
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported type of task splitting - " + ts);
        }
    }

}
