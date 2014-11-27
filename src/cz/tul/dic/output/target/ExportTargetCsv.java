package cz.tul.dic.output.target;

import cz.tul.dic.ComputationException;
import cz.tul.dic.FpsManager;
import cz.tul.dic.Utils;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.CsvWriter;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.data.IExportMode;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

public class ExportTargetCsv extends AbstractExportTarget {

    private static final String EXTENSION = ".csv";

    @Override
    void exportMap(final TaskContainer tc, final IExportMode<double[][]> exporter, Direction direction, Object targetParam, int[] dataParams) throws IOException, ComputationException {
        exportMap(tc, exporter.exportData(tc, direction, dataParams), direction, targetParam, dataParams);
    }
    
    void exportMap(final TaskContainer tc, final double[][] data, Direction direction, Object targetParam, int[] dataParams) throws IOException, ComputationException {
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

    @Override
    void exportPoint(final TaskContainer tc, final IExportMode<Map<Direction, double[]>> exporter, final Object targetParam, final int[] dataParams) throws IOException, ComputationException {
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
    void exportDoublePoint(final TaskContainer tc, final IExportMode<Map<Direction, double[]>> exporter, final Object targetParam, final int[] dataParams) throws IOException, ComputationException {
        exportPoint(tc, exporter, targetParam, dataParams);
    }

    @Override
    void exportSequence(final TaskContainer tc, final IExportMode<List<double[][]>> exporter, Direction direction, Object targetParam) throws IOException, ComputationException {
        final List<double[][]> data = exporter.exportData(tc, direction, null);
        
        final File target = (File) targetParam;
        final String path = target.getAbsolutePath();
        final String subTarget = path.substring(0, path.lastIndexOf("."));

        final int posCount = ((int) Math.log10(data.size())) + 1;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < posCount; i++) {
            sb.append("0");
        }
        final NumberFormat nf = new DecimalFormat(sb.toString());
        for (int i = 0; i < data.size(); i++) {
            exportMap(tc, data.get(i), direction, new File(subTarget + "-" + nf.format(i) + EXTENSION), null);
        }
    }

    @Override
    void exportVideo(final TaskContainer tc, final IExportMode<List<double[][]>> exporter, Direction direction, Object targetParam) throws IOException, ComputationException {
        throw new UnsupportedOperationException("Unsupported mode.");
    }

}
