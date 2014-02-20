package cz.tul.dic.output;

import cz.tul.dic.data.task.TaskContainer;

/**
 *
 * @author Petr Jecmen
 */
public interface IExporter {
    
    public void exportResult(final ExportTask task, final TaskContainer tc);
    
}
