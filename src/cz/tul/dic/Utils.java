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
import cz.tul.pj.journal.Journal;
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
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 */
public final class Utils {

    private static final String TEMP_DIR_NAME = "temp";
    private static final Map<TaskContainer, List<File>> TEMP_FILES;
    private static final NumberFormat NF_DOUBLE, NF_INT;

    static {
        TEMP_FILES = new HashMap<>();

        final DecimalFormatSymbols decimalSymbol = new DecimalFormatSymbols(Locale.getDefault());
        decimalSymbol.setDecimalSeparator('.');
        NF_DOUBLE = new DecimalFormat("#0.00#", decimalSymbol);
        NF_INT = new DecimalFormat("00", decimalSymbol);
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
        List<File> f = TEMP_FILES.get(tc);
        if (f == null) {
            f = new LinkedList<>();
            TEMP_FILES.put(tc, f);
        }
        f.addAll(Arrays.asList(filesForDeletion));
    }

    public static void deleteTempDir(final TaskContainer tc) {
        final File in = (File) (tc.getParameter(TaskParameter.IN));
        final String tempPath = in.getParent().concat(File.separator).concat(TEMP_DIR_NAME);
        final File temp = new File(tempPath);
        if (temp.exists()) {
            List<File> list = TEMP_FILES.get(tc);
            for (File f : list) {
                if (!f.delete()) {
                    Logger.warn("Error deleting {}", f);
                }
            }
            list.clear();
            if (temp.listFiles().length == 0 && !temp.delete()) {
                Logger.warn("Error deleting {}", temp);
            }
        }
    }

    public static void ensureDirectoryExistence(final File file) {
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static String format(final double val) {
        return NF_DOUBLE.format(val);
    }

    public static String format(final int val) {
        return NF_INT.format(val);
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
            Journal.getInstance().addDataEntry(ex, "Error checking the license file {0}.", licenseFile.getAbsolutePath());
        }
        return result;
    }
}
