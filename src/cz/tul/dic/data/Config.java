package cz.tul.dic.data;

import cz.tul.dic.Utils;
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
    private final Map<String, String> data;

    public static Config loadConfig(final File in, final ConfigType configType) throws IOException {
        final String configFileName;
        if (in.getName().endsWith(EXT)) {
            configFileName = in.getAbsolutePath();
        } else {
            switch (configType) {
                case TASK:
                    configFileName = in.getAbsolutePath().concat(EXT);
                    break;
                case SEQUENCE:
                    configFileName = Utils.getTempDir(in).getAbsolutePath().concat(File.separator).concat(in.getName()).concat(EXT);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type of config - " + configType);
            }
        }
        final Config result;

        final File config = new File(configFileName);
        if (config.exists()) {
            result = new Config();

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
        } else {
            result = null;
        }
        return result;
    }

    public static void saveConfig(final File in, final ConfigType configType, final Config config) throws IOException {
        final String configFileName;
        switch (configType) {
            case TASK:
                configFileName = in.getAbsolutePath().concat(EXT);
                break;
            case SEQUENCE:
                configFileName = Utils.getTempDir(in).getAbsolutePath().concat(File.separator).concat(in.getName()).concat(EXT);
                break;
            default:
                throw new IllegalArgumentException("Unsupported type of config - " + configType);
        }
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
        final String fileName = configFile.getName().replace(EXT, "");
        final File[] files = configFile.getParentFile().listFiles();

        ConfigType type = ConfigType.SEQUENCE;
        for (File f : files) {
            if (f.getName().equals(fileName)) {
                type = ConfigType.TASK;
                break;
            }
        }

        return type;
    }

    public static File determineProjectFile(final File configFile) {
        final ConfigType ct = determineType(configFile);
        final String projectFilePath;
        switch (ct) {
            case TASK:
                projectFilePath = configFile.getAbsolutePath().replace(EXT, "");
                break;
            case SEQUENCE:
                projectFilePath = configFile.getParentFile().getParentFile().getAbsolutePath().concat(File.separator).concat(configFile.getName()).replace(EXT, "");
                break;
            default:
                throw new IllegalArgumentException("Unsupported type of config - " + ct);
        }

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
