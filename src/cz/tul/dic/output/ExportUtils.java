package cz.tul.dic.output;

import cz.tul.dic.data.Image;
import cz.tul.dic.data.task.TaskContainer;
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
    
    public static BufferedImage createImageResult(final TaskContainer tc, final int position) {
        final Image background = tc.getImages().get(position);
        final BufferedImage out = new BufferedImage(background.getWidth(), background.getHeight(), IMAGE_TYPE);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(background, 0, 0, null);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, ALPHA));
        g.drawImage(prepareOverlay(tc, position), 0, 0, null);
        g.dispose();
        
        return out;
    }
    
    private static BufferedImage prepareOverlay(final TaskContainer tc, final int position) {
        final Image background = tc.getImages().get(position);
        final BufferedImage out = new BufferedImage(background.getWidth(), background.getHeight(), IMAGE_TYPE);

        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(BACKGROUND_COLOR);
        g.drawRect(0, 0, out.getWidth(), out.getHeight());

        final double[][][] results = tc.getFinalResults(position);

        for (int x = 0; x < results.length; x++) {
            for (int y = 0; y < results[x].length; y++) {
                out.setRGB(x, y, deformationToVal(results[x][y]));
            }
        }

        return out;
    }
    
    private static int deformationToVal(final double[] deformation) {        
        double val = Math.sqrt(deformation[0] * deformation[0] + deformation[1]* deformation[1]);
        return deformationToRGB(val);
    }
    
    public static int deformationToRGB(final double val) {
        int result;
        if (val == 0) {
            result = 0;
        } else if (val < 0) {
            result = ((int) (val)) << 16;
        } else {
            result = ((int) (-val)) & 0xff;
        }
        return result;
    }
    
}
