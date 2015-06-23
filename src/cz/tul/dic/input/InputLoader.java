/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.input;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.task.TaskContainer;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public final class InputLoader {

    private static final Map<Class, AbstractInputLoader> loaders;

    static {
        loaders = new HashMap<>();

        AbstractInputLoader il = new VideoLoader();
        loaders.put(il.getSupporteType(), il);

        il = new ImageLoader();
        loaders.put(il.getSupporteType(), il);
    }

    private InputLoader() {
    }

    public static void loadInput(final TaskContainer tc) throws IOException, ComputationException {
        final Object in = tc.getInput();
        final Class<?> cls = in.getClass();
        AbstractInputLoader loader = null;

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
            try {
                final List<Image> images = loader.loadData(in, tc);
                for (int i = 0; i < images.size(); i++) {
                    tc.addImage(images.get(i));
                }
                final StringBuilder sb = new StringBuilder();
                sb.append("InputLoader statistics - loaded ");
                sb.append(images.size());
                sb.append(" images from ");
                sb.append(in);
                sb.append(" using ");
                sb.append(loader.getClass());
                Logger.trace(sb.toString());
            } catch (ComputationException ex) {
                if (ex.getExceptionCause().equals(ComputationExceptionCause.ILLEGAL_TASK_DATA)) {
                    if (tc.getImages().isEmpty()) {
                        throw ex;
                    }
                } else {
                    throw ex;
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported type of input data - " + cls.toString());
        }
    }

}
