/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.gui.lang.Lang;

/**
 *
 * @author Petr Ječmen
 */
public class FpsManager {

    private final double tickLength;
    private final String tickUnit;

    public FpsManager(final TaskContainer tc) {
        final int fps = (int) tc.getParameter(TaskParameter.FPS);
        double length = 1 / (double) fps;
        length *= 1000;
        tickUnit = "ms";
        tickLength = length;

    }

    public double getTickLength() {
        return tickLength;
    }

    public double getTime(final int imageNr) {
        return tickLength * imageNr;
    }

    public String getTickUnit() {
        return tickUnit;
    }

    public String buildTimeDescription() {
        return Lang.getString("Time").concat(" [".concat(getTickUnit()).concat("]"));
    }

}
