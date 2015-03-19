/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.output;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.engine.strain.StrainResult;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 *
 * @author Petr Jecmen
 */
public class ExportUtils {

    private static final int IMAGE_TYPE = BufferedImage.TYPE_3BYTE_BGR;
    private static final float ALPHA = 0.75f;
    private static final Color COLORBACKGROUND = Color.BLACK;
    private static final Color COLOR_TEXT = Color.BLACK;
    private static final int BAR_SIZE_VERT = 30;    
    private static final int BAR_SIZE_HOR = 15;
    private static final NumberFormat nf = new DecimalFormat("0.0#");
    private static final double COLOR_CENTER = 75 / 360.0;
    private static final double COLOR_GAP = 5 / 360.0;
    private static final double COLOR_RANGE_POS = 160 / 360.0;
    private static final double COLOR_RANGE_NEG = 75 / 360.0;
    private static final int LIMIT_WIDTH = 180;
    private static final int LIMIT_HEIGHT = 180;
    private static final int COLOR_NaN = Color.MAGENTA.getRGB();
    private static boolean debugMode;

    static {
        debugMode = false;
    }

    public static void enableDebugMode() {
        debugMode = true;
    }

    public static boolean isPointInsideROIs(final int x, final int y, final ROI[] rois, final TaskContainer tc, final int round) {
        boolean result = false;

        if (rois != null) {
            for (ROI roi : rois) {
                if (roi == null) {
                    continue;
                }

                if (roi.isPointInside(x, y)) {
                    // check facets                
                    result = true;
                    break;
                }
            }
        } else {
            result = true;
        }

        return result;
    }

    public static double calculateDisplacement(final double[] def, final Direction dir) throws ComputationException {
        double result;
        switch (dir) {
            case Dx:
            case dDx:
                result = def[0];
                break;
            case Dy:
            case dDy:
                result = def[1];
                break;
            case Dabs:
            case dDabs:
                result = Math.sqrt(def[0] * def[0] + def[1] * def[1]);
                break;
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction.");
        }

        return result;
    }

    public static double calculateStrain(final double[] results, final Direction dir) throws ComputationException {
        double result;

        switch (dir) {
            case Exx:
            case dExx:
                result = results[StrainResult.Exx];
                break;
            case Eyy:
            case dEyy:
                result = results[StrainResult.Eyy];
                break;
            case Exy:
            case dExy:
                result = results[StrainResult.Exy];
                break;
            case Eabs:
            case dEabs:
                final double val1 = results[StrainResult.Exx];
                final double val2 = results[StrainResult.Eyy];
                final double val3 = results[StrainResult.Exy];
                result = Math.sqrt(val1 * val1 + val2 * val2 + val3 * val3);
                break;
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction.");
        }

        return result;
    }

    public static double calculateSpeed(final double[] def, final Direction dir, final double time) throws ComputationException {
        double result;
        switch (dir) {
            case rDx:
                result = def[0] / time;
                break;
            case rDy:
                result = def[1] / time;
                break;
            case rDabs:
                result = Math.sqrt(def[0] * def[0] + def[1] * def[1]) / time;
                break;
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction - " + dir);
        }

        return result;
    }

    public static BufferedImage overlayImage(final BufferedImage background, final BufferedImage foreground) {
        final int width = Math.max(background.getWidth(), foreground.getWidth());
        final int height = Math.max(background.getHeight(), foreground.getHeight());
        final BufferedImage out = new BufferedImage(width, height, IMAGE_TYPE);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(background, 0, 0, null);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, ALPHA));
        g.drawImage(foreground, 0, 0, null);
        g.dispose();

        return out;
    }

    public static BufferedImage createImageFromMap(final double[][] mapData, final Direction dir) throws ComputationException {
        if (mapData == null || mapData.length == 0 || mapData[0].length == 0) {
            throw new IllegalArgumentException("Illegal map data.");
        }

        final int width = mapData.length;
        final int height = mapData[1].length;

        double max = 0, min = 0, val;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                val = mapData[x][y];
                if (!Double.isNaN(val)) {
                    if (val > max) {
                        max = val;
                    }
                    if (val < min) {
                        min = val;
                    }
                }
            }
        }

        return createImageFromMap(mapData, dir, max, min);
    }

    public static BufferedImage createImageFromMap(final double[][] mapData, final Direction dir, final double max, final double min) throws ComputationException {
        if (mapData == null || mapData.length == 0 || mapData[0].length == 0) {
            throw new IllegalArgumentException("Illegal map data.");
        }

        final int width = mapData.length;
        final int height = mapData[1].length;

        final BufferedImage out = new BufferedImage(width, height, IMAGE_TYPE);
        Graphics2D g = out.createGraphics();
        g.setColor(COLORBACKGROUND);
        g.drawRect(0, 0, width, height);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                out.setRGB(x, y, deformationToRGB(mapData[x][y], min, max));
            }
        }

        switch (dir) {
            case dDabs:
            case Dabs:
            case dEabs:
            case Eabs:
            case rDabs:                
            case dDy:
            case Dy:
            case dEyy:
            case dExy:
            case Eyy:
            case Exy:
            case rDy:
                drawVerticalBar(out, max, min);
                break;
            case dDx:
            case Dx:
            case dExx:
            case Exx:
            case rDx:
                drawHorizontalBar(out, max, min);
                break;
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction.");
        }

        return out;
    }

    private static int deformationToRGB(final double val, final double min, final double max) {
        final int result;
        if (Double.isNaN(val)) {
            if (debugMode) {
                result = COLOR_NaN;
            } else {
                result = Color.HSBtoRGB(0, 1, 0);
            }
        } else {
            float h, s = 1, b = 1;
            double fract;
            final double midlle = (max + min) / 2.0;
            final double half = max - midlle;
            if (val >= min && val <= max) {
                if (val < midlle) {
                    fract = -(val - midlle) / half;
                    h = (float) (fract * COLOR_RANGE_POS + COLOR_CENTER + COLOR_GAP);
                } else {
                    fract = (val - midlle) / half;
                    h = (float) (COLOR_CENTER - COLOR_GAP - (fract * COLOR_RANGE_NEG));
                }
            } else {
                h = 1;
                s = 0;
                b = 0.5f;
            }

            result = Color.HSBtoRGB(h, s, b);
        }
        return result;
    }

    private static void drawVerticalBar(final BufferedImage image, final double max, final double min) {
        final int height = image.getHeight();

        final Graphics2D g = image.createGraphics();
        final FontMetrics metrics = g.getFontMetrics(g.getFont());

        final int x = image.getWidth() - 1 - BAR_SIZE_VERT;
        final double middle = (max + min) / 2.0;
        final double quarter = (max - middle) / 2.0;

        final int width = image.getWidth();
        // positive part           
        for (int y = 0; y < height; y++) {
            g.setColor(new Color(deformationToRGB(height - 1 - y, 0, height - 1)));
            g.drawRect(x, y, BAR_SIZE_VERT, 1);
        }

        g.setColor(COLOR_TEXT);
        if (height > LIMIT_HEIGHT) {
            g.drawString(nf.format(middle - quarter), width - BAR_SIZE_VERT, height / 4 * 3);
            g.drawString(nf.format(middle + quarter), width - BAR_SIZE_VERT, height / 4);
        }
        g.drawString(nf.format(middle), width - BAR_SIZE_VERT, height / 2 + metrics.getHeight() / 3);
        g.drawString(nf.format(max), width - BAR_SIZE_VERT, metrics.getHeight() / 3 * 2);
        g.drawString(nf.format(min), width - BAR_SIZE_VERT, height - 2);

        g.dispose();
    }

    private static void drawHorizontalBar(final BufferedImage image, final double max, final double min) {
        final int width = image.getWidth();
        final int halfWidth = width / 2;

        final Graphics2D g = image.createGraphics();
        final FontMetrics metrics = g.getFontMetrics(g.getFont());

        final int y = image.getHeight() - 1 - BAR_SIZE_HOR;
        final int tY = image.getHeight() - 3;
        final int tWidth = metrics.stringWidth(nf.format(max));

        final double middle = (max + min) / 2.0;
        final double quarter = (max - middle) / 2.0;        
              
        for (int x = 0; x < width; x++) {
            g.setColor(new Color(deformationToRGB(width - x -1, 0, width - 1)));
            g.drawRect(width - 1 - x, y, 1, BAR_SIZE_HOR);
        }

        g.setColor(COLOR_TEXT);
        if (width > LIMIT_WIDTH) {
            g.drawString(nf.format(middle - quarter), halfWidth - halfWidth / 2, tY);            
            g.drawString(nf.format(middle + quarter), halfWidth + halfWidth / 2 - tWidth, tY);            
        }
        g.drawString(nf.format(min), 0, tY);
        g.drawString(nf.format(max), width - tWidth, tY);

        final String val = nf.format(middle);
        g.drawString(val, halfWidth - metrics.stringWidth(val) / 2, tY);

        g.dispose();
    }

}
