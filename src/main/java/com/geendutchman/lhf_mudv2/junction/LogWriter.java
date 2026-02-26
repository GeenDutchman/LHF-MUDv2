package com.geendutchman.lhf_mudv2.junction;

import java.io.IOException;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.event.Level;

public class LogWriter extends Writer {
    private final Logger logger;
    private Level level = Level.INFO;

    public LogWriter(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (this.logger != null) {
            String string = new String(cbuf, off, len);
            logger.atLevel(this.level).setMessage(string).log();
        }
    }

    @Override
    public void flush() throws IOException {
        // does nothing
    }

    @Override
    public void close() throws IOException {
        // does nothing
    }
}