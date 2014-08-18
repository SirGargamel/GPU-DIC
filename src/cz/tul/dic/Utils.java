package cz.tul.dic;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    public static File getTempDir(final File in) {
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
        if (!file.exists()) {
            file.mkdirs();
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

    public static class ResultCounter {

        private final Map<String, Integer> counter;

        public ResultCounter() {
            counter = new HashMap<>();
        }

        public void inc(final double[] val) {
            final String key = Arrays.toString(val);
            if (counter.containsKey(key)) {
                counter.put(key, counter.get(key) + 1);
            } else {
                counter.put(key, 1);
            }
        }

        @Override
        public String toString() {
            List<Map.Entry> a = new ArrayList<>(counter.entrySet());
            Collections.sort(a, (Map.Entry o1, Map.Entry o2) -> {               
                return ((Comparable) o1.getValue()).compareTo(o2.getValue());
            });

            final StringBuilder sb = new StringBuilder();
            a.stream().forEach((e) -> {
                sb.append("\n")
                        .append(e.getKey())
                        .append(" ")
                        .append(e.getValue());
            });
            return sb.toString();
        }

        public void reset() {
            counter.clear();
        }
    }

}
