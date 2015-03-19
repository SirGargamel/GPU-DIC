/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic;

import org.pmw.tinylog.LoggingLevel;
import org.pmw.tinylog.writers.LoggingWriter;

/**
 *
 * @author Petr Ječmen
 */
public class MultiWriter implements LoggingWriter {

    private final LoggingWriter[] writers;

    public MultiWriter(LoggingWriter... writers) {
        this.writers = writers;
    }

    @Override
    public void write(LoggingLevel level, String logEntry) {
        for (LoggingWriter writer : writers) {
            writer.write(level, logEntry);
        }
    }

    @Override
    public void init() {
        for (LoggingWriter lw : writers) {
            lw.init();
        }
    }
}
