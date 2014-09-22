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
