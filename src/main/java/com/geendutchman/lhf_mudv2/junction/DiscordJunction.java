package com.geendutchman.lhf_mudv2.junction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import com.geendutchman.lhf_mudv2.execution.CommandHandler;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

public class DiscordJunction {
    private JDA api;

    public DiscordJunction() throws Exception {
        ClassLoader classLoader = this.getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream("discord/token.secret")) {
            BufferedReader buf = new BufferedReader(new InputStreamReader(inputStream));
            final String token = buf.readLine();
            this.api = JDABuilder.createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES).build();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            throw e;
        }
    }

    public void run() {
        this.api.addEventListener(new SoulJunction());
    }

    public static class Outy extends PrintWriter {

        public Outy(MessageChannel channel) {
            super(new BufferedWriter(new DiscordWriter(channel), 80), true);
        }

    }

    private static class DiscordWriter extends Writer {
        private final MessageChannel channel;

        public DiscordWriter(MessageChannel channel) {
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

    public static class SoulJunction extends ListenerAdapter {

        private final static Pattern splitter = Pattern.compile("\"((?:\"|[^\"])*?)\"|([^ ]+)");

        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (event == null || event.getAuthor().isBot())
                return;
            Message message = event.getMessage();
            String content = message.getContentRaw();

            try (PrintWriter writer = new Outy(event.getChannel())) {
                CommandLine cli = CommandSpec.create()
                        .addSubcommand("ping", new CommandHandler.PingCommandHandler().createCommandLine(null, null))
                        .mixinStandardHelpOptions(true).commandLine().setOut(writer).setErr(writer);
                Matcher splitten = splitter.matcher(content);
                ArrayList<String> args = new ArrayList<>();
                while (splitten.find()) {
                    if (splitten.group(1) != null) {
                        args.add(splitten.group(1));
                    } else {
                        args.add(splitten.group(1));
                    }
                }
                cli.execute(args.toArray(new String[0]));

            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).atError().setCause(e).addKeyValue("content", content)
                        .log("error processing");
                throw e;
            }

        }
    }

}
