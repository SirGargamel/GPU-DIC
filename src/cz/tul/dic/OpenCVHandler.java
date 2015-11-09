package cz.tul.dic;

import java.lang.reflect.Field;
import org.opencv.core.Core;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Ječmen
 */
public class OpenCVHandler {
    
    private static boolean LOADED = false;

    public static void loadLibrary() {
        if (!LOADED) {
            try {
                final String osName = System.getProperty("os.name");
                final String libPath;
                final String msg;
                if (osName.startsWith("Windows")) {
                    int bitness = Integer.parseInt(System.getProperty("sun.arch.data.model"));
                    switch (bitness) {
                        case 32:
                            msg = "32 bit detected";
                            libPath = "lib/opencv/x86";
                            break;
                        case 64:
                            msg = "64 bit detected";
                            libPath = "lib/opencv/x64";
                            break;
                        default:
                            msg = "Unknown bit detected - trying with 32 bit";
                            libPath = "lib/opencv/x86";
                            break;
                    }
                } else {
                    msg = "Unsupported type of OS - " + osName;
                    libPath = "lib/opencv";
                }

                System.setProperty("java.library.path", libPath);

                final Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
                fieldSysPath.setAccessible(true);
                fieldSysPath.set(null, null);
//
                System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
                LOADED = true;
                Logger.info("OpenCV loaded successfully, {}.", msg);
            } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException ex) {
                throw new RuntimeException("Failed to load opencv native library.", ex);
            }
        }
    }
}
