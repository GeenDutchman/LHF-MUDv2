package com.geendutchman.lhf_mudv2.junction;

import java.io.IOException;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.event.Level;

public class LogWriter extends StringWriter {
    private final Logger logger;
    private Level level = Level.INFO;

    public LogWriter(Logger logger) {
        this.logger = logger;
    }

    public LogWriter(Logger logger, Level level) {
        this.logger = logger;
        this.level = level;
    }

    @Override
    public void flush() {
        synchronized (this.lock) {
            final String asString = this.toString();
            if (this.logger != null && asString != null && asString.length() > 0) {
                logger.atLevel(this.level).setMessage(asString).log();
            }
            this.getBuffer().setLength(0);
        }
    }

    @Override
    public void close() throws IOException {
        this.flush();
        super.close();
    }
}