package cz.tul.dic.output;

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
    private static final Color BACKGROUND_COLOR = Color.BLACK;
    private static final int BAR_SIZE = 20;
    private static final NumberFormat nf = new DecimalFormat("0.0#");
    private static final double BAR_LIMIT = 0.001;

    public static double calculateDisplacement(final double[] def, final Direction dir) {
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
                throw new IllegalArgumentException("Unsupported direction.");
        }

        return result;
    }

    public static double calculateDeformation(final double[][][] results, final int x, final int y, final Direction dir) {
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
                throw new IllegalArgumentException("Unsupported direction.");
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

    public static BufferedImage createImageFromMap(final double[][] mapData, final Direction dir) {
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
        g.setColor(BACKGROUND_COLOR);
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
                throw new UnsupportedOperationException("Unsupported direction.");
        }

        return out;
    }

    public static BufferedImage createImageFromMap(final double[][] mapData, final Direction dir, final double max, final double min) {
        if (mapData == null || mapData.length == 0 || mapData[0].length == 0) {
            throw new IllegalArgumentException("Illegal map data.");
        }

        final int width = mapData.length;
        final int height = mapData[1].length;

        final BufferedImage out = new BufferedImage(width, height, IMAGE_TYPE);
        Graphics2D g = out.createGraphics();
        g.setColor(BACKGROUND_COLOR);
        g.drawRect(0, 0, width, height);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                out.setRGB(x, y, deformationToRGB(mapData[x][y], max, min));
            }
        }

        switch (dir) {
            case ABS:
            case DABS:
            case Y:
            case DY:
                drawVerticalBar(out, max, min);
                break;
            case X:
            case DX:
                drawHorizontalBar(out, max, min);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported direction.");
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
            h = (float) ((90 / 360.0) - (fract * (110 / 360.0)));
        } else {
            fract = val / max;
            h = (float) (fract * (160 / 360.0) + (90 / 360.0));
        }
        return Color.HSBtoRGB(h, s, b);
    }

    private static void drawVerticalBar(final BufferedImage image, final double max) {
        final int height = image.getHeight();

        final Graphics2D g = image.createGraphics();
        final FontMetrics metrics = g.getFontMetrics(g.getFont());

        final int x = image.getWidth() - 1 - BAR_SIZE;

        final int width = image.getWidth();
        String val;
        // positive part   
        if (max > BAR_LIMIT) {
            for (int y = 0; y < height; y++) {
                g.setColor(new Color(deformationToRGB(y, height - 1, 0)));
                g.drawRect(x, y, BAR_SIZE, 1);
            }

            g.setColor(Color.WHITE);
            val = nf.format(max / 4.0);
            g.drawString(val, width - metrics.stringWidth(val), height / 4);
            val = nf.format(max / 2.0);
            g.drawString(val, width - metrics.stringWidth(val), height / 2);
            val = nf.format(max / 4.0 * 3);
            g.drawString(val, width - metrics.stringWidth(val), height / 4 * 3);
            val = nf.format(max);
            g.drawString(val, width - metrics.stringWidth(val), height - 2);
        }

        // zero
        g.setColor(Color.WHITE);
        val = nf.format(0.0);
        g.drawString(val, width - metrics.stringWidth(val), metrics.getHeight() / 3 * 2);

        g.dispose();
    }

    private static void drawVerticalBar(final BufferedImage image, final double maxPos, final double maxNeg) {
        final int height = image.getHeight();
        final int halfHeight = height / 2;

        final Graphics2D g = image.createGraphics();
        final FontMetrics metrics = g.getFontMetrics(g.getFont());

        final int x = image.getWidth() - 1 - BAR_SIZE;

        final int width = image.getWidth();
        String val;

        // negative part
        if (maxNeg < -BAR_LIMIT) {
            for (int y = 0; y < halfHeight; y++) {
                g.setColor(new Color(deformationToRGB(-y, 0, -halfHeight + 1)));
                g.drawRect(x, halfHeight - 1 - y, BAR_SIZE, 1);
            }

            g.setColor(Color.WHITE);
            val = nf.format(maxNeg / 3.0);
            g.drawString(val, width - metrics.stringWidth(val), halfHeight - halfHeight / 3);
            val = nf.format(maxNeg / 3.0 * 2);
            g.drawString(val, width - metrics.stringWidth(val), halfHeight - halfHeight / 3 * 2);
            val = nf.format(maxNeg);
            g.drawString(val, width - metrics.stringWidth(val), metrics.getHeight() / 3 * 2);
        }

        // positive part   
        if (maxPos > BAR_LIMIT) {
            for (int y = 0; y < halfHeight; y++) {
                g.setColor(new Color(deformationToRGB(y, halfHeight - 1, 0)));
                g.drawRect(x, halfHeight + y, BAR_SIZE, 1);
            }

            g.setColor(Color.WHITE);
            val = nf.format(maxPos / 3.0);
            g.drawString(val, width - metrics.stringWidth(val), halfHeight + halfHeight / 3);
            val = nf.format(maxPos / 3.0 * 2);
            g.drawString(val, width - metrics.stringWidth(val), halfHeight + halfHeight / 3 * 2);
            val = nf.format(maxPos);
            g.drawString(val, width - metrics.stringWidth(val), height - 2);
        }

        // zero
        g.setColor(Color.WHITE);
        val = nf.format(0.0);
        g.drawString(val, width - metrics.stringWidth(val), halfHeight + (metrics.getHeight() / 3));

        g.dispose();
    }

    private static void drawHorizontalBar(final BufferedImage image, final double max, final double min) {
        final int width = image.getWidth();
        final int halfWidth = width / 2;

        final Graphics2D g = image.createGraphics();
        final FontMetrics metrics = g.getFontMetrics(g.getFont());

        final int y = image.getHeight() - 1 - BAR_SIZE;
        final int tY = image.getHeight() - 5;
        String val;

        // negative part
        if (min < -BAR_LIMIT) {
            for (int x = 0; x < halfWidth; x++) {
                g.setColor(new Color(deformationToRGB(-x, 0, -halfWidth + 1)));
                g.drawRect(halfWidth - 1 - x, y, 1, BAR_SIZE);
            }

            g.setColor(Color.WHITE);
            g.drawString(nf.format(min / 3.0), halfWidth - halfWidth / 3, tY);
            g.drawString(nf.format(min / 3.0 * 2), halfWidth - halfWidth / 3 * 2, tY);
            g.drawString(nf.format(min), 0, tY);
        }

        // positive part  
        if (max > BAR_LIMIT) {
            for (int x = 0; x < halfWidth; x++) {
                g.setColor(new Color(deformationToRGB(x, halfWidth - 1, 0)));
                g.drawRect(x + halfWidth, y, 1, BAR_SIZE);
            }

            g.setColor(Color.WHITE);
            g.drawString(nf.format(max / 3.0), halfWidth + halfWidth / 3, tY);
            g.drawString(nf.format(max / 3.0 * 2), halfWidth + halfWidth / 3 * 2, tY);
            val = nf.format(max);
            g.drawString(val, width - metrics.stringWidth(val), tY);
        }

        g.setColor(Color.WHITE);
        val = nf.format(0.0);
        g.drawString("0.0", halfWidth - metrics.stringWidth(val) / 2, tY);

        g.dispose();
    }

}
