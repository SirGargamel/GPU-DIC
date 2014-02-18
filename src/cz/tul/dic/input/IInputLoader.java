package cz.tul.dic.input;

import cz.tul.dic.data.Image;
import cz.tul.dic.data.TaskContainer;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Petr Jecmen
 */
public interface IInputLoader {

    public List<Image> loadData(final Object in, final TaskContainer tc) throws IOException;
    
    public Class getSupporteType();
    
}
