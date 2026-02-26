package com.geendutchman.lhf_mudv2.junction;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geendutchman.lhf_mudv2.execution.CommandHandler;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
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

    public static class SoulJunction extends ListenerAdapter {

        private final static Pattern splitter = Pattern.compile("\"((?:\"|[^\"])*?)\"|([^ ]+)");

        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (event == null || event.getAuthor().isBot())
                return;
            Message message = event.getMessage();
            String content = message.getContentRaw();
            Logger logger = LoggerFactory.getLogger(getClass());
            Matcher splitten = splitter.matcher(content);
            ArrayList<String> argsAL = new ArrayList<>();
            while (splitten.find()) {
                if (splitten.group(1) != null) {
                    argsAL.add(splitten.group(1));
                } else {
                    argsAL.add(splitten.group(2));
                }
            }
            String[] args = argsAL.toArray(new String[0]);

            try (PrintWriter writer = new PrintWriter(
                    new TwinWriter(new DiscordPrintWriter(event.getChannel()), new LogWriter(logger)))) {
                CommandSpec spec = CommandSpec.create()
                        .addSubcommand("ping", new CommandHandler.PingCommandHandler().createCommandLine(null, null))
                        .mixinStandardHelpOptions(true);
                CommandLine cli = new CommandLine(spec).setOut(writer).setErr(writer);
                cli.execute(args);

            } catch (Exception e) {
                logger.atError().setCause(e).addKeyValue("content", content).addKeyValue("parsed", args)
                        .log("error processing");
                // throw e;
            }

        }
    }

}
