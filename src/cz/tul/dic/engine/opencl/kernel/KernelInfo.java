/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernel;

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
    private final MemoryCoalescing memoryCoalescing;
    private final UseLimits useLimits;

    public KernelInfo(final Type type, final Input input, final Correlation correlation, final MemoryCoalescing memoryCoalescing, final UseLimits useLimits) {
        this.type = type;
        this.input = input;
        this.correlation = correlation;
        this.memoryCoalescing = memoryCoalescing;
        this.useLimits = useLimits;
    }

    public static KernelInfo fromConfig(final String configString) {
        final String[] vals = configString.split(SEPARATOR);
        return new KernelInfo(Type.valueOf(vals[0]), Input.valueOf(vals[1]), Correlation.valueOf(vals[2]), MemoryCoalescing.valueOf(vals[3]), UseLimits.valueOf(vals[4]));
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

    public MemoryCoalescing getMemoryCoalescing() {
        return memoryCoalescing;
    }

    public UseLimits getUseLimits() {
        return useLimits;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + Objects.hashCode(this.type);
        hash = 31 * hash + Objects.hashCode(this.input);
        hash = 31 * hash + Objects.hashCode(this.correlation);
        hash = 31 * hash + Objects.hashCode(this.memoryCoalescing);
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
        if (this.memoryCoalescing != other.memoryCoalescing) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return type + SEPARATOR + input + SEPARATOR + correlation + SEPARATOR + memoryCoalescing + SEPARATOR + useLimits;
    }

    public enum Input {
        ARRAY,
        IMAGE,
        BEST
    }

    public enum Correlation {
        ZNCC,
        ZNSSD,
        WZNSSD,
        BEST
    }

    public enum Type {
        CL1D,
        CL2D,
        CL15D_pF,
        BEST
    }

    public enum MemoryCoalescing {
        YES, NO, BEST
    }

    public enum UseLimits {
        YES, NO, BEST
    }
}
