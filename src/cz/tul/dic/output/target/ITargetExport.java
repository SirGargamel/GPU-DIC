package cz.tul.dic.output.target;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportMode;
import java.io.IOException;

/**
 *
 * @author Petr Jecmen
 */
public interface ITargetExport {

    void exportData(Object data, Direction direction, Object targetParam, int[] dataParams, final TaskContainer tc) throws IOException;
    
    boolean supportsMode(final ExportMode mode);

}
