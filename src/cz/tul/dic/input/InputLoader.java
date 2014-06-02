package cz.tul.dic.input;

import cz.tul.dic.Utils;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.task.TaskContainer;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.LoggingLevel;

/**
 *
 * @author Petr Jecmen
 */
public class InputLoader {

    private static final Map<Class, IInputLoader> loaders;

    static {
        loaders = new HashMap<>();

        IInputLoader il = new VideoLoader();
        loaders.put(il.getSupporteType(), il);

        il = new ImageLoader();
        loaders.put(il.getSupporteType(), il);
    }

    public static void loadInput(final TaskContainer tc) throws IOException {
        final Object in = tc.getInput();
        final Class<?> cls = in.getClass();
        IInputLoader loader = null;

        if (loaders.containsKey(cls)) {
            loader = loaders.get(cls);
        } else {
            for (Class<?> c : loaders.keySet()) {
                if (c.isAssignableFrom(cls)) {
                    loader = loaders.get(c);
                    break;
                }
            }
        }

        if (loader != null) {
            final List<Image> images = loader.loadData(in, tc);
            for (int i = 0; i < images.size(); i++) {
                tc.addImage(images.get(i));
            }
            // print statistics
            if (Utils.isLevelLogged(LoggingLevel.TRACE)) {
                final StringBuilder sb = new StringBuilder();
                sb.append("InputLoader statistics - loaded ");
                sb.append(images.size());
                sb.append(" images from ");
                sb.append(in);
                sb.append(" using ");
                sb.append(loader.getClass());                                
                Logger.trace(sb.toString());
            }
        } else {
            throw new IllegalArgumentException("Unsupported type of input data - " + cls.toString());
        }
    }

}
