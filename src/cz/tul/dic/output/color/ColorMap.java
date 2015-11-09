/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.output.color;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.SortedMap;
import java.util.TreeMap;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Ječmen
 */
public abstract class ColorMap {

    private static final String EXTENSION = ".map";
    private static final String SEPARATOR = ",";
    private static final int COLOR_NAN = Color.MAGENTA.getRGB();
    private static final int DEFAULT_BIN_COUNT = 1000;
    private final Type colorMapType;
    private SortedMap<Integer, byte[]> colorMapping;
    private boolean debug;

    public ColorMap(final Type colorMapType) {
        this.colorMapType = colorMapType;
        debug = false;
        setBinCout(0);
    }

    // 0 to turn of binning, negative values not allowed
    public final void setBinCout(final int binCount) {
        if (binCount < 0) {
            throw new IllegalArgumentException("Bin count needs to be positive.");
        }

        if (binCount == 0) {
            colorMapping = loadColorMap(colorMapType, DEFAULT_BIN_COUNT);
        } else {
            colorMapping = loadColorMap(colorMapType, binCount);
        }
    }

    private static SortedMap<Integer, byte[]> loadColorMap(final Type colorMapType, final int binCount) {
        SortedMap<Integer, byte[]> result = new TreeMap<>();
        try (BufferedReader bin = new BufferedReader(new InputStreamReader(ColorMap.class.getResourceAsStream(colorMapType.toString().concat(EXTENSION))))) {
            String line;
            String[] split;
            double dVal;
            int iVal;
            byte[] color;
            while (bin.ready()) {
                line = bin.readLine();
                split = line.split(SEPARATOR);
                if (split.length == 4) {
                    try {
                        dVal = Double.valueOf(split[0]);
                        iVal = (int) (dVal * binCount);
                        color = new byte[]{
                            Integer.valueOf(split[1].trim()).byteValue(),
                            Integer.valueOf(split[2].trim()).byteValue(),
                            Integer.valueOf(split[3].trim()).byteValue()};
                        result.put(iVal, color);
                    } catch (NumberFormatException ex) {
                        Logger.warn(ex, "Illegal values in color map file - {}", line);
                    }
                } else {
                    Logger.warn("Illegal line in color map file - {}", line);
                }
            }
        } catch (IOException ex) {
            Logger.error(ex, "Error loading color map file {}", colorMapType);
        }
        return result;
    }

    protected abstract double convertVal(double value);

    public int getRGBColor(final double value) {
        if (Double.isNaN(value)) {
            if (debug) {
                return COLOR_NAN;
            } else {
                return Color.BLACK.getRGB();
            }
        } else {
            final byte[] vals = getColor(convertVal(value));
            int result = Byte.toUnsignedInt(vals[2]);
            result += Byte.toUnsignedInt(vals[1]) << 8;
            result += Byte.toUnsignedInt(vals[0]) << 16;
            return result;
        }

    }

    protected byte[] getColor(final double value) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("Value must be between 0 and 1 (both inclusive)");
        }

        final int key = (int) (value * DEFAULT_BIN_COUNT);
        byte[] result = null;
        if (colorMapping.containsKey(key)) {
            result = colorMapping.get(key);
        } else {
            int prev = 0;
            for (int next : colorMapping.keySet()) {
                if (next > key) {
                    result = interpolate(key, prev, colorMapping.get(prev), next, colorMapping.get(next));
                    break;
                }
                prev = next;
            }
            if (result != null) {
                colorMapping.put(key, result);
            } else {
                result = interpolate(key, prev, colorMapping.get(prev), 0, null);
                colorMapping.put(key, result);
            }
        }
        return result;
    }

    private static byte[] interpolate(final int key, final int prev, final byte[] aPrev, final int next, final byte[] aNext) {
        if (aNext == null) {
            return aPrev;
        } else if (aPrev == null) {
            return aNext;
        }

        final double dif = (key - prev) / (double) (next - prev);
        if (dif < 0.5) {
            return aPrev;
        } else {
            return aNext;
        }
    }

    public void enableDebugMode(boolean debugMode) {
        debug = debugMode;
    }

    public enum Type {
        CoolWarm,
        Rainbow
    }

}
