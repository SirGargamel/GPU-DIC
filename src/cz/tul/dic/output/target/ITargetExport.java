package cz.tul.dic.output.target;

import cz.tul.dic.data.task.TaskContainer;
import java.io.IOException;

/**
 *
 * @author Petr Jecmen
 */
public interface ITargetExport {
    
    void exportData(Object data, Object targetParam, int[] dataParams, final TaskContainer tc) throws IOException;        
    
}
