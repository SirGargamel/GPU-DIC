/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernels.info;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author Petr Ječmen
 */
public class KernelInfo implements Serializable {

    private static final String SEPARATOR = ";";
    private final Type type;
    private final Input input;
    private final Correlation correlation;

    public KernelInfo(final Type type, final Input input, final Correlation correlation) {
        this.type = type;
        this.input = input;
        this.correlation = correlation;
    }

    public static KernelInfo fromConfig(final String configString) {
        final String[] vals = configString.split(SEPARATOR);
        return new KernelInfo(Type.valueOf(vals[0]), Input.valueOf(vals[1]), Correlation.valueOf(vals[2]));
    }

    public Type getType() {
        return type;
    }

    public Input getInput() {
        return input;
    }

    public Correlation getCorrelation() {
        return correlation;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + Objects.hashCode(this.type);
        hash = 31 * hash + Objects.hashCode(this.input);
        hash = 31 * hash + Objects.hashCode(this.correlation);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final KernelInfo other = (KernelInfo) obj;
        if (this.type != other.type) {
            return false;
        }
        if (this.input != other.input) {
            return false;
        }
        if (this.correlation != other.correlation) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return type + SEPARATOR + input + SEPARATOR + correlation;
    }

    public enum Input {
        ARRAY,
        IMAGE,
        BEST
    }

    public enum Correlation {
        ZNCC,
        ZNSSD,
        BEST
    }

    public enum Type {

        CL1D_I_V_LL_D,
        CL1D_I_V_LL_MC_D,
        CL2D_Int_D,
        CL15D_pF_D,
        BEST
    }

}
