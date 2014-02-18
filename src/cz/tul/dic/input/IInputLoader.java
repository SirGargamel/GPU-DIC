package cz.tul.dic.input;

import cz.tul.dic.data.TaskContainer;
import java.io.IOException;

/**
 *
 * @author Petr Jecmen
 */
public interface IInputLoader {

    public void loadData(final Object in, final TaskContainer tc) throws IOException;
    
    public Class getSupporteType();
    
}
