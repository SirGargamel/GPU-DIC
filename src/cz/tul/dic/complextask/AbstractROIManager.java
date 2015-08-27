/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.complextask;

import cz.tul.dic.data.task.TaskContainer;

/**
 *
 * @author Petr Jecmen
 */
public abstract class AbstractROIManager {
    
    protected static final double PRECISION = 0.5;
    protected final TaskContainer task;
    protected double[] defLimits;

    public AbstractROIManager(final TaskContainer tc) {
        this.task = tc;
    }        
    
    public abstract void generateNextRound(final int round, final int nextRound);

    public TaskContainer getTc() {
        return task;
    }
    
    protected static boolean haveMoved(final double shift0, final double shift1) {
        return Math.abs(shift0) > PRECISION || Math.abs(shift1) > PRECISION;
    }
    
}
