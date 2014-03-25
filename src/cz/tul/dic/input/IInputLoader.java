package cz.tul.dic.input;

import cz.tul.dic.data.Image;
import cz.tul.dic.data.task.TaskContainer;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Petr Jecmen
 */
public interface IInputLoader {

    List<Image> loadData(final Object in, final TaskContainer tc) throws IOException;
    
    Class getSupporteType();
    
}
