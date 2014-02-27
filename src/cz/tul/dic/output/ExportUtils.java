package cz.tul.dic.output;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 *
 * @author Petr Jecmen
 */
public class ExportUtils {

    private static final int IMAGE_TYPE = BufferedImage.TYPE_3BYTE_BGR;
    private static final float ALPHA = 0.75f;
    private static final Color BACKGROUND_COLOR = Color.BLACK;

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
        final BufferedImage out = new BufferedImage(background.getWidth(), background.getHeight(), IMAGE_TYPE);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(background, 0, 0, null);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, ALPHA));
        g.drawImage(foreground, 0, 0, null);
        g.dispose();

        return out;
    }

    public static BufferedImage createImageFromMap(final double[][] mapData) {
        if (mapData == null || mapData.length == 0 || mapData[0].length == 0) {
            throw new IllegalArgumentException("Illegal map data.");
        }

        final int width = mapData.length;
        final int height = mapData[1].length;

        double max = -Double.MAX_VALUE;
        double val;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                val = Math.abs(mapData[x][y]);
                if (val > max) {
                    max = val;
                }
            }
        }

        final BufferedImage out = new BufferedImage(width, height, IMAGE_TYPE);
        Graphics2D g = out.createGraphics();
        g.setColor(BACKGROUND_COLOR);
        g.drawRect(0, 0, width, height);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                out.setRGB(x, y, deformationToRGB(mapData[x][y], max));
            }
        }

        return out;
    }

    private static int deformationToRGB(final double val, final double max) {
        int result;
        if (val == 0) {
            result = 0;
        } else if (val > 0) {
            result = ((int) (val / max * 255));
        } else {
            result = ((byte) ((-val) / max * 255)) << 16;
        }
        return result;
    }

}
