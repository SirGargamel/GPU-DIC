package cz.tul.dic.data.task.splitter;

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

    public TaskSplitter(final TaskContainer tc, final int round) {
        this.tc = tc;
        this.round = round;
    }

    public static Iterator<ComputationTask> prepareSplitter(final TaskContainer tc, final int round) {
        final TaskSplit ts = (TaskSplit) tc.getParameter(TaskParameter.TASK_SPLIT_VARIANT);

        switch (ts) {
            case NONE:
                return new NoSplit(tc, round);
            case STATIC:
                return new StaticSplit(tc, round);
            case DYNAMIC_MEMORY:
            default:
                throw new IllegalArgumentException("Unsupported type of task splitting - " + ts);
        }
    }

}
