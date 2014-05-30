package cz.tul.dic.input;

import cz.tul.dic.Utils;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.LoggingLevel;

public class ImageLoader implements IInputLoader {

    private static final Class<?> TYPE = List.class;

    @Override
    public List<Image> loadData(Object in, TaskContainer tc) throws IOException {        
        final Class<?> c = in.getClass();
        if (!TYPE.isAssignableFrom(c)) {
            throw new IllegalArgumentException("ImageLoader needs a list of files as input.");
        }

        @SuppressWarnings("unchecked")
        final List<File> data = (List<File>) in;
        if (data.isEmpty()) {
            throw new IllegalArgumentException("No images.");
        } else {
            final File first = data.get(0);
            tc.setParameter(TaskParameter.IN, first);
        }

        final List<Image> result = new ArrayList<>(data.size());

        Image img;
        for (int i = 0; i < data.size(); i++) {
            img = Image.loadImageFromDisk(data.get(i));
            result.add(img);
        }

        return result;
    }

    @Override
    public Class getSupporteType() {
        return TYPE;
    }

}
