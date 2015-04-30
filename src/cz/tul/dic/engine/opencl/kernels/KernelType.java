/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernels;

/**
 *
 * @author Petr Jecmen
 */
public enum KernelType {

    CL1D_I_V_LL_D(true),
    CL1D_I_V_LL_MC_D(true),
    CL2D_Int_D(false),
    CL15D_pF_D(false),;

    private final boolean safeToUse;

    private KernelType(final boolean safeToUse) {
        this.safeToUse = safeToUse;
    }

    public boolean isSafeToUse() {
        return safeToUse;
    }

}
