/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.pmw.tinylog.Logger;

/**
 *
 * @author Petr Jecmen
 * @param <F>
 */
public class CsvOutput<F> {

    private static final String SEPARATOR_VALUE = ",";
    private static final String SEPARATOR_LINE = "\n";
    private final Map<String, Map<String, F>> data;

    public CsvOutput() throws IOException {
        data = new LinkedHashMap<>();
    }

    public void addValue(final String key1, final String key2, final F value) {
        Map<String, F> m = data.get(key1);
        if (m == null) {
            m = new LinkedHashMap<>();
            data.put(key1, m);
        }
        m.put(key2, value);
    }

    public void writeData(final File out) throws IOException {
        if (!out.exists()) {
            out.createNewFile();
        }
        try (final BufferedWriter output = new BufferedWriter(new FileWriter(out))) {
            output.write(" ");
            for (String s : data.get(data.keySet().iterator().next()).keySet()) {
                output.write(SEPARATOR_VALUE);
                writeValue(output, s);
            }
            output.write(SEPARATOR_LINE);
            
            for (Entry<String, Map<String, F>> e : data.entrySet()) {
                writeValue(output, e.getKey());

                for (F val : e.getValue().values()) {
                    output.write(SEPARATOR_VALUE);
                    writeValue(output, val.toString());
                }

                output.write(SEPARATOR_LINE);
            }
        } catch (IOException ex) {
            Logger.error(ex);
        }
    }

    private void writeValue(final BufferedWriter output, final String val) throws IOException {
        output.write("\"");
        output.write(val);
        output.write("\"");
    }

}
