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

    private static final String NAME_SEPARATOR = "..";
    private static final String EXT = ".config";
    private static final String SEPARATOR = " :: ";
    private final Map<String, String> data;

    public static Config loadConfig(final File projectDir, final String projectName, final ConfigType configType) throws IOException {
        final String configFileName = projectDir.getAbsolutePath().concat(File.separator).concat(projectName).concat(NAME_SEPARATOR).concat(configType.toString()).concat(EXT);
        final Config result = new Config();

        final File config = new File(configFileName);
        if (config.exists()) {
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

    public static void saveConfig(final File projectDir, final String projectName, final ConfigType configType, final Config config) throws IOException {
        final String configFileName = projectDir.getAbsolutePath().concat(File.separator).concat(projectName).concat(NAME_SEPARATOR).concat(configType.toString()).concat(EXT);
        try (FileWriter fw = new FileWriter(new File(configFileName))) {
            for (Entry<String, String> e : config.entrySet()) {
                fw.write(e.getKey());
                fw.write(SEPARATOR);
                fw.write(e.getValue());
                fw.write("\n");
            }
        }
    }

    public static ConfigType determineType(final File configFile) {
        final String fileName = configFile.getName();
        ConfigType type = null;
        for (ConfigType ct : ConfigType.values()) {
            if (fileName.contains(ct.toString())) {
                type = ct;
                break;
            }
        }
        return type;
    }
    
    public static File determineProjectFile(final File configFile) {
        final String fullPath = configFile.getAbsolutePath();
        final ConfigType ct = determineType(configFile);
        final String projectFilePath = fullPath.replace(EXT, "").replace(ct.toString(), "").replace(NAME_SEPARATOR, "");
        return new File(projectFilePath);
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
