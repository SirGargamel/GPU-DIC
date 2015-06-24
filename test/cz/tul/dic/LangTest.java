/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.gui.lang.Lang;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Lenam s.r.o.
 */
public class LangTest {
    
    @Test
    public void testLang() {
        assertNotNull("No bundle available.", Lang.getBundle());
        assertNull("Message should not exist", Lang.getString(""));
        assertEquals("Illegal message received.", "GPU-DIC", Lang.getString("Title"));
        assertTrue("Illegal end of received String.", Lang.getString("IO", "appended").endsWith("appended"));
    }
    
}
