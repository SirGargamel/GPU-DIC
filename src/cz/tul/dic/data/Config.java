/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
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

    private static final String SEPARATOR = " :: ";
    private static final String CLASS_ID = Config.class.getSimpleName();
    private final Map<String, String> data;

    public static Config loadConfig(final File in) throws IOException {
        final Config result;
        if (in.exists()) {
            result = new Config();

            String line;
            String[] split;
            try (BufferedReader br = new BufferedReader(new FileReader(in))) {
                while (br.ready()) {
                    line = br.readLine();
                    split = line.split(SEPARATOR);
                    if (split.length != 2) {
                        throw new IOException("Illegal text found inside config file - " + line);
                    } else {
                        result.put(split[0].trim(), split[1].trim());
                    }
                }
            }
        } else {
            result = null;
        }
        return result;
    }

    public static void saveConfig(final Config config, final ConfigType configType, final File target) throws IOException {
        config.put(CLASS_ID, configType.toString());
        try (FileWriter fw = new FileWriter(target)) {
            for (Entry<String, String> e : config.entrySet()) {
                fw.write(e.getKey());
                fw.write(SEPARATOR);
                fw.write(e.getValue());
                fw.write("\n");
            }
        }
    }

    public static ConfigType determineType(final Config configFile) {
        final String configType = configFile.get(CLASS_ID);
        ConfigType result;
        try {
            result = ConfigType.valueOf(configType);
        } catch (IllegalArgumentException ex) {
            result = ConfigType.TASK;
            configFile.put(CLASS_ID, ConfigType.TASK.toString());
        }
        return result;
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
