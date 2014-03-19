package cz.tul.dic.output.data;

import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.Direction;

/**
 *
 * @author Petr Jecmen
 * @param <O>
 */
public interface IDataExport<O> {
    
    O exportData(final TaskContainer tc, final Direction direction, final int[] dataParams);
    
    O exportData(final TaskContainer tc, final Direction direction, final int[] dataParams, final ROI... rois);
    
}
