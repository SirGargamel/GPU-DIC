package cz.tul.dic.input;

import cz.tul.dic.Utils;
import cz.tul.dic.data.Config;
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

    private static final Class TYPE = List.class;

    @Override
    public List<Image> loadData(Object in, TaskContainer tc) throws IOException {
        if (!TYPE.isAssignableFrom(in.getClass())) {
            throw new IllegalArgumentException("ImageLoader needs a list of files as input.");
        }

        final List<File> data = (List<File>) in;
        if (data.isEmpty()) {
            throw new IllegalArgumentException("No images.");
        } else {            
            tc.setParameter(TaskParameter.DIR, data.get(0).getParentFile());
            Config.setProjectDir(data.get(0).getParentFile());
        }

        final List<Image> result = new ArrayList<>(data.size());

        Image img;
        for (int i = 0; i < data.size(); i++) {
            img = Image.loadImageFromDisk(data.get(i));
            result.add(img);
        }

        if (Utils.isLevelLogged(LoggingLevel.DEBUG)) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Loaded ");
            sb.append(result.size());
            sb.append(" images - ");
            for (File f : data) {
                sb.append(f.getName());
                sb.append(", ");
            }
            sb.setLength(sb.length()-2);
            Logger.debug(sb.toString());
        }

        return result;
    }

    @Override
    public Class getSupporteType() {
        return TYPE;
    }

}
