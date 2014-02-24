/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.tul.dic.output.target;

import cz.tul.dic.Utils;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskContainerUtils;
import cz.tul.dic.output.ExportUtils;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import javax.imageio.ImageIO;

/**
 *
 * @author Petr Jecmen
 */


public class TargetExportFile implements ITargetExport {

    private static final String IMAGE_EXTENSION = ".bmp";
    private static final String SCRIPT_NAME = "scriptVideoJoin.vcf";
    private static final String SCRIPT_FILE = "%FILE%";
    private static final String SCRIPT_TARGET = "%TARGET%";
    private static final File VIRTUAL_DUB = new File("virtualDub\\VirtualDub.exe");

    @Override
    public void exportData(Object data, Object targetParam, int[] dataParams, final TaskContainer tc) throws IOException {
        if (data instanceof double[][]) {
            // export image
            exportImage((double[][]) data, targetParam, dataParams, tc);
        } else if (data instanceof List) {
            // export video
            exportVideo((List<double[][]>) data, targetParam, tc);
        } else {
            throw new IllegalArgumentException("Unsupported data for file export - " + data.getClass());
        }
    }

    private void exportImage(final double[][] data, final Object targetParams, int[] dataParams, final TaskContainer tc) throws IOException {
        if (!(targetParams instanceof File)) {
            throw new IllegalArgumentException("Illegal type of target parameter - " + targetParams.getClass());
        }
        if (dataParams.length < 1) {
            throw new IllegalArgumentException("Not enough data parameters.");
        }

        final int position = dataParams[0];
        final File target = (File) targetParams;

        final BufferedImage background = tc.getImages().get(position);
        final BufferedImage overlay = ExportUtils.createImageFromMap(data);
        
        ImageIO.write(ExportUtils.overlayImage(background, overlay), "BMP", target);
    }
    
    public void exportVideo(final List<double[][]> data, final Object targetParams, TaskContainer tc) throws IOException {        
        if (!(targetParams instanceof File)) {
            throw new IllegalArgumentException("Input parameter has to be the target file. - " + targetParams);
        }

        final File out = (File) targetParams;
        final String fullName = out.getName();

        final String name = fullName.substring(0, fullName.lastIndexOf("."));        

        final File temp = Utils.getTempDir(tc);
        final int roundCount = TaskContainerUtils.getRoundCount(tc);
        
        if (roundCount != data.size()) {
            throw new IllegalArgumentException("Provided data length and round count mismatch.");
        }

        File target;
        for (int i = 0; i < roundCount; i++) {
            target = new File(temp.getAbsolutePath() + File.separator + name + i + IMAGE_EXTENSION);
            exportImage(data.get(i), target, new int[] {i}, tc);
            
        }
        // prepare script
        String script = loadScript();        
        script = script.replace(SCRIPT_FILE, temp.getAbsolutePath() + File.separator + name + "0" + IMAGE_EXTENSION);
        script = script.replace(SCRIPT_TARGET, out.getAbsolutePath());
        final String scriptPath = temp.getAbsolutePath().concat(File.separator).concat(SCRIPT_NAME);
        saveScript(script, new File(scriptPath));
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

        Utils.deleteTempFir(tc);
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

}