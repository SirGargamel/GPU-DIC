package cz.tul.dic.output;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.data.roi.ROI;
import cz.tul.dic.data.task.TaskContainer;
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
    private static final int BAR_SIZE_VERTA = 25;
    private static final int BAR_SIZE_HOR = 15;
    private static final NumberFormat nf = new DecimalFormat("0.0#");
    private static final double BAR_LIMIT = 0.001;
    private static final double COLOR_CENTER = 75 / 360.0;
    private static final double COLOR_GAP = 5 / 360.0;
    private static final double COLOR_RANGE_POS = 160 / 360.0;
    private static final double COLOR_RANGE_NEG = 75 / 360.0;
    private static final int LIMIT_WIDTH = 180;
    private static final int LIMIT_HEIGHT = 180;

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
            case X:
                result = def[0];
                break;
            case Y:
                result = def[1];
                break;
            case ABS:
                result = Math.sqrt(def[0] * def[0] + def[1] * def[1]);
                break;
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction.");
        }

        return result;
    }

    public static double calculateDeformation(final double[][][] results, final int x, final int y, final Direction dir) throws ComputationException {
        double result;

        if (x < 0 || y < 0 || (x + 1) >= results.length || (y + 1) >= results[x].length) {
            throw new IllegalArgumentException("Position outside of data range - [" + x + ";" + y + "]");
        }

        switch (dir) {
            case DX:
                result = results[x + 1][y][0] - results[x][y][0];
                break;
            case DY:
                result = results[x][y + 1][1] - results[x][y][1];
                break;
            case DABS:
                final double val1 = results[x + 1][y][0] - results[x][y][0];
                final double val2 = results[x][y + 1][1] - results[x][y][1];
                result = Math.sqrt(val1 * val1 + val2 * val2);
                break;
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction.");
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
                if (val > max) {
                    max = val;
                }
                if (val < min) {
                    min = val;
                }
            }
        }
//        for (double[] da : mapData) {
//            for (double d : da) {
//                if (d > max) {
//                    max = d;
//                }
//                if (d < min) {
//                    min = d;
//                }
//            }
//        }

        final BufferedImage out = new BufferedImage(width, height, IMAGE_TYPE);
        Graphics2D g = out.createGraphics();
        g.setColor(COLORBACKGROUND);
        g.drawRect(0, 0, width, height);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                out.setRGB(x, y, deformationToRGB(mapData[x][y], max, min));
            }
        }

        switch (dir) {
            case ABS:
            case DABS:
                drawVerticalBar(out, max);
                break;
            case Y:
            case DY:
                drawVerticalBar(out, max, min);
                break;
            case X:
            case DX:
                drawHorizontalBar(out, max, min);
                break;
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction.");
        }

        return out;
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
                out.setRGB(x, y, deformationToRGB(mapData[x][y], max, min));
            }
        }

        switch (dir) {
            case ABS:
            case DABS:
                drawVerticalBar(out, max);
                break;
            case Y:
            case DY:
                drawVerticalBar(out, max, min);
                break;
            case X:
            case DX:
                drawHorizontalBar(out, max, min);
                break;
            default:
                throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Unsupported direction.");
        }

        return out;
    }

    private static int deformationToRGB(final double val, final double max, final double min) {
        float h, s = 1, b = 1;
        double fract;
        if (val == 0) {
            h = 0.0f;
            b = 0.0f;
        } else if (val < 0) {
            fract = val / min;
            h = (float) (COLOR_CENTER - COLOR_GAP - (fract * COLOR_RANGE_NEG));
        } else {
            fract = val / max;
            h = (float) (fract * COLOR_RANGE_POS + COLOR_CENTER + COLOR_GAP);
        }
        return Color.HSBtoRGB(h, s, b);
    }

    private static void drawVerticalBar(final BufferedImage image, final double max) {
        final int height = image.getHeight();

        final Graphics2D g = image.createGraphics();
        final FontMetrics metrics = g.getFontMetrics(g.getFont());

        final int x = image.getWidth() - 1 - BAR_SIZE_VERTA;

        final int width = image.getWidth();
        // positive part   
        if (max > BAR_LIMIT) {
            for (int y = 0; y < height; y++) {
                g.setColor(new Color(deformationToRGB(height - 1 - y, height - 1, 0)));
                g.drawRect(x, y, BAR_SIZE_VERTA, 1);
            }

            g.setColor(COLOR_TEXT);

            if (height > LIMIT_HEIGHT) {
                g.drawString(nf.format(max / 4.0), width - BAR_SIZE_VERTA, height / 4 * 3);
                g.drawString(nf.format(max / 4.0 * 3), width - BAR_SIZE_VERTA, height / 4);
            }
            g.drawString(nf.format(max / 2.0), width - BAR_SIZE_VERTA, height / 2);
            g.drawString(nf.format(max), width - BAR_SIZE_VERTA, metrics.getHeight() / 3 * 2);
        }

        // zero
        g.setColor(COLOR_TEXT);
        g.drawString(nf.format(0.0), width - BAR_SIZE_VERTA, height - 2);

        g.dispose();
    }

    private static void drawVerticalBar(final BufferedImage image, final double maxPos, final double maxNeg) {
        final int height = image.getHeight();
        final int halfHeight = height / 2;

        final Graphics2D g = image.createGraphics();
        final FontMetrics metrics = g.getFontMetrics(g.getFont());

        final int x = image.getWidth() - 1 - BAR_SIZE_VERT;

        final int width = image.getWidth();
        final int minusWidth = metrics.stringWidth("-");

        // negative part
        if (maxNeg < -BAR_LIMIT) {
            for (int y = 0; y < halfHeight; y++) {
                g.setColor(new Color(deformationToRGB(-y, 0, -halfHeight + 1)));
                g.drawRect(x, halfHeight - 1 - y, BAR_SIZE_VERT, 1);
            }

            g.setColor(COLOR_TEXT);

            if (height > LIMIT_HEIGHT) {
                g.drawString(nf.format(maxNeg / 3.0), width - BAR_SIZE_VERT, halfHeight - halfHeight / 3);
                g.drawString(nf.format(maxNeg / 3.0 * 2), width - BAR_SIZE_VERT, halfHeight - halfHeight / 3 * 2);
            } else {
                g.drawString(nf.format(maxNeg / 2.0), width - BAR_SIZE_VERT, metrics.getHeight() - halfHeight / 2);
            }
            g.drawString(nf.format(maxNeg), width - BAR_SIZE_VERT, metrics.getHeight() / 3 * 2);

            g.drawString(nf.format(0.0), width - BAR_SIZE_VERT + minusWidth, halfHeight);
        }

        // positive part        
        if (maxPos > BAR_LIMIT) {
            for (int y = 0; y < halfHeight; y++) {
                g.setColor(new Color(deformationToRGB(y, halfHeight - 1, 0)));
                g.drawRect(x, halfHeight + y, BAR_SIZE_VERT, 1);
            }

            g.setColor(COLOR_TEXT);
            if (height > LIMIT_HEIGHT) {
                g.drawString(nf.format(maxPos / 3.0), width - BAR_SIZE_VERT + minusWidth, halfHeight + halfHeight / 3);
                g.drawString(nf.format(maxPos / 3.0 * 2), width - BAR_SIZE_VERT + minusWidth, halfHeight + halfHeight / 3 * 2);
            } else {
                g.drawString(nf.format(maxPos / 2.0), width - BAR_SIZE_VERT + minusWidth, halfHeight + halfHeight / 2);
            }
            g.drawString(nf.format(maxPos), width - BAR_SIZE_VERT + minusWidth, height - 2);
            g.drawString(nf.format(0.0), width - BAR_SIZE_VERT + minusWidth, halfHeight + (metrics.getHeight() * 2 / 3));
        }

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
        String val;

        // negative part
        if (min < -BAR_LIMIT) {
            for (int x = 0; x < halfWidth; x++) {
                g.setColor(new Color(deformationToRGB(-x, 0, -halfWidth + 1)));
                g.drawRect(halfWidth - 1 - x, y, 1, BAR_SIZE_HOR);
            }

            g.setColor(COLOR_TEXT);
            if (width > LIMIT_WIDTH) {
                g.drawString(nf.format(min / 3.0), halfWidth - halfWidth / 3, tY);
                g.drawString(nf.format(min / 3.0 * 2), halfWidth - halfWidth / 3 * 2, tY);
            } else {
                g.drawString(nf.format(min / 2.0), halfWidth - halfWidth / 2 - tWidth / 2, tY);
            }
            g.drawString(nf.format(min), 0, tY);
        }

        // positive part  
        if (max > BAR_LIMIT) {
            for (int x = 0; x < halfWidth; x++) {
                g.setColor(new Color(deformationToRGB(x, halfWidth - 1, 0)));
                g.drawRect(x + halfWidth, y, 1, BAR_SIZE_HOR);
            }

            g.setColor(COLOR_TEXT);
            if (width > LIMIT_WIDTH) {
                g.drawString(nf.format(max / 3.0), halfWidth + halfWidth / 3 - tWidth, tY);
                g.drawString(nf.format(max / 3.0 * 2), halfWidth + halfWidth / 3 * 2 - tWidth, tY);
            } else {
                g.drawString(nf.format(max / 2.0), halfWidth + halfWidth / 2 - tWidth / 2, tY);
            }

            g.drawString(nf.format(max), width - tWidth, tY);
        }

        g.setColor(COLOR_TEXT);
        val = nf.format(0.0);
        g.drawString("0.0", halfWidth - metrics.stringWidth(val) / 2, tY);

        g.dispose();
    }

}
