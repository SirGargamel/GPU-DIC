/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.gui.lang;

import cz.tul.pj.journal.Journal;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 *
 * @author Petr Jecmen
 */
public final class Lang {

    private static final ResourceBundle BUNDLE;

    static {
        BUNDLE = ResourceBundle.getBundle("cz.tul.dic.gui.lang.Lang", Locale.getDefault());
    }

    private Lang() {
    }

    public static ResourceBundle getBundle() {
        return BUNDLE;
    }

    public static String getString(final String key, final String... params) {
        String result = null;
        try {
            result = BUNDLE.getString(key).replaceAll("\\\\n", "\n");

            for (int i = 0; i < params.length; i++) {
                result = result.replaceAll("\\{".concat(Integer.toString(i).concat("\\}")), params[i]);
            }
        } catch (MissingResourceException ex) {
            Journal.getInstance().addDataEntry(ex, "A resource is missing.");
        }

        return result;
    }

}
