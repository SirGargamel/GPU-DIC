package cz.tul.dic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Jecmen
 */
public class CsvOutput {

    private static final String SEPARATOR_VALUE = ",";
    private static final String SEPARATOR_LINE = "\n";
    private static final Map<Integer, Map<Integer, CsvOutput>> outputters;
    private static final NumberFormat nf;
    private final BufferedWriter out;
    
    static {
        outputters = new HashMap<>();
        nf = new DecimalFormat("000");
    }
    
    public static void addValue(final int round, final int x, final int y, final double[] value) {
        Map<Integer, CsvOutput> m = outputters.get(x);
        if (m == null) {
            m = new HashMap<>();
            outputters.put(x, m);
        }
        
        CsvOutput out = m.get(y);
        if (out == null) {
            try {
                out = new CsvOutput(new File("D:\\temp\\.cluster\\".concat(nf.format(round)).concat("_").concat(nf.format(x)).concat("_").concat(nf.format(y)).concat(".csv")));
            } catch (IOException ex) {
                Logger.getLogger(CsvOutput.class.getName()).log(Level.SEVERE, null, ex);
            }
            m.put(y, out);
        }
        out.addLine(value);
    }
    
    public static void closeSession() {
        outputters.values().stream().forEach((m) -> {
            m.values().stream().forEach((out) -> {
                out.closeWriter();
            });
        });
        outputters.clear();
    }

    private CsvOutput(final File target) throws IOException {
        if (!target.exists()) {
            target.createNewFile();
        }
        out = new BufferedWriter(new FileWriter(target));
    }

    void addLine(final String line) {
        try {
            out.write(line);
            out.write(SEPARATOR_LINE);
        } catch (IOException ex) {
            Logger.getLogger(CsvOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void addLine(final double[] value) {
        try {
            for (double d : value) {
                out.write(Double.toString(d));
                out.write(SEPARATOR_VALUE);
            }
            out.write(SEPARATOR_LINE);
        } catch (IOException ex) {
            Logger.getLogger(CsvOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void closeWriter() {
        try {
            out.flush();
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(CsvOutput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
