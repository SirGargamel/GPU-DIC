package cz.tul.dic.input;

import cz.tul.dic.Utils;
import cz.tul.dic.data.Config;
import cz.tul.dic.data.ConfigType;
import cz.tul.dic.data.Image;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import org.pmw.tinylog.Logger;

public class VideoLoader implements IInputLoader {

    private static final String PREFIX_SIZE = "SIZE_";
    private static final String PREFIX_MOD = "MOD_";
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
        // create temp dir to store images        
        final File temp = Utils.getTempDir(tc);
        // check cache
        final List<File> files;
        Config config = Config.loadConfig(input.getParentFile(), input.getName(), ConfigType.SEQUENCE);
        if (!isCacheDataValid(input, temp, config)) {
            Logger.debug("Cache data for file {0} invalid, using VirtualDub.", input.getAbsolutePath());
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
            files = Arrays.asList(temp.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith(input.getName());
                }
            }));

            // save config
            config = new Config();
            config.put(PREFIX_MOD.concat(input.getName()), Long.toString(input.lastModified()));
            config.put(PREFIX_SIZE.concat(input.getName()), Long.toString(input.length()));
            for (File f : files) {
                config.put(PREFIX_MOD.concat(f.getName()), Long.toString(f.lastModified()));
                config.put(PREFIX_SIZE.concat(f.getName()), Long.toString(f.length()));
            }
            Config.saveConfig(input.getParentFile(), input.getName(), ConfigType.SEQUENCE, config);
        } else {
            files = convertCacheDataToFiles(input, temp, config);
        }
        // list of all bmp files inside temp dir with roght name        
        final ImageLoader il = new ImageLoader();
        final List<Image> result = il.loadData(files, tc);

        tc.setParameter(TaskParameter.DIR, input.getParentFile());
        tc.setParameter(TaskParameter.NAME, input.getName());

        return result;
    }

    private boolean isCacheDataValid(final File source, final File tempFolder, final Config config) {
        boolean result = true;

        if (!tempFolder.isDirectory() || config == null || config.isEmpty()) {
            result = false;
        } else {
            final String baseTempPath = tempFolder.getAbsolutePath().concat(File.separator);
            File tempFile;
            String key, fileName;
            long valueConfig, valueFile;
            for (Entry<String, String> e : config.entrySet()) {
                key = e.getKey();
                if (key.startsWith(PREFIX_SIZE)) {
                    fileName = key.replaceFirst(PREFIX_SIZE, "");
                    if (fileName.equals(source.getName())) {
                        valueFile = source.length();
                        valueConfig = Long.parseLong(e.getValue());
                        if (valueFile != valueConfig) {
                            result = false;
                            break;
                        }
                    } else {
                        tempFile = new File(baseTempPath.concat(fileName));
                        if (!tempFile.exists()) {
                            result = false;
                            break;
                        }
                        valueFile = tempFile.length();
                        valueConfig = Long.parseLong(e.getValue());
                        if (valueFile != valueConfig) {
                            result = false;
                            break;
                        }
                    }
                } else {
                    fileName = key.replaceFirst(PREFIX_MOD, "");
                    if (fileName.equals(source.getName())) {
                        valueFile = source.lastModified();
                        valueConfig = Long.parseLong(e.getValue());
                        if (valueFile != valueConfig) {
                            result = false;
                            break;
                        }
                    } else {
                        tempFile = new File(baseTempPath.concat(fileName));
                        if (!tempFile.exists()) {
                            result = false;
                            break;
                        }
                        valueFile = tempFile.lastModified();
                        valueConfig = Long.parseLong(e.getValue());
                        if (valueFile != valueConfig) {
                            result = false;
                            break;
                        }
                    }
                }
            }
        }

        return result;
    }

    private List<File> convertCacheDataToFiles(final File source, final File tempFolder, final Config config) {
        final String baseTempPath = tempFolder.getAbsolutePath().concat(File.separator);
        File tempFile;
        final List<File> result = new LinkedList<>();
        String key, fileName;
        for (Entry<String, String> e : config.entrySet()) {
            key = e.getKey();
            if (key.startsWith(PREFIX_SIZE)) {
                fileName = key.replaceFirst(PREFIX_SIZE, "");
                if (!fileName.equals(source.getName())) {
                    tempFile = new File(baseTempPath.concat(fileName));
                    result.add(tempFile);
                }
            }
        }
        Logger.debug("Found valid cache data for file {0}, importing {1} images.", source.getAbsolutePath(), result.size());
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
