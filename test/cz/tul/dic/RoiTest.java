/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.data.roi.AbstractROI;
import cz.tul.dic.data.roi.CircularROI;
import cz.tul.dic.data.roi.RectangleROI;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Lenam s.r.o.
 */
public class RoiTest {

    private static final String POINT_SHOULD_BE_INSIDE = "Point should be inside.";
    private static final String COORD_VALUE_MISMATCH = "Coord value mismatch";
    private static final String POINT_SHOULD_BE_OUTSIDE = "Point should be outside.";

    @Test
    public void testRectangleRoi() {
        final AbstractROI roi = new RectangleROI(0, 0, 12, 10);

        assertTrue("Area should be inside.", roi.isAreaInside(1, 1, 2, 2));
        assertTrue("Area should be inside.", roi.isAreaInside(1, 1, 10, 10));
        assertFalse("Area should be outside.", roi.isAreaInside(1, 1, 13, 2));

        assertTrue(POINT_SHOULD_BE_INSIDE, roi.isPointInside(5, 9));
        assertTrue(POINT_SHOULD_BE_INSIDE, roi.isPointInside(0, 0));
        assertTrue(POINT_SHOULD_BE_INSIDE, roi.isPointInside(10, 10));
        assertFalse(POINT_SHOULD_BE_OUTSIDE, roi.isPointInside(-1, 9));
        assertFalse(POINT_SHOULD_BE_OUTSIDE, roi.isPointInside(1, 11));

        assertEquals("Width should be 12.", 13, roi.getWidth(), 0.01);
        assertEquals("Width should be 10.", 11, roi.getHeight(), 0.01);

        assertEquals(COORD_VALUE_MISMATCH, 0, roi.getX1(), 0.01);
        assertEquals(COORD_VALUE_MISMATCH, 0, roi.getY1(), 0.01);
        assertEquals(COORD_VALUE_MISMATCH, 12, roi.getX2(), 0.01);
        assertEquals(COORD_VALUE_MISMATCH, 10, roi.getY2(), 0.01);
    }

    @Test
    public void testCircularRoi() {
        final AbstractROI roi = new CircularROI(5, 6, 5);

        assertTrue("Area should be inside.", roi.isAreaInside(4, 4, 6, 6));
        assertFalse("Area should be outside.", roi.isAreaInside(1, 1, 5, 6));
        assertFalse("Area should be outside.", roi.isAreaInside(1, 1, 13, 18));

        assertTrue(POINT_SHOULD_BE_INSIDE, roi.isPointInside(5, 9));
        assertTrue(POINT_SHOULD_BE_INSIDE, roi.isPointInside(0, 6));
        assertTrue(POINT_SHOULD_BE_INSIDE, roi.isPointInside(10, 6));
        assertFalse(POINT_SHOULD_BE_OUTSIDE, roi.isPointInside(-1, 9));
        assertFalse(POINT_SHOULD_BE_OUTSIDE, roi.isPointInside(1, 12));

        assertEquals("Width should be 10.", 10, roi.getWidth(), 0.01);
        assertEquals("Width should be 10.", 10, roi.getHeight(), 0.01);

        assertEquals(COORD_VALUE_MISMATCH, 0, roi.getX1(), 0.01);
        assertEquals(COORD_VALUE_MISMATCH, 1, roi.getY1(), 0.01);
        assertEquals(COORD_VALUE_MISMATCH, 10, roi.getX2(), 0.01);
        assertEquals(COORD_VALUE_MISMATCH, 11, roi.getY2(), 0.01);
    }

}
