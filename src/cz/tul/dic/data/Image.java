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
        System.err.println("TODO ImageToGrayscale");
        // TODO
    }
    
}
