package cz.tul.dic.output.target;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.ExportMode;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.Format;

public class TargetExportCsv implements ITargetExport {

    private static final String SEPARATOR_VALUE = ",";
    private static final String SEPARATOR_LINE = "\n";
    private static final Format numberFormat = new DecimalFormat("###.##");

    @Override
    public void exportData(Object data, Object targetParam, int[] dataParams, TaskContainer tc) throws IOException {
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

        try (FileWriter out = new FileWriter(target)) {
            for (double[] data1 : data) {
                for (int x = 0; x < data1.length; x++) {
                    out.append(numberFormat.format(data1[x]));
                    out.append(SEPARATOR_VALUE);
                }
                out.append(SEPARATOR_LINE);
            }
        }
    }

    private void exportLine(final double[] data, final Object targetParam) throws IOException {
        if (!(targetParam instanceof File)) {
            throw new IllegalArgumentException("Illegal type of target parameter - " + targetParam.getClass());
        }

        final File target = (File) targetParam;

        try (FileWriter out = new FileWriter(target)) {
            for (int i = 0; i < data.length; i++) {
                out.append(numberFormat.format(data[i]));
                out.append(SEPARATOR_VALUE);
            }
            out.append(SEPARATOR_LINE);
        }
    }

    @Override
    public boolean supportsMode(ExportMode mode) {
        return !ExportMode.SEQUENCE.equals(mode);
    }

}
