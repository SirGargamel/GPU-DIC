/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic.output.target;

import cz.tul.dic.FpsManager;
import cz.tul.dic.Utils;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportUtils;
import cz.tul.dic.output.data.IExportMode;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 *
 * @author Petr Jecmen
 */
public class ExportTargetFile extends AbstractExportTarget {

    private static final String EXTENSION = ".bmp";
    private static final String SCRIPT_NAME = "scriptVideoJoin.vcf";
    private static final String SCRIPT_FILE = "%FILE%";
    private static final String SCRIPT_TARGET = "%TARGET%";
    private static final File VIRTUAL_DUB = new File("virtualDub\\VirtualDub.exe");

    @Override
    public void exportSequence(final TaskContainer tc, final IExportMode<List<double[][]>> exporter, final Direction direction, final Object targetParam, final double[] limits) throws IOException {
        final List<double[][]> data = exporter.exportData(tc, direction, null);

        final File target = (File) targetParam;
        final String path = target.getAbsolutePath();
        final String subTarget = path.substring(0, path.lastIndexOf("."));

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

        final double[] minMax;
        if (Double.isNaN(limits[0]) || Double.isNaN(limits[1])) {
            minMax = findMinMax(data);
            if (!Double.isNaN(limits[0])) {
                minMax[0] = limits[0];
            }
            if (!Double.isNaN(limits[1])) {
                minMax[1] = limits[1];
            }
        } else {
            minMax = new double[]{limits[0], limits[1]};
        }

        for (int i = 0; i < data.size(); i++) {
            if (data.get(i) != null) {
                exportMap(data.get(i), direction, new File(subTarget + "-" + nf.format(fpsM.getTime(i)) + fpsM.getTickUnit() + EXTENSION), new int[]{i}, tc, minMax);
            }
        }
    }

    @Override
    public void exportMap(final TaskContainer tc, final IExportMode<double[][]> exporter, final Direction direction, final Object targetParam, final int[] dataParams, final double[] limits) throws IOException {
        final double[][] data = exporter.exportData(tc, direction, dataParams);

        final double[] minMax;
        if (Double.isNaN(limits[0]) || Double.isNaN(limits[1])) {
            minMax = ExportUtils.findMinMax(data);
            if (!Double.isNaN(limits[0])) {
                minMax[0] = limits[0];
            }
            if (!Double.isNaN(limits[1])) {
                minMax[1] = limits[1];
            }
        } else {
            minMax = new double[]{limits[0], limits[1]};
        }

        exportMap(data, direction, targetParam, dataParams, tc, minMax);
    }

    private void exportMap(final double[][] data, final Direction direction, final Object targetParams, final int[] dataParams, final TaskContainer tc, final double[] limits) throws IOException {
        if (!(targetParams instanceof File)) {
            throw new IllegalArgumentException("Illegal type of target parameter - " + targetParams.getClass());
        }
        if (dataParams.length < 1) {
            throw new IllegalArgumentException("Not enough data parameters.");
        }

        final int position = dataParams[0];
        final File target = (File) targetParams;
        Utils.ensureDirectoryExistence(target.getParentFile());

        final BufferedImage background = tc.getImage(position);
        final BufferedImage overlay;
        if (data != null) {
            overlay = ExportUtils.overlayImage(background, ExportUtils.createImageFromMap(data, direction, limits[0], limits[1]));
        } else {
            overlay = background;
        }

        ImageIO.write(overlay, "BMP", target);
    }

    @Override
    public void exportVideo(final TaskContainer tc, final IExportMode<List<double[][]>> exporter, final Direction direction, final Object targetParams, final double[] limits) throws IOException {
        if (!(targetParams instanceof File)) {
            throw new IllegalArgumentException("Input parameter has to be the target file. - " + targetParams);
        }

        final List<double[][]> data = exporter.exportData(tc, direction, null);

        final File out = (File) targetParams;
        Utils.ensureDirectoryExistence(out.getParentFile());
        final String fullName = out.getName();

        final String name = fullName.substring(0, fullName.lastIndexOf("."));
        final File temp = Utils.getTempDir(out);

        double[] minMax = null;
        if (Double.isNaN(limits[0])) {
            minMax = findMinMax(data);
            limits[0] = minMax[0];
        }
        if (Double.isNaN(limits[1])) {
            if (minMax == null) {
                minMax = findMinMax(data);
            }
            limits[1] = minMax[1];
        }

        final int posCount = ((int) Math.log10(data.size())) + 1;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < posCount; i++) {
            sb.append("0");
        }
        final NumberFormat nf = new DecimalFormat(sb.toString());
        File target;
        double[][] map;
        for (int i = 0; i < data.size(); i++) {
            target = new File(temp.getAbsolutePath() + File.separator + name + nf.format(i) + EXTENSION);
            Utils.ensureDirectoryExistence(target.getParentFile());
            final BufferedImage background = tc.getImage(i);
            final BufferedImage overlay;
            map = data.get(i);
            if (map != null) {
                overlay = ExportUtils.overlayImage(background, ExportUtils.createImageFromMap(map, direction, limits[0], limits[1]));
            } else {
                overlay = background;
            }

            ImageIO.write(overlay, "BMP", target);
            Utils.markTempFilesForDeletion(tc, target);
        }
        // prepare script
        String script = loadScript();
        script = script.replace(SCRIPT_FILE, temp.getAbsolutePath() + File.separator + name + nf.format(0) + EXTENSION);
        script = script.replace(SCRIPT_TARGET, out.getAbsolutePath());
        final String scriptPath = temp.getAbsolutePath().concat(File.separator).concat(SCRIPT_NAME);
        final File scriptFile = new File(scriptPath);
        saveScript(script, scriptFile);
        Utils.markTempFilesForDeletion(tc, scriptFile);
        // launch virtualdub to strip video to images
        final String[] command = {
            extendBackslashes(VIRTUAL_DUB.getAbsolutePath()),
            "/x /s \"" + extendBackslashes(extendBackslashes(scriptPath)) + "\""};
        try {
            final Process p = Runtime.getRuntime().exec(command[0].concat(" ").concat(command[1]));
            int result = p.waitFor();
            if (result != 0) {
                throw new IOException("Video joining has failed.");
            }
        } catch (InterruptedException ex) {
            throw new IOException("VirtualDub has been interrupted.", ex);
        }

        Utils.deleteTempDir(tc);
    }

    private static double[] findMinMax(final List<double[][]> data) {
        double max = Double.NEGATIVE_INFINITY, min = Double.POSITIVE_INFINITY;
        for (double[][] daa : data) {
            if (daa != null) {
                for (double[] da : daa) {
                    for (double d : da) {
                        if (!Double.isNaN(d)) {
                            if (d > max) {
                                max = d;
                            }
                            if (d < min) {
                                min = d;
                            }
                        }
                    }
                }
            }
        }
        if (!Double.isFinite(min) || !Double.isFinite(max)) {
            min = 0;
            max = 0;
        }
        return new double[]{min, max};
    }

    private static String loadScript() throws IOException {
        InputStream in = ExportTargetFile.class
                .getResourceAsStream(SCRIPT_NAME);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader bin = new BufferedReader(new InputStreamReader(in))) {
            while (bin.ready()) {
                sb.append(bin.readLine());
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private static void saveScript(final String script, final File target) throws IOException {
        try (FileWriter out = new FileWriter(target)) {
            out.write(extendBackslashes(script));
        }
    }

    private static String extendBackslashes(final String in) {
        return in.replaceAll("\\\\", "\\\\\\\\");
    }
}
