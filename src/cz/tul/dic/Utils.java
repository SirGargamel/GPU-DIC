package cz.tul.dic;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.io.File;
import java.io.IOException;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.LoggingLevel;

/**
 *
 * @author Petr Jecmen
 */
public class Utils {

    private static final String TEMP_DIR_NAME = "temp";

    public static File getTempDir(final TaskContainer tc) {
        final File dir = (File) (tc.getParameter(TaskParameter.DIR));
        final String tempPath = dir.getAbsolutePath().concat(File.separator).concat(TEMP_DIR_NAME);
        final File temp = new File(tempPath);
        if (!temp.exists()) {
            temp.mkdirs();
        }

        return temp;
    }

    public static void deleteTempDir(final TaskContainer tc) {                
        final File dir = (File) (tc.getParameter(TaskParameter.DIR));
        final String tempPath = dir.getAbsolutePath().concat(File.separator).concat(TEMP_DIR_NAME);
        final File temp = new File(tempPath);
        if (temp.exists()) {
            Logger.trace("Deleting temp folder {0} and all of its contents.", temp.getAbsolutePath());
            
            File[] list = temp.listFiles();
            for (File f : list) {
                if (!f.delete()) {
                    try {
                        throw new IOException("Error deleting " + f.getAbsolutePath());
                    } catch (IOException ex) {
                        System.err.println(ex.getLocalizedMessage());
                    }
                }
            }
            if (!temp.delete()) {
                try {
                    throw new IOException("Error deleting " + temp.getAbsolutePath());
                } catch (IOException ex) {
                    System.err.println(ex.getLocalizedMessage());
                }
            }
        }
    }

    public static void ensureDirectoryExistence(final File file) {
        if (file.isDirectory()) {
            if (!file.exists()) {
                file.mkdirs();
            }
        } else {
            final File dir = file.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }
    
    public static boolean isLevelLogged(final LoggingLevel testedLevel) {
        final LoggingLevel currentLevel = Logger.getLoggingLevel();
        int indexTestedLevel = 0, indexCurrentLevel = 1;
        final LoggingLevel[] levels = LoggingLevel.values();        
        for (int l = 0; l < levels.length; l++) {
            if (levels[l].equals(testedLevel)) {
                indexTestedLevel = l;
            }
            if (levels[l].equals(currentLevel)) {
                indexCurrentLevel = l;
            }
        }
        return indexCurrentLevel < indexTestedLevel;
    }

}
