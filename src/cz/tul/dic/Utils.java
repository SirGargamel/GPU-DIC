package cz.tul.dic;

import cz.tul.dic.data.TaskContainer;
import cz.tul.dic.data.TaskParameter;
import java.io.File;

/**
 *
 * @author Petr Jecmen
 */
public class Utils {
    
    private static final String TEMP_DIR_NAME = "temp";

    public static File getTempDir(final TaskContainer tc) {
        final File dir = (File) tc.getParameter(TaskParameter.DIR);
        final String tempPath = dir.getAbsolutePath().concat(File.separator).concat(TEMP_DIR_NAME);
        return new File(tempPath);
    }
    
}
