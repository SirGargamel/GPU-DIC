/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.engine.opencl.kernels;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author Petr Ječmen
 */
public class KernelInfo implements Serializable {

    private static final String SEPARATOR = ";";
    private final KernelType type;
    private final boolean usesImage, usesZNCC;

    public KernelInfo(KernelType type, boolean usesImage, final boolean usesZNCC) {
        this.type = type;
        this.usesImage = usesImage;
        this.usesZNCC = usesZNCC;
    }

    public static KernelInfo fromConfig(final String configString) {
        final String[] vals = configString.split(SEPARATOR);
        return new KernelInfo(KernelType.valueOf(vals[0]), Boolean.valueOf(vals[1]), Boolean.valueOf(vals[2]));
    }

    public KernelType getType() {
        return type;
    }

    public boolean usesImage() {
        return usesImage;
    }

    public boolean usesZNCC() {
        return usesZNCC;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 19 * hash + Objects.hashCode(this.type);
        hash = 19 * hash + (this.usesImage ? 1 : 0);
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
        if (this.usesImage != other.usesImage) {
            return false;
        }
        return this.type == other.type;
    }

    @Override
    public String toString() {
        return type + SEPARATOR + usesImage + SEPARATOR + usesZNCC;
    }

}
