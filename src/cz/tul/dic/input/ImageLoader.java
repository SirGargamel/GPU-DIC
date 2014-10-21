package cz.tul.dic.input;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
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
    public List<Image> loadData(Object in, TaskContainer tc) throws IOException, ComputationException {
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
        final File inputSource = (File) tc.getParameter(TaskParameter.IN);

        Image img;
        File image;
        for (int i = 0; i < data.size(); i++) {
            image = data.get(i);
            if (!image.exists()) {
                image = new File(inputSource.getParent().concat(File.separator).concat(image.getName()));
                image = new File(image.getName());
                if (!image.exists()) {
                    throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Input file " + image.toString() + " not found.");
                }
            }

            img = Image.loadImageFromDisk(image);
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
