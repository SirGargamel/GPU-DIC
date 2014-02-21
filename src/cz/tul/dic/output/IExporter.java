package cz.tul.dic.output;

import cz.tul.dic.data.task.TaskContainer;
import java.io.IOException;

/**
 *
 * @author Petr Jecmen
 */
public interface IExporter {

    public void exportResult(final ExportTask task, final TaskContainer tc) throws IOException;
    
    public ExportTarget getTarget();
    
    public ExportMode getMode();

}
