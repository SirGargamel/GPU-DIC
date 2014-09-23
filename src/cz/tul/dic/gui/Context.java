package cz.tul.dic.gui;

import cz.tul.dic.ComputationException;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.data.ExportMode;
import cz.tul.dic.output.target.ExportTarget;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.Exporter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.EnumMap;
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

    private static final String SEPARATOR = ";";
    private static final Context instance;
    private TaskContainer tc;
    private final Map<Integer, Map<Direction, BufferedImage>> exportCacheImages;
    private final Map<Integer, Map<Integer, Map<Direction, double[]>>> exportCachePoints;
    private final Map<String, Map<Direction, double[]>> exportCacheDoublePoints;

    private Context() {
        exportCacheImages = new HashMap<>();
        exportCachePoints = new HashMap<>();
        exportCacheDoublePoints = new HashMap<>();
    }

    public TaskContainer getTc() {
        return tc;
    }

    public void setTc(TaskContainer tc) {
        this.tc = tc;

        exportCacheImages.clear();
        exportCachePoints.clear();
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
                Exporter.export(tc, ExportTask.generateMapExport(dir, ExportTarget.GUI, this, round));
                result = m.get(dir);
            } catch (IOException ex) {
                Logger.error(ex, "Unexpected IO error.");
            }
        }

        return result;
    }

    public Map<Direction, double[]> getPointResult(final int x, final int y) throws ComputationException {
        Map<Integer, Map<Direction, double[]>> m = exportCachePoints.get(x);
        if (m == null) {
            m = new HashMap<>();
            exportCachePoints.put(x, m);
        }

        Map<Direction, double[]> result = m.get(y);
        if (result == null) {
            try {
                Exporter.export(tc, ExportTask.generatePointExport(ExportTarget.GUI, this, x, y));
                result = m.get(y);
            } catch (IOException ex) {
                Logger.error(ex, "Unexpected IO error.");
            }
        }

        return result;
    }

    public Map<Direction, double[]> getComparativeStrain(final int x1, final int y1, final int x2, final int y2) throws ComputationException {
        final String key = generateKey(x1, y1, x2, y2);
        Map<Direction, double[]> result = exportCacheDoublePoints.get(key);
        if (result == null) {
            try {
                Exporter.export(tc, ExportTask.generateDoublePointExport(ExportTarget.GUI, this, x1, y1, x2, y2));
                result = exportCacheDoublePoints.get(key);
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

    public void storePointExport(final Map<Direction, double[]> data, final int x, final int y) {
        if (!(data instanceof EnumMap)) {
            throw new IllegalArgumentException("Illegal type of data - " + data.getClass());
        }

        Map<Integer, Map<Direction, double[]>> m = exportCachePoints.get(x);
        if (m == null) {
            m = new HashMap<>();
            exportCachePoints.put(x, m);
        }

        m.put(y, data);
    }

    public void storePointExport(final Map<Direction, double[]> data, final int x1, final int y1, final int x2, final int y2) {
        if (!(data instanceof EnumMap)) {
            throw new IllegalArgumentException("Illegal type of data - " + data.getClass());
        }

        final String key = generateKey(x1, y1, x2, y2);
        exportCacheDoublePoints.put(key, data);
    }

    private static String generateKey(int... vals) {
        final StringBuilder sb = new StringBuilder();
        {
            for (int i : vals) {
                sb.append(i);
                sb.append(SEPARATOR);
            }
            return sb.toString();
        }
    }
}
