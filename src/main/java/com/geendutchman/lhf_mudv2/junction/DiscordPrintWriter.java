package com.geendutchman.lhf_mudv2.junction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import com.google.common.base.Preconditions;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class DiscordPrintWriter extends PrintWriter {

    private static class DiscordWriter extends Writer {
        private final MessageChannel channel;

        public DiscordWriter(MessageChannel channel) {
            Preconditions.checkNotNull(channel, "channel should not be null");
            this.channel = channel;
        }

        @Override
        public void close() throws IOException {
            // does nothing
        }

        @Override
        public void flush() throws IOException {
            // does nothing
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            final String message = new String(cbuf, off, len);
            channel.sendMessage(message).queue();
        }

    }

    public DiscordPrintWriter(MessageChannel channel) {
        super(new BufferedWriter(new DiscordWriter(channel), 1024), true);
    }

}