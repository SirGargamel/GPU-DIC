/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic.output.target;

import cz.tul.dic.ComputationException;
import cz.tul.dic.Utils;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.output.Direction;
import cz.tul.dic.output.ExportMode;
import cz.tul.dic.output.ExportTarget;
import cz.tul.dic.output.ExportTask;
import cz.tul.dic.output.ExportUtils;
import cz.tul.dic.output.Exporter;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import javax.imageio.ImageIO;

/**
 *
 * @author Petr Jecmen
 */
public class TargetExportFile implements ITargetExport {

    private static final String EXTENSION_IMAGE = ".bmp";
    private static final String EXTENSION_CSV = ".csv";
    private static final String SCRIPT_NAME = "scriptVideoJoin.vcf";
    private static final String SCRIPT_FILE = "%FILE%";
    private static final String SCRIPT_TARGET = "%TARGET%";
    private static final File VIRTUAL_DUB = new File("virtualDub\\VirtualDub.exe");

    @Override
    @SuppressWarnings("unchecked")
    public void exportData(Object data, Direction direction, Object targetParam, int[] dataParams, final TaskContainer tc) throws IOException, ComputationException {
        if (data instanceof double[][]) {
            // export image
            exportImage((double[][]) data, direction, targetParam, dataParams, tc);
        } else if (data instanceof List) {
            List<double[][]> dataList = (List<double[][]>) data;
            // export video
            if (dataParams == null || dataParams[0] == ExportTask.EXPORT_SEQUENCE_AVI) {
                exportVideo(dataList, direction, targetParam, tc);
            } else if (dataParams[0] == ExportTask.EXPORT_SEQUENCE_BMP || dataParams[0] == ExportTask.EXPORT_SEQUENCE_CSV) {
                boolean image = dataParams[0] == ExportTask.EXPORT_SEQUENCE_BMP;

                final File target = (File) targetParam;
                final String path = target.getAbsolutePath();
                final String subTarget = path.substring(0, path.lastIndexOf("."));

                final int posCount = ((int) Math.log10(dataList.size())) + 1;
                final StringBuilder sb = new StringBuilder();
                for (int i = 0; i < posCount; i++) {
                    sb.append("0");
                }
                final NumberFormat nf = new DecimalFormat(sb.toString());
                for (int i = 0; i < dataList.size(); i++) {

                    if (image) {
                        if (dataList.get(i) != null) {
                            exportImage(dataList.get(i), direction, new File(subTarget + "-" + nf.format(i) + EXTENSION_IMAGE), new int[]{i}, tc);
                        }
                    } else {
                        Exporter.exportData(new ExportTask(direction, ExportMode.SEQUENCE, ExportTarget.CSV, new File(subTarget + "-" + nf.format(i) + EXTENSION_CSV), null), tc, dataList.get(i));
                    }
                }
            }
        } else if (data != null) {
            throw new IllegalArgumentException("Unsupported data for file export - " + data.getClass());
        }
    }

    private void exportImage(final double[][] data, Direction direction, final Object targetParams, int[] dataParams, final TaskContainer tc) throws IOException, ComputationException {
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
            overlay = ExportUtils.overlayImage(background, ExportUtils.createImageFromMap(data, direction));
        } else {
            overlay = background;
        }

        ImageIO.write(overlay, "BMP", target);
    }

    private void exportVideo(final List<double[][]> data, Direction direction, final Object targetParams, TaskContainer tc) throws IOException, ComputationException {
        if (!(targetParams instanceof File)) {
            throw new IllegalArgumentException("Input parameter has to be the target file. - " + targetParams);
        }

        final File out = (File) targetParams;
        Utils.ensureDirectoryExistence(out.getParentFile());
        final String fullName = out.getName();

        final String name = fullName.substring(0, fullName.lastIndexOf("."));

        final File temp = Utils.getTempDir((File) tc.getParameter(TaskParameter.IN));

        double globalMaxPos = -Double.MAX_VALUE, globalMaxNeg = Double.MAX_VALUE;
        for (double[][] daa : data) {
            if (daa != null) {
                for (double[] da : daa) {
                    for (double d : da) {
                        if (d > globalMaxPos) {
                            globalMaxPos = d;
                        }
                        if (d < globalMaxNeg) {
                            globalMaxNeg = d;
                        }
                    }
                }
            }
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
            target = new File(temp.getAbsolutePath() + File.separator + name + nf.format(i) + EXTENSION_IMAGE);
            Utils.ensureDirectoryExistence(target.getParentFile());
            final BufferedImage background = tc.getImage(i);
            final BufferedImage overlay;
            map = data.get(i);
            if (map != null) {
                overlay = ExportUtils.overlayImage(background, ExportUtils.createImageFromMap(map, direction, globalMaxPos, globalMaxNeg));
            } else {
                overlay = background;
            }

            ImageIO.write(overlay, "BMP", target);
            Utils.markTempFilesForDeletion(tc, target);
        }
        // prepare script
        String script = loadScript();
        script = script.replace(SCRIPT_FILE, temp.getAbsolutePath() + File.separator + name + nf.format(0) + EXTENSION_IMAGE);
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

    private String loadScript() throws IOException {
        InputStream in = TargetExportFile.class.getResourceAsStream(SCRIPT_NAME);
        BufferedReader bin = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        while (bin.ready()) {
            sb.append(bin.readLine());
            sb.append("\n");
        }
        return sb.toString();
    }

    private void saveScript(final String script, final File target) throws IOException {
        try (FileWriter out = new FileWriter(target)) {
            out.write(extendBackslashes(script));
        }
    }

    private String extendBackslashes(final String in) {
        return in.replaceAll("\\\\", "\\\\\\\\");
    }

    @Override
    public boolean supportsMode(ExportMode mode) {
        return !ExportMode.LINE.equals(mode);
    }

}
