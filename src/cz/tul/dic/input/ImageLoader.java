package cz.tul.dic.input;

import cz.tul.dic.data.Image;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageLoader extends AbstractInputLoader {

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
        }

        final List<Image> result = new ArrayList<>(data.size());

        Image img;
        for (int i = 0; i < data.size(); i++) {
            img = Image.loadImageFromDisk(data.get(i));
            result.add(img);
        }

        final StackTraceElement[] st = Thread.currentThread().getStackTrace();
        if (!st[2].getClassName().equals(VideoLoader.class.getName())) {
            loadUdaFile(data.get(0).getAbsolutePath(), tc);
        }

        return result;
    }

    @Override
    public Class getSupporteType() {
        return TYPE;
    }

}
