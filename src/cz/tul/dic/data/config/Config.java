/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

/**
 *
 * @author Petr Jecmen
 */
public class Config {

    private static final String CLASS_ID = Config.class.getSimpleName();
    private final Properties data;

    public Config() {
        data = new Properties();
        data.put(CLASS_ID, ConfigType.TASK.toString());
    }

    public ConfigType getType() {
        final String configType = get(CLASS_ID);
        ConfigType result;
        try {
            result = ConfigType.valueOf(configType);
        } catch (IllegalArgumentException ex) {
            result = ConfigType.TASK;
            setType(ConfigType.TASK);
        }
        return result;
    }

    public Config load(final File in) throws IOException {
        data.load(new FileInputStream(in));
        return this;
    }

    public void save(final File out) throws IOException {
        data.store(new FileOutputStream(out), null);
    }

    public void put(final String key, final String value) {
        data.put(key, value.trim());
    }

    public void setType(final ConfigType configType) {
        put(CLASS_ID, configType.toString());
    }

    public Set<String> keySet() {
        return data.stringPropertyNames();
    }

    public String get(final String key) {
        return data.getProperty(key);
    }

}
