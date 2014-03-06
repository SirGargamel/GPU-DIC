package cz.tul.dic.data.task.splitter;

import cz.tul.dic.data.task.ComputationTask;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.LoggingLevel;

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
            // print statistics          
            if (Logger.getLoggingLevel().equals(LoggingLevel.TRACE)) {
                int lastValue = -1, value;
                int counter;
                final StringBuilder sb = new StringBuilder();
                sb.append("Splitting statistics - ");
                for (Entry<Integer, List<ComputationTask>> e : tc.getTasks().entrySet()) {
                    counter = 0;                    
                    sb.append(e.getKey());
                    sb.append(":[");
                    for (ComputationTask ct : e.getValue()) {
                        value = ct.getFacets().size();
                        if (value != lastValue) {
                            if (counter > 0) {
                                sb.append(counter);
                                sb.append("x");
                                sb.append(lastValue);
                                sb.append(", ");
                            }
                            lastValue = value;
                            counter = 1;
                        } else {
                            counter++;
                        }
                    }
                    sb.append(counter);
                    sb.append("x");
                    sb.append(lastValue);
                    sb.append("]; ");                    
                }
                sb.setLength(sb.length()-2);
                Logger.trace(sb.toString());
            }
        } else {
            throw new IllegalArgumentException("Unsupported type of task splitting.");
        }
    }

    public abstract void split(final TaskContainer tc);

}
