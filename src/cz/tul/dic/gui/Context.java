package cz.tul.dic.gui;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportMode;
import cz.tul.dic.output.ExportTarget;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class Context {

    static {
        instance = new Context();
    }

    public static Context getInstance() {
        return instance;
    }

    private static final Context instance;
    private TaskContainer tc;
    private final Map<Integer, Map<Direction, BufferedImage>> exportCacheImages;
    private final Map<Integer, Map<Integer, Map<Direction, double[]>>> exportCacheLines;

    private Context() {
        exportCacheImages = new HashMap<>();
        exportCacheLines = new HashMap<>();
    }

    public TaskContainer getTc() {
        return tc;
    }

    public void setTc(TaskContainer tc) {
        this.tc = tc;

        exportCacheImages.clear();
        exportCacheLines.clear();
    }

    public BufferedImage getMapResult(final int round, final Direction dir) throws ComputationException {
        Map<Direction, BufferedImage> m = exportCacheImages.get(round);
        if (m == null) {
            m = new HashMap<>();
            exportCacheImages.put(round, m);
        }

        BufferedImage result = m.get(dir);
        if (result == null) {
            try {
                Exporter.export(ExportTask.generateMapExport(dir, ExportTarget.GUI, this, round), tc);
                result = m.get(dir);
            } catch (IOException ex) {
                Logger.error(ex, "Unexpected IO error.");
            }
        }

        return result;
    }

    public double[] getLineResult(final int x, final int y, final Direction dir) throws ComputationException {
        Map<Integer, Map<Direction, double[]>> m = exportCacheLines.get(x);
        if (m == null) {
            m = new HashMap<>();
            exportCacheLines.put(x, m);                        
        }

        Map<Direction, double[]> m2 = m.get(y);
        if (m2 == null) {
            m2 = new HashMap<>();
            m.put(y, m2);
        }

        double[] result = m2.get(dir);
        if (result == null) {
            try {
                Exporter.export(ExportTask.generateLineExport(dir, ExportTarget.GUI, this, x, y), tc);
                result = m2.get(dir);
            } catch (IOException ex) {
                Logger.error(ex, "Unexpected IO error.");
            }
        }

        return result;
    }

    public void storeMapExport(final Object data, final int round, final ExportMode mode, final Direction dir) {
        if (!(data instanceof BufferedImage)) {
            throw new IllegalArgumentException("Illegal type of data - " + data.getClass());
        }

        Map<Direction, BufferedImage> m = exportCacheImages.get(round);
        if (m == null) {
            m = new HashMap<>();
            exportCacheImages.put(round, m);
        }

        m.put(dir, (BufferedImage) data);
    }

    public void storeLineExport(final Object data, final int x, final int y, final ExportMode mode, final Direction dir) {
        if (!(data instanceof double[])) {
            throw new IllegalArgumentException("Illegal type of data - " + data.getClass());
        }

        Map<Integer, Map<Direction, double[]>> m = exportCacheLines.get(x);
        if (m == null) {
            m = new HashMap<>();
            exportCacheLines.put(x, m);
        }

        Map<Direction, double[]> m2 = m.get(y);
        if (m2 == null) {
            m2 = new HashMap<>();
            m.put(y, m2);
        }

        m2.put(dir, (double[]) data);

    }
}
