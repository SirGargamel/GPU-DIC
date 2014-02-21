package cz.tul.dic.output;

import cz.tul.dic.data.task.TaskContainer;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author Petr Jecmen
 */
public class ExportImageFile implements IExporter {

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

        ImageIO.write(ExportUtils.createImageResult(tc, position), "BMP", target);
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
