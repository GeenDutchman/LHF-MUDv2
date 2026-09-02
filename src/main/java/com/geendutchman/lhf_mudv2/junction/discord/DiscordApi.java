package com.geendutchman.lhf_mudv2.junction.discord;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.GatewayIntent;

@Service
public class DiscordApi {

    private JDA api;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DiscordJunction junction;

    public DiscordApi(@Autowired DiscordJunction junction) throws Exception {
        this.junction = junction;
        ClassLoader classLoader = this.getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream("discord/token.secret")) {
            BufferedReader buf = new BufferedReader(new InputStreamReader(inputStream));
            final String token = buf.readLine();
            logger.atInfo().log("Building JDA...");
            this.api = JDABuilder.createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES).build();
            logger.atInfo().log("Adding event listener...");
            this.api.addEventListener(this.junction);
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

    public Optional<User> getUser(IEntityID id) {
        DiscordEntityRepository repo = this.junction.repository();
        if (repo == null) {
            return Optional.empty();
        }
        return repo.otherId(id).map(l -> this.api.getUserById(l));
    }

    public Optional<User> getUser(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.api.getUserById(id));
    }

}
