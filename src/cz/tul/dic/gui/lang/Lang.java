/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.gui.lang;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public final class Lang {

    private static final String MISSING_RESOURCE = "MISSING";
    private static final ResourceBundle BUNDLE;

    static {
        BUNDLE = ResourceBundle.getBundle("cz.tul.dic.gui.lang.Lang", Locale.getDefault());
    }

    public static ResourceBundle getBundle() {
        return BUNDLE;
    }

    public static String getString(final String key, final String... params) {
        String result;
        try {
            result = BUNDLE.getString(key).replaceAll("\\\\n", "\n");

            for (int i = 0; i < params.length; i++) {
                result = result.replaceAll("\\{".concat(Integer.toString(i).concat("\\}")), params[i]);
            }
        } catch (MissingResourceException ex) {
            result = MISSING_RESOURCE;
            Logger.debug(ex);
        }

        return result;
    }

    private Lang() {
    }

}
