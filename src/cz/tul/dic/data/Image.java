/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data;

import cz.tul.dic.OpenCVHandler;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 *
 * @author Petr Jecmen
 */
public final class Image extends BufferedImage {

    private final String imageName;
    private byte[] grayScale;
    private byte[][] grayScale2d;
    private byte[] filtered;

    static {
        OpenCVHandler.loadLibrary();
    }

    private Image(final int width, final int height, final int imageType, final String imageName) {
        super(width, height, imageType);
        this.imageName = imageName;
    }

    public static Image loadImageFromDisk(final File in) throws IOException {
        if (!in.exists() || !in.isFile()) {
            throw new IllegalArgumentException("Illegal input file.");
        }

        return createImage(ImageIO.read(in), in.getName());
    }

    public static Image createImage(final BufferedImage img) {
        return createImage(img, null);
    }
    
    public static Image createImage(final BufferedImage img, final String imageName) {
        final Image result = new Image(img.getWidth(), img.getHeight(), img.getType(), imageName);
        result.getGraphics().drawImage(img, 0, 0, null);

        return result;
    }

    public byte[] toBWArray() {
        if (grayScale == null) {
            createBw();
        }

        return grayScale;
    }

    private void createBw() {
        final int width = getWidth();
        final int height = getHeight();
        grayScale = new byte[width * height];
        grayScale2d = new byte[width][height];

        int val;
        byte r, g, b, newVal;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                val = getRGB(x, y);
                r = (byte) ((val & 0xff0000) >> 16);
                g = (byte) ((val & 0xff00) >> 8);
                b = (byte) (val & 0xff);
                newVal = (byte) ((r + g + b) / 3);
                grayScale[y * width + x] = newVal;
                grayScale2d[x][y] = newVal;
            }
        }
    }

    public byte[][] to2DBWArray() {
        if (grayScale == null) {
            createBw();
        }

        return grayScale2d;
    }

    public void filter(int filterSize) {
        final byte[] bw = toBWArray();
        if (filterSize > 0) {
            if (filterSize % 2 == 0) {
                filterSize++;
            }
            final Mat in = new Mat(getWidth(), getHeight(), CvType.CV_8U);
            in.put(0, 0, bw);
            final Mat out = new Mat();
            Imgproc.GaussianBlur(in, out, new Size(filterSize, filterSize), 0, 0);
            filtered = new byte[bw.length];
            out.get(0, 0, filtered);
        } else {
            filtered = bw;
        }
    }

    public byte[] toFiltered() {
        final byte[] result = filtered == null ? toBWArray() : filtered;
        return result;
    }

    public String getImageName() {
        return imageName;
    }

}
