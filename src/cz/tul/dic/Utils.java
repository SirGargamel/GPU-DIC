package cz.tul.dic;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.LoggingLevel;

/**
 *
 * @author Petr Jecmen
 */
public class Utils {

    private static final String TEMP_DIR_NAME = "temp";
    private static final Map<TaskContainer, List<File>> tempFiles;

    static {
        tempFiles = new HashMap<>();
    }

    public static File getTempDir(final TaskContainer tc) {
        final File in = (File) (tc.getParameter(TaskParameter.IN));
        final String tempPath = in.getParent().concat(File.separator).concat(TEMP_DIR_NAME);
        final File temp = new File(tempPath);
        ensureDirectoryExistence(temp);

        return temp;
    }

    public static void markTempFilesForDeletion(final TaskContainer tc, final File... filesForDeletion) {
        List<File> f = tempFiles.get(tc);
        if (f == null) {
            f = new LinkedList<>();
            tempFiles.put(tc, f);
        }
        f.addAll(Arrays.asList(filesForDeletion));
    }

    public static void deleteTempDir(final TaskContainer tc) {
        final File in = (File) (tc.getParameter(TaskParameter.IN));
        final String tempPath = in.getParent().concat(File.separator).concat(TEMP_DIR_NAME);
        final File temp = new File(tempPath);
        if (temp.exists()) {
            Logger.trace("Deleting files inside temp folder {0}.", temp.getAbsolutePath());

            List<File> list = tempFiles.get(tc);
            for (File f : list) {
                if (!f.delete()) {
                    Logger.warn("Error deleting " + f.getAbsolutePath());
                }
            }
            list.clear();
            if (temp.listFiles().length == 0 && !temp.delete()) {
                Logger.warn("Error deleting " + temp.getAbsolutePath());
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
