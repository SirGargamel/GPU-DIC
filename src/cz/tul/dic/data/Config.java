package cz.tul.dic.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Petr Jecmen
 */
public class Config {

    private static final String EXT = ".config";
    private static final String SEPARATOR = " :: ";    
    private static boolean enableConfigs = true;
    private final Map<String, String> data;   

    public static Config loadConfig(final String name, final File projectDir) throws IOException {
        final String configFileName = projectDir.getAbsolutePath().concat(File.separator).concat(name).concat(EXT);
        final Config result = new Config();

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

    public static void saveConfig(final String name, final File projectDir, final Config config) throws IOException {
        final String configFileName = projectDir.getAbsolutePath().concat(File.separator).concat(name).concat(EXT);
        try (FileWriter fw = new FileWriter(new File(configFileName))) {
            for (Entry<String, String> e : config.entrySet()) {
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
    
    public Config() {
        data = new LinkedHashMap<>();
    }
    
    public void put(final String key, final String value) {
        data.put(key, value);
    }
    
    public Set<Entry<String, String>> entrySet() {
        return data.entrySet();
    }
    
    public String get(final String key) {
        return data.get(key);
    }
    
    public boolean isEmpty() {
        return data.isEmpty();
    }

}
