/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import java.util.Set;
import org.pmw.tinylog.Configuration;
import org.pmw.tinylog.LogEntry;
import org.pmw.tinylog.writers.LogEntryValue;
import org.pmw.tinylog.writers.Writer;

/**
 *
 * @author Petr Ječmen
 */
public class MultiWriter implements Writer {

    private final Writer[] writers;

    public MultiWriter(Writer... writers) {
        this.writers = writers;
    }

    @Override
    public Set<LogEntryValue> getRequiredLogEntryValues() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void init(Configuration configuration) throws Exception {
        for (Writer writer : writers) {
            writer.init(configuration);
        }
    }

    @Override
    public void write(LogEntry logEntry) throws Exception {
        for (Writer writer : writers) {
            writer.write(logEntry);
        }
    }

    @Override
    public void flush() throws Exception {
        for (Writer writer : writers) {
            writer.flush();
        }
    }

    @Override
    public void close() throws Exception {
        for (Writer writer : writers) {
            writer.close();
        }
    }
}
