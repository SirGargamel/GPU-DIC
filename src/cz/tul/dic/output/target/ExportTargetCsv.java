package cz.tul.dic.output.target;

import cz.tul.dic.FpsManager;
import cz.tul.dic.Utils;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.output.CsvWriter;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.data.ExportMode;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ExportTargetCsv implements IExportTarget {    
    
    @Override
    public void exportData(Object data, Direction direction, Object targetParam, int[] dataParams, TaskContainer tc) throws IOException {
        if (data instanceof Map<?, ?>) {
            // export image
            exportPoint((Map<Direction, double[]>) data, targetParam, (int) tc.getParameter(TaskParameter.FPS));
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
            final String[][] out = new String[height][width];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    out[y][x] = Double.toString(data[x][y]);
                }
            }
            CsvWriter.writeDataToCsv(target, out);
        }

    }

    private void exportPoint(final Map<Direction, double[]> data, final Object targetParam, final int fps) throws IOException {
        if (!(targetParam instanceof File)) {
            throw new IllegalArgumentException("Illegal type of target parameter - " + targetParam.getClass());
        }

        
        final File target = (File) targetParam;
        final FpsManager fpsM = new FpsManager(fps);        

        if (data != null) {
            int l = 0;
            double[] vals;
            for (Direction d : Direction.values()) {
                vals = data.get(d);
                if (vals.length > l) {
                    l = vals.length;
                }
            }
            final String[][] out = new String[l + 1][Direction.values().length + 1];
            // header
            out[0][0] = fpsM.buildTimeDescription();
            for (Direction d : Direction.values()) {
                out[0][d.ordinal() + 1] = d.toString();
            }
            // data

            for (int i = 0; i < l; i++) {
                out[i + 1][0] = Utils.format(fpsM.getTime(i + 1));
                for (Direction d : Direction.values()) {
                    vals = data.get(d);
                    if (i < vals.length) {
                        out[i + 1][d.ordinal() + 1] = Utils.format(vals[i]);
                    }
                }
            }
            CsvWriter.writeDataToCsv(target, out);
        }
    }

    @Override
    public boolean supportsMode(ExportMode mode) {
        return ExportMode.POINT.equals(mode) || ExportMode.MAP.equals(mode);
    }

}
