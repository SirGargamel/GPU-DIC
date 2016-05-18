/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data;

import cz.tul.dic.engine.memory.AbstractOpenCLMemoryManager;
import cz.tul.dic.engine.memory.BasicMemoryManager;
import cz.tul.dic.output.color.ColorMap;
import java.util.prefs.Preferences;

/**
 *
 * @author Petr Ječmen
 */
public class AppSettings {

    private final Preferences prefs;
    private ColorMap.Type colorMapType;
    private BasicMemoryManager.Type memManagerType;

    private AppSettings() {        
        prefs = Preferences.userNodeForPackage(AppSettings.class);
        colorMapType = ColorMap.Type.valueOf(prefs.get(ColorMap.Type.class.toString(), ColorMap.Type.CoolWarm.toString()));
        memManagerType = AbstractOpenCLMemoryManager.Type.valueOf(prefs.get(AbstractOpenCLMemoryManager.Type.class.toString(), AbstractOpenCLMemoryManager.Type.PREFETCH.toString()));
    }

    public ColorMap.Type getColorMapType() {
        return colorMapType;
    }

    public void setColorMapType(ColorMap.Type colorMapType) {
        this.colorMapType = colorMapType;
        prefs.put(ColorMap.Type.class.toString(), colorMapType.toString());
    }

    public BasicMemoryManager.Type getMemManagerType() {
        return memManagerType;
    }

    public void setMemManagerType(AbstractOpenCLMemoryManager.Type memManagerType) {
        this.memManagerType = memManagerType;
        prefs.put(AbstractOpenCLMemoryManager.Type.class.toString(), memManagerType.toString());
    }

    public static AppSettings getInstance() {
        return AppSettingsHolder.INSTANCE;
    }

    private static class AppSettingsHolder {

        private static final AppSettings INSTANCE = new AppSettings();
    }
}
