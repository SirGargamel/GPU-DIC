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

    BRUTE_FORCE("BruteForce", "BF"),
    COARSE_FINE("CoarseFine", "CF"),
    NEWTON_RHAPSON_CENTRAL("NewtonRaphsonCentral", "NRC"),
    NEWTON_RHAPSON_CENTRAL_HE("NewtonRaphsonCentralHE", "NRCH"),
    NEWTON_RHAPSON_FORWARD("NewtonRaphsonForward", "NRF"),    
    NEWTON_RHAPSON_FORWARD_HE("NewtonRaphsonForwardHE", "NRFH"),
    ;
    
    private final String className, abbreviation;

    private Solver(final String className, final String abbreviation) {
        this.className = className;
        this.abbreviation = abbreviation;
    }

    public String getClassName() {
        return className;
    }
    
    public String getAbbreviation() {
        return abbreviation;
    }

}
