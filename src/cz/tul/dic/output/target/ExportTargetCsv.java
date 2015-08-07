package cz.tul.dic.output.target;

import cz.tul.dic.FpsManager;
import cz.tul.dic.Utils;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.CsvWriter;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.data.IExportMode;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExportTargetCsv extends AbstractExportTarget {

    private static final String EXTENSION = ".csv";

    @Override
    public void exportMap(final TaskContainer tc, final IExportMode<double[][]> exporter, final Direction direction, final Object targetParam, final int[] dataParams, final double[] limits) throws IOException {
        exportMap(exporter.exportData(tc, direction, dataParams), targetParam, limits);
    }

    private void exportMap(final double[][] data, final Object targetParam, final double[] limits) throws IOException {
        if (!(targetParam instanceof File)) {
            throw new IllegalArgumentException("Illegal type of target parameter - " + targetParam.getClass());
        }

        final File target = (File) targetParam;
        Utils.ensureDirectoryExistence(target.getParentFile());

        if (data != null) {
            final int width = data.length;
            final int height = data[0].length;
            final String[][] out = new String[height][width];

            final double min = limits[0];
            final double max = limits[1];

            double val;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    val = data[x][y];
                    if (!Double.isNaN(min)) {
                        val = Math.max(val, min);
                    }
                    if (!Double.isNaN(max)) {
                        val = Math.min(val, max);
                    }
                    out[y][x] = Double.toString(val);
                }
            }
            CsvWriter.writeDataToCsv(target, out);
        }
    }

    @Override
    public void exportPoint(final TaskContainer tc, final IExportMode<Map<Direction, double[]>> exporter, final Object targetParam, final int[] dataParams) throws IOException {
        if (!(targetParam instanceof File)) {
            throw new IllegalArgumentException("Illegal type of target parameter - " + targetParam.getClass());
        }

        final Map<Direction, double[]> data = exporter.exportData(tc, null, dataParams);
        final FpsManager fpsM = new FpsManager(tc);

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
    public void exportDoublePoint(final TaskContainer tc, final IExportMode<Map<Direction, double[]>> exporter, final Object targetParam, final int[] dataParams) throws IOException {
        exportPoint(tc, exporter, targetParam, dataParams);
    }

    @Override
    public void exportSequence(final TaskContainer tc, final IExportMode<List<double[][]>> exporter, final Direction direction, final Object targetParam, final double[] limits) throws IOException {
        final List<double[][]> data = exporter.exportData(tc, direction, null);

        final File target = (File) targetParam;
        final String path = target.getAbsolutePath();
        final String subTarget = path.substring(0, path.lastIndexOf('.'));
        
        final FpsManager fpsM = new FpsManager(tc);
        final int posCount = ((int) Math.log10(fpsM.getTime(data.size() - 1))) + 1;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < posCount; i++) {
            sb.append("0");
        }
        sb.append(".0#");
        final DecimalFormatSymbols decimalSymbol = new DecimalFormatSymbols(Locale.getDefault());
        decimalSymbol.setDecimalSeparator('.');
        final NumberFormat nf = new DecimalFormat(sb.toString(), decimalSymbol);

        for (int i = 0; i < data.size(); i++) {
            exportMap(data.get(i), new File(subTarget + "-" + nf.format(fpsM.getTime(i)) + fpsM.getTickUnit() + EXTENSION), limits);
        }
    }

    @Override
    public void exportVideo(final TaskContainer tc, final IExportMode<List<double[][]>> exporter, final Direction direction, final Object targetParam, final double[] limits) throws IOException {
        throw new UnsupportedOperationException("Unsupported mode.");
    }

}
