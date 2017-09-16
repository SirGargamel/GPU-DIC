/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.output;

import cz.tul.dic.data.AppSettings;
import cz.tul.dic.data.result.StrainResult;
import cz.tul.dic.data.roi.AbstractROI;
import cz.tul.dic.output.color.AbsoluteColorMap;
import cz.tul.dic.output.color.ColorMap;
import cz.tul.dic.output.color.GeneralColorMap;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

/**
 *
 * @author Petr Jecmen
 */
public final class ExportUtils {

    private static final String UNSUPPORTED_DIRECTION = "Unsupported direction - ";
    private static final int IMAGE_TYPE = BufferedImage.TYPE_3BYTE_BGR;
    private static final float ALPHA = 0.75f;
    private static final Color COLORBACKGROUND = Color.BLACK;
    private static final Color COLOR_TEXT = Color.BLACK;
    private static final int BAR_SIZE_VERT = 30;
    private static final int BAR_SIZE_HOR = 15;
    private static final NumberFormat NF = new DecimalFormat("0.0#");
    private static final int LIMIT_WIDTH = 180;
    private static final int LIMIT_HEIGHT = 180;    
    private static boolean debugMode;

    static {
        debugMode = false;
    }

    private ExportUtils() {
    }

    public static void enableDebugMode() {
        debugMode = true;
    }

    public static boolean isPointInsideROIs(final int x, final int y, final AbstractROI[] rois) {
        boolean result = false;

        if (rois == null) {
            result = true;
        } else {
            for (AbstractROI roi : rois) {
                if (roi != null && roi.isPointInside(x, y)) {
                    // check subsets                
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    public static double calculateDisplacement(final double[] def, final Direction dir) {
        double result;
        switch (dir) {
            case DX:
            case D_DX:
                result = def[0];
                break;
            case DY:
            case D_DY:
                result = def[1];
                break;
            case DABS:
            case D_DABS:
                result = Math.sqrt(def[0] * def[0] + def[1] * def[1]);
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_DIRECTION + dir);
        }

        return result;
    }

    public static double calculateStrain(final double[] results, final Direction dir) {
        double result;

        switch (dir) {
            case EXX:
            case D_EXX:
                result = results[StrainResult.E_XX];
                break;
            case EYY:
            case D_EYY:
                result = results[StrainResult.E_YY];
                break;
            case EXY:
            case D_EXY:
                result = results[StrainResult.E_XY];
                break;
            case EABS:
            case D_EABS:
                final double val1 = results[StrainResult.E_XX];
                final double val2 = results[StrainResult.E_YY];
                final double val3 = results[StrainResult.E_XY];
                result = Math.sqrt(val1 * val1 + val2 * val2 + val3 * val3);
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_DIRECTION + dir);
        }

        return result;
    }

    public static double calculateSpeed(final double[] def, final Direction dir, final double time) {
        double result;
        switch (dir) {
            case R_DX:
                result = def[0] / time;
                break;
            case R_DY:
                result = def[1] / time;
                break;
            case R_DABS:
                result = Math.sqrt(def[0] * def[0] + def[1] * def[1]) / time;
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_DIRECTION + dir);
        }

        return result;
    }

    public static BufferedImage overlayImage(final BufferedImage background, final BufferedImage foreground) {
        final int width = Math.max(background.getWidth(), foreground.getWidth());
        final int height = Math.max(background.getHeight(), foreground.getHeight());
        final BufferedImage out = new BufferedImage(width, height, IMAGE_TYPE);
        final Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(background, 0, 0, null);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, ALPHA));
        g.drawImage(foreground, 0, 0, null);
        g.dispose();

        return out;
    }

    public static BufferedImage createImageFromMap(final double[][] mapData, final Direction dir) {
        if (mapData == null || mapData.length == 0 || mapData[0].length == 0) {
            throw new IllegalArgumentException("Illegal map data.");
        }

        final double[] minMax = findMinMax(mapData);

        return createImageFromMap(mapData, dir, minMax[0], minMax[1]);
    }

    public static double[] findMinMax(final double[][] data) {
        double max = Double.NEGATIVE_INFINITY, min = Double.POSITIVE_INFINITY;
        for (double[] da : data) {
            for (double d : da) {
                if (Double.isFinite(d)) {
                    if (d > max) {
                        max = d;
                    }
                    if (d < min) {
                        min = d;
                    }
                }
            }
        }
        if (!Double.isFinite(min) || !Double.isFinite(max)) {
            min = 0;
            max = 0;
        }
        return new double[]{min, max};
    }

    public static BufferedImage createImageFromMap(final double[][] mapData, final Direction dir, final double min, final double max) {
        if (mapData == null || mapData.length == 0 || mapData[0].length == 0) {
            throw new IllegalArgumentException("Illegal map data .");
        }

        final int width = mapData.length;
        final int height = mapData[0].length;
        final BufferedImage out;

        final ColorMap.Type colorMapType = AppSettings.getInstance().getColorMapType();
        final ColorMap colorMap;
        switch (dir) {
            case D_DABS:
            case DABS:
            case D_EABS:
            case EABS:
            case R_DABS:
            case Q_D:
            case Q_D_D:
            case O_D_EX:
            case O_D_EY:
            case O_EX:
            case O_EY:
                colorMap = new AbsoluteColorMap(colorMapType, max);
                break;
            case D_DX:
            case DX:
            case D_DY:
            case DY:
            case D_EXX:
            case EXX:
            case D_EYY:
            case EYY:
            case D_EXY:
            case EXY:
            case R_DX:
            case R_DY:
                colorMap = new GeneralColorMap(colorMapType, min, max);
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_DIRECTION + dir);
        }
        colorMap.enableDebugMode(debugMode);

        switch (dir) {
            case D_DABS:
            case DABS:
            case D_EABS:
            case EABS:
            case R_DABS:
            case D_DY:
            case DY:
            case D_EYY:
            case D_EXY:
            case EYY:
            case EXY:
            case R_DY:
            case Q_D:
            case Q_D_D:
            case O_D_EX:
            case O_D_EY:
            case O_EX:
            case O_EY:
                out = new BufferedImage(width + BAR_SIZE_VERT, height, IMAGE_TYPE);
                break;
            case D_DX:
            case DX:
            case D_EXX:
            case EXX:
            case R_DX:
                out = new BufferedImage(width, height + BAR_SIZE_HOR, IMAGE_TYPE);
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_DIRECTION + dir);
        }

        final Graphics2D g = out.createGraphics();
        g.setColor(COLORBACKGROUND);
        g.drawRect(0, 0, width, height);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                out.setRGB(x, y, colorMap.getRGBColor(mapData[x][y], min, max));
            }
        }

        switch (dir) {
            case D_DABS:
            case DABS:
            case D_EABS:
            case EABS:
            case R_DABS:
            case Q_D:
            case Q_D_D:
            case O_D_EX:
            case O_D_EY:
            case O_EX:
            case O_EY:
                drawVerticalBarAbsoluteValues(out, min, max);
                break;
            case D_DY:
            case DY:
            case D_EYY:
            case D_EXY:
            case EYY:
            case EXY:
            case R_DY:
                drawVerticalBar(out, min, max);
                break;
            case D_DX:
            case DX:
            case D_EXX:
            case EXX:
            case R_DX:
                drawHorizontalBar(out, min, max);
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_DIRECTION + dir);
        }

        return out;
    }

    private static void drawVerticalBar(final BufferedImage image, final double min, final double max) {
        final int height = image.getHeight();

        final Graphics2D g = image.createGraphics();
        final FontMetrics metrics = g.getFontMetrics(g.getFont());

        final int x = image.getWidth() - 1 - BAR_SIZE_VERT;
        final double middle = (max + min) / 2.0;
        final double quarter = (max - middle) / 2.0;

        final ColorMap.Type colorMapType = AppSettings.getInstance().getColorMapType();
        final ColorMap colorMap = new GeneralColorMap(colorMapType, 0, height - 1);

        final int width = image.getWidth();
        // positive part           
        for (int y = 0; y < height; y++) {
            g.setColor(new Color(colorMap.getRGBColor(y, 0, height)));
            g.drawRect(x, y, BAR_SIZE_VERT, 1);
        }

        g.setColor(COLOR_TEXT);
        if (height > LIMIT_HEIGHT) {
            g.drawString(NF.format(middle - quarter), width - BAR_SIZE_VERT, height / 4);
            g.drawString(NF.format(middle + quarter), width - BAR_SIZE_VERT, height / 4 * 3);
        }
        g.drawString(NF.format(middle), width - BAR_SIZE_VERT, height / 2 + metrics.getHeight() / 3);
        g.drawString(NF.format(max), width - BAR_SIZE_VERT, height - 2);
        g.drawString(NF.format(min), width - BAR_SIZE_VERT, metrics.getHeight() / 3 * 2);

        g.dispose();
    }

    private static void drawVerticalBarAbsoluteValues(final BufferedImage image, final double min, final double max) {
        final int width = image.getWidth();
        final int height = image.getHeight();

        final Graphics2D g = image.createGraphics();
        final FontMetrics metrics = g.getFontMetrics(g.getFont());

        final int x = image.getWidth() - 1 - BAR_SIZE_VERT;
        final double middle = (max + min) / 2.0;
        final double quarter = (max - middle) / 2.0;

        final ColorMap.Type colorMapType = AppSettings.getInstance().getColorMapType();
        final ColorMap colorMap = new AbsoluteColorMap(colorMapType, height - 1);
        // positive part           
        for (int y = 0; y < height; y++) {
            g.setColor(new Color(colorMap.getRGBColor(height - 1 - y, 0, height)));
            g.drawRect(x, y, BAR_SIZE_VERT, 1);
        }

        g.setColor(COLOR_TEXT);
        if (height > LIMIT_HEIGHT) {
            g.drawString(NF.format(middle - quarter), width - BAR_SIZE_VERT, height / 4 * 3);
            g.drawString(NF.format(middle + quarter), width - BAR_SIZE_VERT, height / 4);
        }
        g.drawString(NF.format(middle), width - BAR_SIZE_VERT, height / 2 + metrics.getHeight() / 3);
        g.drawString(NF.format(max), width - BAR_SIZE_VERT, metrics.getHeight() / 3 * 2);
        g.drawString(NF.format(min), width - BAR_SIZE_VERT, height - 2);

        g.dispose();
    }

    private static void drawHorizontalBar(final BufferedImage image, final double min, final double max) {
        final int width = image.getWidth();
        final int halfWidth = width / 2;

        final Graphics2D g = image.createGraphics();
        final FontMetrics metrics = g.getFontMetrics(g.getFont());

        final int y = image.getHeight() - 1 - BAR_SIZE_HOR;
        final int tY = image.getHeight() - 3;
        final int tWidth = metrics.stringWidth(NF.format(max));

        final double middle = (max + min) / 2.0;
        final double quarter = (max - middle) / 2.0;

        final ColorMap.Type colorMapType = AppSettings.getInstance().getColorMapType();
        final ColorMap colorMap = new GeneralColorMap(colorMapType, 0, width - 1);

        for (int x = 0; x < width; x++) {
            g.setColor(new Color(colorMap.getRGBColor(width - x - 1, 0, width)));
            g.drawRect(width - 1 - x, y, 1, BAR_SIZE_HOR);
        }

        g.setColor(COLOR_TEXT);
        if (width > LIMIT_WIDTH) {
            g.drawString(NF.format(middle - quarter), halfWidth - halfWidth / 2, tY);
            g.drawString(NF.format(middle + quarter), halfWidth + halfWidth / 2 - tWidth, tY);
        }
        g.drawString(NF.format(min), 0, tY);
        g.drawString(NF.format(max), width - tWidth, tY);

        final String val = NF.format(middle);
        g.drawString(val, halfWidth - metrics.stringWidth(val) / 2, tY);

        g.dispose();
    }
    
    public static double[][] generateNanArray(final int width, final int height) {
        final double[][] result = new double[width][height];
        for (double[] dA : result) {
            Arrays.fill(dA, Double.NaN);
        }
        return result;
    }

}
