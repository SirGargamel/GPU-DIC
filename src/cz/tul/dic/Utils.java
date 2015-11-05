/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import cz.tul.dic.data.task.TaskContainer;
import cz.tul.dic.data.task.TaskParameter;
import cz.tul.dic.license.License;
import cz.tul.dic.license.LicenseHandler;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public final class Utils {

    private static final String TEMP_DIR_NAME = "temp";
    private static final Map<TaskContainer, List<File>> tempFiles;

    private static final NumberFormat nfDouble, nfInt;

    static {
        tempFiles = new HashMap<>();

        final DecimalFormatSymbols decimalSymbol = new DecimalFormatSymbols(Locale.getDefault());
        decimalSymbol.setDecimalSeparator('.');
        nfDouble = new DecimalFormat("#0.00#", decimalSymbol);
        nfInt = new DecimalFormat("00", decimalSymbol);
    }

    private Utils() {
    }

    public static File getTempDir(final File in) {        
        final String tempPath = in.getParent().concat(File.separator).concat(TEMP_DIR_NAME);
        final File temp = new File(tempPath);
        ensureDirectoryExistence(temp);

        return temp;
    }

    public static void markTempFilesForDeletion(final TaskContainer tc, final File... filesForDeletion) {
        List<File> f = tempFiles.get(tc);
        if (f == null) {
            f = new LinkedList<>();
            tempFiles.put(tc, f);
        }
        f.addAll(Arrays.asList(filesForDeletion));
    }

    public static void deleteTempDir(final TaskContainer tc) {
        final File in = (File) (tc.getParameter(TaskParameter.IN));
        final String tempPath = in.getParent().concat(File.separator).concat(TEMP_DIR_NAME);
        final File temp = new File(tempPath);
        if (temp.exists()) {
            Logger.trace("Deleting files inside temp folder {0}.", temp.getAbsolutePath());

            List<File> list = tempFiles.get(tc);
            for (File f : list) {
                if (!f.delete()) {
                    Logger.warn("Error deleting " + f.getAbsolutePath());
                }
            }
            list.clear();
            if (temp.listFiles().length == 0 && !temp.delete()) {
                Logger.warn("Error deleting " + temp.getAbsolutePath());
            }
        }
    }

    public static void ensureDirectoryExistence(final File file) {
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static boolean isLevelLogged(final Level testedLevel) {
        final Level currentLevel = Logger.getLevel();
        int indexTestedLevel = 0, indexCurrentLevel = 1;
        final Level[] levels = Level.values();
        for (int l = 0; l < levels.length; l++) {
            if (levels[l].equals(testedLevel)) {
                indexTestedLevel = l;
            }
            if (levels[l].equals(currentLevel)) {
                indexCurrentLevel = l;
            }
        }
        return indexCurrentLevel < indexTestedLevel;
    }

    public static String format(final double val) {
        return nfDouble.format(val);
    }

    public static String format(final int val) {
        return nfInt.format(val);
    }

    public static double[][] generateNaNarray(final int width, final int height) {
        final double[][] result = new double[width][height];
        for (double[] dA : result) {
            Arrays.fill(dA, Double.NaN);
        }
        return result;
    }

    public static boolean checkLicense(final File licenseFile) {
        boolean result = false;
        try {
            final License license = LicenseHandler.readLicenseFile(licenseFile);
            result = LicenseHandler.checkLicense(license);
        } catch (IOException ex) {
            Logger.error(ex);
        }
        return result;
    }
}
