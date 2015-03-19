/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
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
