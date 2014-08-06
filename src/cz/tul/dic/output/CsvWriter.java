package cz.tul.dic.output;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author Petr Ječmen
 */
public class CsvWriter {

    private static final String SEPARATOR_VALUE = ",";
    private static final String SEPARATOR_LINE = "\n";

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
