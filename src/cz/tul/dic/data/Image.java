package cz.tul.dic.data;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import javax.imageio.ImageIO;

/**
 *
 * @author Petr Jecmen
 */
public class Image extends BufferedImage {

    private int[] grayScale;

    public static Image loadImageFromDisk(final File in) throws IOException {
        if (!in.exists() || !in.isFile()) {
            throw new IllegalArgumentException("Illegal input file.");
        }

        return createImage(ImageIO.read(in));
    }

    public static Image createImage(final BufferedImage img) {
        final Image result = new Image(img.getWidth(), img.getHeight(), img.getType());
        result.getGraphics().drawImage(img, 0, 0, null);

        return result;
    }

    private Image(int width, int height, int imageType) {
        super(width, height, imageType);
    }

    private void createBw() {
        final int width = getWidth();
        final int height = getHeight();
        grayScale = new int[width * height];

        int val, r, g, b;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                val = getRGB(x, y);
                r = (val & 0xff0000) >> 16;
                g = (val & 0xff00) >> 8;
                b = val & 0xff;
                val = (r + g + b) / 3;
                grayScale[y * width + x] = val;
            }
        }
    }

    public int[] toBWArray() {
        if (grayScale == null) {
            createBw();
        }

        return grayScale;
    }

}
