package cz.tul.dic.output.target;

import cz.tul.dic.Utils;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.CsvWriter;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.data.ExportMode;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ExportTargetCsv implements IExportTarget {

    private static final String SEPARATOR_VALUE = ",";
    private static final String SEPARATOR_LINE = "\n";

    @Override
    public void exportData(Object data, Direction direction, Object targetParam, int[] dataParams, TaskContainer tc) throws IOException {
        if (data instanceof Map<?, ?>) {
            // export image
            exportLine((Map<Direction, double[]>) data, targetParam);
        } else if (data instanceof double[][]) {
            // export map
            exportMap((double[][]) data, targetParam);
        } else if (data != null) {
            throw new IllegalArgumentException("Unsupported data for CSV export - " + data.getClass());
        }
    }

    private void exportMap(final double[][] data, final Object targetParam) throws IOException {
        if (!(targetParam instanceof File)) {
            throw new IllegalArgumentException("Illegal type of target parameter - " + targetParam.getClass());
        }

        final File target = (File) targetParam;
        Utils.ensureDirectoryExistence(target.getParentFile());

        if (data != null) {
            final int width = data.length;
            final int height = data[0].length;
            final String[][] out = new String[width][height];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    out[x][y] = Double.toString(data[x][y]);
                }
            }
            CsvWriter.writeDataToCsv(target, out);
        }

    }

    private void exportLine(final Map<Direction, double[]> data, final Object targetParam) throws IOException {
        if (!(targetParam instanceof File)) {
            throw new IllegalArgumentException("Illegal type of target parameter - " + targetParam.getClass());
        }

        final File target = (File) targetParam;

        if (data != null) {
            int l = 0;
            double[] vals;
            for (Direction d : Direction.values()) {
                vals = data.get(d);
                if (vals.length > l) {
                    l = vals.length;
                }
            }
            final String[][] out = new String[Direction.values().length][l + 1];
            // header
            for (Direction d : Direction.values()) {
                out[0][d.ordinal()] = d.toString();
            }
            // data

            for (int i = 0; i < l; i++) {
                for (Direction d : Direction.values()) {
                    vals = data.get(d);
                    if (i < vals.length) {
                        out[i + 1][d.ordinal()] = Double.toString(vals[i]);
                    }
                }
            }
            CsvWriter.writeDataToCsv(target, out);
        }
    }

    @Override
    public boolean supportsMode(ExportMode mode) {
        return !ExportMode.SEQUENCE.equals(mode);
    }

}
