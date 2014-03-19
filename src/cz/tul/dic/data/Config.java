package cz.tul.dic.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Petr Jecmen
 */
public class Config {

    private static final String EXT = ".config";
    private static final String SEPARATOR = " :: ";
    private static File projectDir;
    private static boolean enableConfigs = true;

    public static void setProjectDir(final File dir) {
        if (dir.isDirectory()) {
            projectDir = dir;
        } else if (dir.isFile()) {
            projectDir = dir.getParentFile();
        } else {
            throw new IllegalArgumentException("Illegal project directory - " + dir);
        }
    }

    public static Map<String, String> loadConfig(final String name) throws IOException {
        final String configFileName = projectDir.getAbsolutePath().concat(File.separator).concat(name).concat(EXT);
        final Map<String, String> result = new LinkedHashMap<>();

        final File config = new File(configFileName);
        if (enableConfigs && config.exists()) {
            String line;
            String[] split;
            try (BufferedReader br = new BufferedReader(new FileReader(configFileName))) {
                while (br.ready()) {
                    line = br.readLine();
                    split = line.split(SEPARATOR);
                    if (split.length != 2) {
                        throw new IOException("Illegal text found inside config file - " + line);
                    } else {
                        result.put(split[0], split[1]);
                    }
                }
            }
        }
        return result;
    }

    public static void saveConfig(final String name, final Map<String, String> data) throws IOException {
        final String configFileName = projectDir.getAbsolutePath().concat(File.separator).concat(name).concat(EXT);
        try (FileWriter fw = new FileWriter(new File(configFileName))) {
            for (Entry<String, String> e : data.entrySet()) {
                fw.write(e.getKey());
                fw.write(SEPARATOR);
                fw.write(e.getValue());
                fw.write("\n");
            }
        }
    }

    public static void enableConfigs(boolean enable) {
        enableConfigs = enable;
    }

}
