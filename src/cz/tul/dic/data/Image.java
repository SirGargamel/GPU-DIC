package cz.tul.dic.data;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author Petr Jecmen
 */
public class Image extends BufferedImage {

    public static Image loadImageFromDisk(final File in) throws IOException {
        if (!in.exists() || !in.isFile()) {
            throw new IllegalArgumentException("Illegal input file.");
        }

        final BufferedImage img = ImageIO.read(in);
        final Image result = new Image(img.getWidth(), img.getHeight(), img.getType());
        result.getGraphics().drawImage(img, 0, 0, null);

        result.convertToGrayscale();

        return result;

    }

    private Image(int width, int height, int imageType) {
        super(width, height, imageType);

    }

    private void convertToGrayscale() {
        final int width = getWidth();
        final int height = getHeight();
        int val, r, g, b;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                val = getRGB(x, y);
                r = (val & 0xff0000) >> 16;
                g = (val & 0xff00) >> 8;
                b = val & 0xff;
                val = (r + g + b) / 3;
                setRGB(x, y, val);
            }
        }
    }

    public int[] toArray() {
        final int width = getWidth();
        final int height = getHeight();
        final int[] result = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[y * width + x] = getRGB(x, y) & 0xff;
            }
        }

        return result;
    }

}
