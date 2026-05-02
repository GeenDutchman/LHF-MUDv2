package com.geendutchman.lhf_mudv2.junction;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentSkipListMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.execution.MessageBus;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

@Service
public class DiscordJunction extends ListenerAdapter {
    private JDA api;
    private final ConcurrentSkipListMap<String, IEntityID> userToController;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private final MessageBus bus;

    public DiscordJunction(@Autowired MessageBus messageBus) throws Exception {
        ClassLoader classLoader = this.getClass().getClassLoader();
        this.userToController = new ConcurrentSkipListMap<>();
        this.bus = messageBus;

        try (InputStream inputStream = classLoader.getResourceAsStream("discord/token.secret")) {
            BufferedReader buf = new BufferedReader(new InputStreamReader(inputStream));
            final String token = buf.readLine();
            logger.atInfo().log("Building JDA...");
            this.api = JDABuilder.createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES).build();
            logger.atInfo().log("Adding event listener...");
            this.api.addEventListener(this);
            logger.atInfo().log("Waiting until ready...");
            this.api.awaitReady();
            logger.atInfo().log("Ready!!");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                this.logger.info("Shutting down");
                this.api.shutdown();
                this.logger.info("Post shut down");
            }, "JDA Shutdown"));
        } catch (Exception e) {
            logger.atError().setCause(e).log("encountered error setting up");
            throw e;
        }
    }

    public Status status() {
        return this.api.getStatus();
    }

    @Override
    public void onMessageReceived(final MessageReceivedEvent event) {
        if (event == null || event.getAuthor().isBot())
            return;
        final Message message = event.getMessage();
        final User author = message.getAuthor();
        final String content = message.getContentRaw();
        if (!content.startsWith("mud") && !content.startsWith("lhf")) {
            // we only care about mud/lhf
            return;
        }

        final IEntityID myEntity = this.userToController.getOrDefault(author.getAsTag(), null);
        if (myEntity == null) {
            this.logger.atWarn().addKeyValue("author", author).addKeyValue("message", content)
                    .log("missing controller");
            author.openPrivateChannel().flatMap(pc -> pc.sendMessage("You do not have anything set up yet")).queue(null,
                    failSend -> {
                        this.logger.atError().setCause(failSend).addKeyValue("author", author)
                                .log("Could not send failure message");
                    });
            return;
        }
        author.openPrivateChannel().flatMap(pc -> pc.sendMessage("Well, you seem to have made it")).queue(null,
                failSend -> {
                    this.logger.atError().setCause(failSend).addKeyValue("author", author)
                            .addKeyValue("entity", myEntity).log("failed to send DM");
                });

        // try (PrintWriter writer = new PrintWriter(
        // new TwinWriter(new DiscordPrintWriter(event.getChannel()), new
        // LogWriter(logger)))) {
        // CommandSpec spec = CommandSpec.create()
        // .addSubcommand("ping", new
        // CommandHandler.PingCommandHandler().createCommandLine(null, null))
        // .mixinStandardHelpOptions(true);
        // CommandLine cli = new CommandLine(spec).setOut(writer).setErr(writer);
        // cli.execute(args);

        // } catch (Exception e) {
        // logger.atError().setCause(e).addKeyValue("content",
        // content).addKeyValue("parsed", args)
        // .log("error processing");
        // // throw e;
        // }

    }

}
