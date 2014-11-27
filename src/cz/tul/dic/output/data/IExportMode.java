package cz.tul.dic.output.data;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.Direction;

/**
 *
 * @author Petr Jecmen
 * @param <T>
 */
public interface IExportMode<T> {

    T exportData(final TaskContainer tc, final Direction direction, final int... dataParams) throws ComputationException;    

}
