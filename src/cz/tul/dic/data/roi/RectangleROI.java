/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.roi;

import java.io.Serializable;

public class RectangleROI extends AbstractROI implements Serializable {

    private final double x1, y1, x2, y2;

    public RectangleROI(final double x1, final double y1, final double x2, final double y2) {
        super();

        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
    }

    @Override
    public int getX1() {
        return (int) Math.floor(x1);
    }

    @Override
    public int getY1() {
        return (int) Math.floor(y1);
    }

    @Override
    public int getX2() {
        return (int) Math.ceil(x2);
    }

    @Override
    public int getY2() {
        return (int) Math.ceil(y2);
    }

    @Override
    public int getWidth() {
        return (int) Math.ceil(x2 - x1 + 1);
    }

    @Override
    public int getHeight() {
        return (int) Math.ceil(y2 - y1 + 1);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(x1);
        sb.append(SEPARATOR);
        sb.append(y1);
        sb.append(SEPARATOR);
        sb.append(x2);
        sb.append(SEPARATOR);
        sb.append(y2);
        return sb.toString();
    }

    @Override
    public boolean isAreaInside(final double x1, final double y1, final double x2, final double y2) {
        return Math.min(x1, x2) >= this.x1
                && Math.min(y1, y2) >= this.y1
                && Math.max(x1, x2) <= this.x2
                && Math.max(y1, y2) <= this.y2;
    }

    @Override
    public boolean isPointInside(final double x, final double y) {
        return x >= this.x1
                && y >= this.y1
                && x <= this.x2
                && y <= this.y2;
    }

}
