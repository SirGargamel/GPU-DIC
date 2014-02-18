package cz.tul.dic.output;

import cz.tul.dic.data.Result;

/**
 *
 * @author Petr Jecmen
 */
public interface IExporter {
    
    public void exportResult(final ExportTask task, final Result result);
    
}
