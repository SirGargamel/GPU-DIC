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

    BRUTE_FORCE("BruteForce", "BF", true, false),
    COARSE_FINE("CoarseFine", "CF", false, false),
    NEWTON_RHAPSON_CENTRAL("NewtonRaphsonCentral", "NRC", true, false),
    NEWTON_RHAPSON_CENTRAL_HE("NewtonRaphsonCentralHE", "NRCH", true, false),
    NEWTON_RHAPSON_FORWARD("NewtonRaphsonForward", "NRF", true, false),    
    NEWTON_RHAPSON_FORWARD_HE("NewtonRaphsonForwardHE", "NRFH", true, false),
    SPGD("SPGD", "SPGD", true, true),
    ;
    
    private final String className, abbreviation;
    private final boolean higherOrderDeformation, weighedCorrelation;

    private Solver(String className, String abbreviation, boolean higherOrderDeformation, boolean weighedCorrelation) {
        this.className = className;
        this.abbreviation = abbreviation;
        this.higherOrderDeformation = higherOrderDeformation;
        this.weighedCorrelation = weighedCorrelation;
    }    

    public String getClassName() {
        return className;
    }
    
    public String getAbbreviation() {
        return abbreviation;
    }

    public boolean supportsHigherOrderDeformation() {
        return higherOrderDeformation;
    }

    public boolean supportsWeighedCorrelation() {
        return weighedCorrelation;
    }

}
