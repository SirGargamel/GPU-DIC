/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
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
    private final Map<String, Map<Direction, double[]>> exportCachePoints;
    private double[] limits;

    private Context() {
        exportCacheImages = new HashMap<>();
        exportCachePoints = new HashMap<>();
        
        limits = new double[] {Double.NaN, Double.NaN};
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
        BufferedImage result = null;
        try {
            Exporter.export(tc, ExportTask.generateMapExport(dir, ExportTarget.GUI, this, round, limits));
            Map<Direction, BufferedImage> m = exportCacheImages.get(round);
            if (m != null) {
                result = m.get(dir);
            }
        } catch (IOException ex) {
            Logger.error(ex, "Unexpected IO error.");
        }

        return result;
    }

    public Map<Direction, double[]> getPointResult(final int x, final int y) throws ComputationException {
        Map<Direction, double[]> result = null;
        try {
            Exporter.export(tc, ExportTask.generatePointExport(ExportTarget.GUI, this, x, y));
            result = exportCachePoints.get(generateKey(x, y));
        } catch (IOException ex) {
            Logger.error(ex, "Unexpected IO error.");
        }

        return result;
    }

    public Map<Direction, double[]> getComparativeStrain(final int x1, final int y1, final int x2, final int y2) throws ComputationException {
        Map<Direction, double[]> result = null;
        try {
            Exporter.export(tc, ExportTask.generateDoublePointExport(ExportTarget.GUI, this, x1, y1, x2, y2));
            result = exportCachePoints.get(generateKey(x1, y1, x2, y2));
        } catch (IOException ex) {
            Logger.error(ex, "Unexpected IO error.");
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
        exportCachePoints.put(generateKey(x, y), data);
    }

    public void storePointExport(final Map<Direction, double[]> data, final int x1, final int y1, final int x2, final int y2) {
        if (!(data instanceof EnumMap)) {
            throw new IllegalArgumentException("Illegal type of data - " + data.getClass());
        }
        exportCachePoints.put(generateKey(x1, y1, x2, y2), data);
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

    public void setLimits(final double[] limits) {
        this.limits = limits;
    }
}
