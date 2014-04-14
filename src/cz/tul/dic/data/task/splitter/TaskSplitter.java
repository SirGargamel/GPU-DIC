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
    protected final int round;
    protected final List<Facet> facets;
    protected final double[] deformations;

    public TaskSplitter(final TaskContainer tc, final int round, final List<Facet> facets, final double[] deformations) {
        this.tc = tc;
        this.round = round;
        this.facets = facets;
        this.deformations = deformations;
    }

    public static Iterator<ComputationTask> prepareSplitter(final TaskContainer tc, final int round, final List<Facet> facets, final double[] deformations) throws ComputationException {
        final TaskSplit ts = (TaskSplit) tc.getParameter(TaskParameter.TASK_SPLIT_VARIANT);

        switch (ts) {
            case NONE:
                return new NoSplit(tc, round, facets, deformations);
            case STATIC:
                return new StaticSplit(tc, round, facets, deformations);
            case DYNAMIC_MEMORY:
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported type of task splitting - " + ts);
        }
    }

}
