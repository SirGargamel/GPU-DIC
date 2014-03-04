package cz.tul.dic.data.task.splitter;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.util.EnumMap;
import java.util.Map;

/**
 *
 * @author Petr Ječmen
 */
public abstract class TaskSplitter {

    private static final Map<TaskSplit, TaskSplitter> splitters;

    static {
        splitters = new EnumMap<>(TaskSplit.class);

        splitters.put(TaskSplit.NONE, new NoSplit());
        splitters.put(TaskSplit.STATIC, new StaticSplit());
    }

    public static void splitTask(final TaskContainer tc) {
        TaskSplit ts = (TaskSplit) tc.getParameter(TaskParameter.TASK_SPLIT_VARIANT);
        if (splitters.containsKey(ts)) {
            splitters.get(ts).split(tc);
        } else {
            throw new IllegalArgumentException("Unsupported type of task splitting.");
        }
    }

    public abstract void split(final TaskContainer tc);

}
