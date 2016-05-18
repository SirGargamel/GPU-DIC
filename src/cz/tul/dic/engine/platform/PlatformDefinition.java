package cz.tul.dic.engine.platform;

import cz.tul.dic.engine.DeviceType;
import cz.tul.dic.engine.KernelInfo;
import java.util.Objects;

/**
 *
 * @author user
 */
public class PlatformDefinition {

    private static final String SEPARATOR = ":";
    private final PlatformType platform;
    private final DeviceType device;    
    private final KernelInfo kernelInfo;

    public PlatformDefinition(PlatformType platform, DeviceType device, KernelInfo kernelInfo) {
        this.platform = platform;
        this.device = device;
        this.kernelInfo = kernelInfo;
    }

    public PlatformType getPlatform() {
        return platform;
    }

    public DeviceType getDevice() {
        return device;
    }

    public KernelInfo getKernelInfo() {
        return kernelInfo;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.platform);
        hash = 23 * hash + Objects.hashCode(this.device);
        hash = 23 * hash + Objects.hashCode(this.kernelInfo);
        return hash;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PlatformDefinition other = (PlatformDefinition) obj;
        if (this.platform != other.platform) {
            return false;
        }
        if (this.device != other.device) {
            return false;
        }
        if (this.kernelInfo != other.kernelInfo) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(platform).append(SEPARATOR).append(device).append(SEPARATOR).append(kernelInfo);
        return result.toString();
    }
}
