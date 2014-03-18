package cz.tul.dic.data.task.splitter;

import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.util.Iterator;

/**
 *
 * @author Petr Ječmen
 */
public abstract class TaskSplitter implements Iterator<ComputationTask> {

    protected final TaskContainer tc;
    protected final int round;
    protected final ROI roi;

    public TaskSplitter(final TaskContainer tc, final int round, final ROI roi) {
        this.tc = tc;
        this.round = round;
        this.roi = roi;
    }

    public static Iterator<ComputationTask> prepareSplitter(final TaskContainer tc, final int round, final ROI roi) {
        final TaskSplit ts = (TaskSplit) tc.getParameter(TaskParameter.TASK_SPLIT_VARIANT);

        switch (ts) {
            case NONE:
                return new NoSplit(tc, round, roi);
            case STATIC:
                return new StaticSplit(tc, round, roi);
            case DYNAMIC_MEMORY:
            default:
                throw new IllegalArgumentException("Unsupported type of task splitting - " + ts);
        }
    }

}
