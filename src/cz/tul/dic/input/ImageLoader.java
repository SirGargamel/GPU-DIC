package cz.tul.dic.input;

import cz.tul.dic.data.Image;
import cz.tul.dic.data.TaskContainer;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class ImageLoader implements IInputLoader {
    
    private static final List<File> type;

    static {
        type = Arrays.asList(new File[0]);
    }
    
    @Override
    public void loadData(Object in, TaskContainer tc) throws IOException {
        if (!in.getClass().isAssignableFrom(type.getClass())) {
            throw new IllegalArgumentException("ImageLoader needs a list of files as input.");
        }
        
        List<File> data = (List<File>) in;
        
        Image img;
        for (int i = 0; i < data.size(); i++) {
            img = Image.loadImageFromDisk(data.get(i));
            tc.addImage(img);
        }
    }

    @Override
    public Class getSupporteType() {
        return type.getClass();
    }
    
}
