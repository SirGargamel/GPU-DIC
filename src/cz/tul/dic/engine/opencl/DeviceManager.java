package cz.tul.dic.engine.opencl;

import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLMemory;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.util.Filter;
import java.nio.ByteBuffer;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class DeviceManager {

    private static final CLDevice.Type DEVICE_TYPE = CLDevice.Type.GPU;
    private static final CLPlatform platform;
    private static final CLDevice device;
    private static final CLContext context;

    static {
        @SuppressWarnings("unchecked")
        final CLPlatform tmpP = CLPlatform.getDefault((Filter<CLPlatform>) (CLPlatform i) -> i.getMaxFlopsDevice(CLDevice.Type.GPU) != null && i.listCLDevices(CLDevice.Type.CPU).length == 0);
        if (tmpP == null) {
            platform = CLPlatform.getDefault();
        } else {
            platform = tmpP;
        }

        final CLDevice tmpD = platform.getMaxFlopsDevice(DEVICE_TYPE);
        if (tmpD == null) {
            device = platform.getMaxFlopsDevice();
        } else {
            device = tmpD;
        }
        Logger.debug("Using " + device);

        context = CLContext.create(device);
        context.addCLErrorHandler((String string, ByteBuffer bb, long l) -> {
            Logger.error("CLError - " + string);
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            clearContext();
            if (!context.isReleased()) {
                context.release();
            }
        }));
    }

    public static CLContext getContext() {
        return context;
    }

    public static void clearContext() {
        if (context != null) {
            for (CLMemory mem : context.getMemoryObjects()) {
                mem.release();
            }
        }
    }

    public static CLDevice getDevice() {
        return device;
    }

}
