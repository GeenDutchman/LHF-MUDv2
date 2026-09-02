package com.geendutchman.lhf_mudv2.junction.common;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TwinWriter extends Writer {
    private static final class TwinIOExtension extends IOException {
        private List<IOException> exceptions = new ArrayList<>();

        protected TwinIOExtension(Collection<IOException> problems) {
            super();
            this.exceptions = List.copyOf(problems);
        }

        @Override
        public String getMessage() {
            StringBuilder message = new StringBuilder("Errors occured:");
            for (IOException ioException : exceptions) {
                message.append("\n").append(ioException.getMessage());
            }
            return message.toString();
        }

    }

    private final ArrayList<Writer> subs;

    public TwinWriter(Writer one, Writer... others) {
        this.subs = new ArrayList<>();
        if (one != null) {
            subs.add(one);
        }
        for (Writer writer : others) {
            if (writer == null) {
                continue;
            }
            subs.add(writer);
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        ArrayList<IOException> exceptions = new ArrayList<>();
        for (Writer writer : this.subs) {
            if (writer == null) {
                continue;
            }
            try {
                writer.write(cbuf, off, len);
            } catch (IOException e) {
                exceptions.add(e);
            }
        }
        if (exceptions.size() > 0) {
            throw new TwinIOExtension(exceptions);
        }
    }

    @Override
    public void flush() throws IOException {
        ArrayList<IOException> exceptions = new ArrayList<>();
        for (Writer writer : this.subs) {
            if (writer == null) {
                continue;
            }
            try {
                writer.flush();
            } catch (IOException e) {
                exceptions.add(e);
            }
        }
        if (exceptions.size() > 0) {
            throw new TwinIOExtension(exceptions);
        }
    }

    @Override
    public void close() throws IOException {
        ArrayList<IOException> exceptions = new ArrayList<>();
        for (Writer writer : this.subs) {
            if (writer == null) {
                continue;
            }
            try {
                writer.close();
            } catch (IOException e) {
                exceptions.add(e);
            }
        }
        if (exceptions.size() > 0) {
            throw new TwinIOExtension(exceptions);
        }
    }
}