package cz.tul.dic.output.target;

import cz.tul.dic.Utils;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportMode;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TargetExportCsv implements ITargetExport {

    private static final String SEPARATOR_VALUE = ",";
    private static final String SEPARATOR_LINE = "\n";

    @Override
    public void exportData(Object data, Direction direction, Object targetParam, int[] dataParams, TaskContainer tc) throws IOException {
        if (data instanceof double[]) {
            // export image
            exportLine((double[]) data, targetParam);
        } else if (data instanceof double[][]) {
            // export map
            exportMap((double[][]) data, targetParam);
        } else {
            throw new IllegalArgumentException("Unsupported data for CSV export - " + data.getClass());
        }
    }

    private void exportMap(final double[][] data, final Object targetParam) throws IOException {
        if (!(targetParam instanceof File)) {
            throw new IllegalArgumentException("Illegal type of target parameter - " + targetParam.getClass());
        }

        final File target = (File) targetParam;
        Utils.ensureDirectoryExistence(target);

        try (FileWriter out = new FileWriter(target)) {
            if (data != null) {
                final int width = data.length;
                final int height = data[0].length;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        out.append(Double.toString(data[x][y]));
                        out.append(SEPARATOR_VALUE);
                    }
                    out.append(SEPARATOR_LINE);
                }
            }
        }
    }

    private void exportLine(final double[] data, final Object targetParam) throws IOException {
        if (!(targetParam instanceof File)) {
            throw new IllegalArgumentException("Illegal type of target parameter - " + targetParam.getClass());
        }

        final File target = (File) targetParam;

        try (FileWriter out = new FileWriter(target)) {
            if (data != null) {
                for (int i = 0; i < data.length; i++) {
                    out.append(Double.toString(data[i]));
                    out.append(SEPARATOR_VALUE);
                }
                out.append(SEPARATOR_LINE);
            }
        }
    }

    @Override
    public boolean supportsMode(ExportMode mode) {
        return !ExportMode.SEQUENCE.equals(mode);
    }

}
