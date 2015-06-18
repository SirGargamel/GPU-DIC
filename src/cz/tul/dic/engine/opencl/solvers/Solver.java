/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.solvers;

/**
 *
 * @author Petr Ječmen
 */
public enum Solver {

    BRUTE_FORCE("BruteForce"),
    COARSE_FINE("CoarseFine"),
    NEWTON_RHAPSON("NewtonRaphson"),
    NEWTON_RHAPSON_FORWARD("NewtonRaphsonForward"),
    ;
    
    private final String className;

    private Solver(final String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

}
