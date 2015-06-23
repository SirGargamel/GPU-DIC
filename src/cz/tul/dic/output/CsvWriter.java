/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.output;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author Petr Ječmen
 */
public final class CsvWriter {

    private static final String SEPARATOR_VALUE = ",";
    private static final String SEPARATOR_LINE = "\n";

    private CsvWriter() {
    }

    public static void writeDataToCsv(final File target, final String[][] data) throws IOException {
        try (FileWriter out = new FileWriter(target)) {
            for (String[] sa : data) {
                for (String s : sa) {
                    out.append(s);
                    out.append(SEPARATOR_VALUE);
                }
                out.append(SEPARATOR_LINE);
            }
        }
    }

}
