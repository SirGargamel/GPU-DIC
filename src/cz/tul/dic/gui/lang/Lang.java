package cz.tul.dic.gui.lang;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 *
 * @author Petr Jecmen
 */
public class Lang {
    
    private static final ResourceBundle rb;
    
    static {
        rb = ResourceBundle.getBundle("cz.tul.dic.gui.lang.Lang", Locale.getDefault());
    }
    
    public static ResourceBundle getBundle() {
        return rb;
    }
    
    public static String getString(final String key) {
        return rb.getString(key);
    }
    
}
