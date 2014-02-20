package cz.tul.dic.input;

import cz.tul.dic.Utils;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.TaskContainer;
import cz.tul.dic.data.TaskParameter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class VideoLoader implements IInputLoader {

    private static final String SCRIPT_NAME = "scriptVideoSplit.vcf";
    private static final String SCRIPT_FILE = "%FILE%";
    private static final String SCRIPT_DIR = "%DIR%";
    private static final File VIRTUAL_DUB = new File("virtualDub\\VirtualDub.exe");

    @Override
    public List<Image> loadData(Object in, TaskContainer tc) throws IOException {
        if (!(in instanceof File)) {
            throw new IllegalArgumentException("VideoLoader needs a single file as input.");
        }
        if (!VIRTUAL_DUB.exists()) {
            throw new FileNotFoundException("VirtualDub is not available.");
        }

        final File input = (File) in;
        tc.addParameter(TaskParameter.DIR, input.getParentFile());
        // create temp dir to store images
        final File temp = Utils.getTempDir(tc);        
        // prepare script
        String script = loadScript();        
        script = script.replace(SCRIPT_FILE, input.getAbsolutePath());
        script = script.replace(SCRIPT_DIR, temp.getAbsolutePath().concat(File.separator).concat(input.getName()));
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
                throw new IOException("Video splitting has failed.");
            }
        } catch (InterruptedException ex) {
            throw new IOException("VirtualDub has been interrupted.", ex);
        }
        // list of all bmp files inside temp dir with roght name
        final File[] files = temp.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(input.getName());
            }
        });
        final ImageLoader il = new ImageLoader();
        final List<Image> result = il.loadData(Arrays.asList(files), tc);
        
        Utils.deleteTempFir(tc);
        
        return result;
    }

    private String loadScript() throws IOException {
        InputStream in = VideoLoader.class.getResourceAsStream(SCRIPT_NAME);
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
    public Class getSupporteType() {
        return File.class;
    }

}
