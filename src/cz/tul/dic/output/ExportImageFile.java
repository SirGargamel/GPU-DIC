package cz.tul.dic.output;

import cz.tul.dic.data.Image;
import cz.tul.dic.data.task.TaskContainer;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author Petr Jecmen
 */
public class ExportImageFile implements IExporter {

    private static final int IMAGE_TYPE = BufferedImage.TYPE_3BYTE_BGR;
    private static final float ALPHA = 0.75f;
    private static final Color BACKGROUND_COLOR = Color.BLACK;

    @Override
    public void exportResult(ExportTask task, TaskContainer tc) throws IOException {
        final Object o = task.getParam();
        if (!(o instanceof Object[])) {
            throw new IllegalArgumentException("Illegal parameters - " + o);
        }

        final Object[] params = (Object[]) o;
        if (params.length < 2) {
            throw new IllegalArgumentException("Not enough parameters.");
        }
        if (!(params[1] instanceof File)) {
            throw new IllegalArgumentException("Second parameter has to be target file.");
        }

        final int position = Integer.valueOf(params[0].toString());
        final File target = (File) params[1];
        final Image background = tc.getImages().get(position);

        final BufferedImage out = new BufferedImage(background.getWidth(), background.getHeight(), IMAGE_TYPE);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(background, 0, 0, null);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, ALPHA));
        g.drawImage(prepareOverlay(tc, position), 0, 0, null);
        g.dispose();

        ImageIO.write(out, "BMP", target);
    }

    private BufferedImage prepareOverlay(final TaskContainer tc, final int position) {
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
    
    private int deformationToVal(final double[] deformation) {        
        double val = Math.sqrt(deformation[0] * deformation[0] + deformation[1]* deformation[1]);
        return Exporter.deformationToRGB(val);
    }

    @Override
    public ExportTarget getTarget() {
        return ExportTarget.IMAGE;
    }

    @Override
    public ExportMode getMode() {
        return ExportMode.MAP;
    }

}
