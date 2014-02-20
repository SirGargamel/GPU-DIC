package cz.tul.dic;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.io.File;
import java.io.IOException;

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
            temp.mkdir();
        }

        return temp;
    }

    public static void deleteTempFir(final TaskContainer tc) {
        final File dir = (File) (tc.getParameter(TaskParameter.DIR));
        final String tempPath = dir.getAbsolutePath().concat(File.separator).concat(TEMP_DIR_NAME);
        final File temp = new File(tempPath);
        if (temp.exists()) {
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

}
