/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data.task.loaders;

import cz.tul.dic.ComputationException;
import cz.tul.dic.ComputationExceptionCause;
import cz.tul.dic.Utils;
import cz.tul.dic.data.config.Config;
import cz.tul.dic.data.config.ConfigType;
import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.output.NameGenerator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import org.pmw.tinylog.Logger;

public class VideoLoader extends AbstractInputLoader {

    private static final String SUPPORTED_TYPES = "avi";
    private static final String PREFIX_SIZE = "SIZE_";
    private static final String PREFIX_MOD = "MOD_";
    private static final String SCRIPT_NAME = "scriptVideoSplit.vcf";
    private static final String SCRIPT_FILE = "%FILE%";
    private static final String SCRIPT_DIR = "%DIR%";
    private static final File VIRTUAL_DUB = new File("virtualDub\\VirtualDub.exe");

    @Override
    public TaskContainer loadTask(final Object in, final TaskContainer task) throws ComputationException {
        if (!(in instanceof File)) {
            throw new IllegalArgumentException("VideoLoader needs a single file as input.");
        }
        if (!VIRTUAL_DUB.exists()) {
            throw new ComputationException(ComputationExceptionCause.IO, new FileNotFoundException("VirtualDub is not available."));
        }

        File input = (File) in;
        if (!input.exists()) {
            throw new ComputationException(ComputationExceptionCause.ILLEGAL_TASK_DATA, "Input file " + in.toString() + " not found.");
        }

        final File temp = Utils.getTempDir(input);
        final File sequenceConfigFile = new File(temp.getAbsolutePath().concat(File.separator).concat(input.getName()).concat(NameGenerator.EXT_CONFIG));
        // check cache
        final List<File> files;
        try {
            if (sequenceConfigFile.exists()) {
                final Config config = new Config().load(sequenceConfigFile);
                if (!isCacheDataValid(input, temp, config)) {
                    files = loadVideoByVirtualDub(input, temp, sequenceConfigFile);
                } else {
                    files = convertCacheDataToFiles(input, temp, config);
                }
            } else {
                files = loadVideoByVirtualDub(input, temp, sequenceConfigFile);
            }

            // list of all bmp files inside temp dir with roght name        
            final ImageLoader il = new ImageLoader();
            task.setParameter(TaskParameter.IN, in);
            final TaskContainer result = il.loadTask(files, task);
            loadUdaFile(input.getAbsolutePath(), result);

            return result;
        } catch (IOException ex) {
            Logger.error(ex, "Error loading sequence config file.");
            return null;
        }
    }

    private List<File> loadVideoByVirtualDub(File input, final File temp, final File sequenceConfigFile) throws IOException {
        Logger.trace("Cache data for file {0} invalid, using VirtualDub.", input.getAbsolutePath());
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
        final String inputName = input.getName();
        final List<File> files = Arrays.asList(temp.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(inputName) && !name.endsWith(NameGenerator.EXT_CONFIG);
            }
        }));
        // save config
        final Config config = new Config();
        config.put(PREFIX_MOD.concat(input.getName()), Long.toString(input.lastModified()));
        config.put(PREFIX_SIZE.concat(input.getName()), Long.toString(input.length()));
        for (File f : files) {
            config.put(PREFIX_MOD.concat(f.getName()), Long.toString(f.lastModified()));
            config.put(PREFIX_SIZE.concat(f.getName()), Long.toString(f.length()));
        }
        config.setType(ConfigType.SEQUENCE);
        config.save(sequenceConfigFile);
        return files;
    }

    private boolean isCacheDataValid(final File source, final File tempFolder, final Config config) {
        boolean result = true;

        if (!tempFolder.isDirectory() || config == null || config.keySet().isEmpty() || !config.getType().equals(ConfigType.SEQUENCE)) {
            result = false;
        } else {
            final String baseTempPath = tempFolder.getAbsolutePath().concat(File.separator);
            File tempFile;
            String fileName;
            long valueConfig, valueFile;
            for (String key : config.keySet()) {
                if (key.startsWith(PREFIX_SIZE)) {
                    fileName = key.replaceFirst(PREFIX_SIZE, "");
                    if (fileName.equals(source.getName())) {
                        valueFile = source.length();
                        valueConfig = Long.parseLong(config.get(key));
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
                        valueConfig = Long.parseLong(config.get(key));
                        if (valueFile != valueConfig) {
                            result = false;
                            break;
                        }
                    }
                } else if (key.startsWith(PREFIX_MOD)) {
                    fileName = key.replaceFirst(PREFIX_MOD, "");
                    if (fileName.equals(source.getName())) {
                        valueFile = source.lastModified();
                        valueConfig = Long.parseLong(config.get(key));
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
                        valueConfig = Long.parseLong(config.get(key));
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
        String fileName;
        final List<String> files = new ArrayList<>(config.keySet());
        Collections.sort(files);
        for (String key : files) {
            if (key.startsWith(PREFIX_SIZE)) {
                fileName = key.replaceFirst(PREFIX_SIZE, "");
                if (!fileName.equals(source.getName())) {
                    tempFile = new File(baseTempPath.concat(fileName));
                    result.add(tempFile);
                }
            }
        }
        Logger.trace("Found valid cache data for file {0}, importing {1} images.", source.getAbsolutePath(), result.size());
        return result;
    }

    private String loadScript() throws IOException {
        InputStream in = VideoLoader.class.getResourceAsStream(SCRIPT_NAME);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader bin = new BufferedReader(new InputStreamReader(in))) {
            while (bin.ready()) {
                sb.append(bin.readLine());
                sb.append("\n");
            }
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
    public boolean canLoad(Object in) {
        boolean result = false;
        if (in instanceof File) {
            final File input = (File) in;
            final String ext = input.getName().substring(input.getName().lastIndexOf('.') + 1).toLowerCase(Locale.getDefault());
            result = SUPPORTED_TYPES.contains(ext);
        }
        return result;
    }

}
