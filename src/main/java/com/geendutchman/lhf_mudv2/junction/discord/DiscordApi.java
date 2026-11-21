package com.geendutchman.lhf_mudv2.junction.discord;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private final String ENV_VAR = "LHF_MUD_DISCORD_TOKEN";
    private final String HOME_FILE = ".lhf_mud_discord_token";
    private final String RESOURCE_NAME = "discord/token.secret";

    public DiscordApi(@Autowired DiscordJunction junction) throws Exception {
        this.junction = junction;
        final ClassLoader classLoader = this.getClass().getClassLoader();

        String token = System.getenv(ENV_VAR);
        if (token == null || token.isEmpty()) {
            Path homeDirTokenFile = Path.of(System.getProperty("user.home"), HOME_FILE);
            logger.atInfo()
                    .addKeyValue("env var name", ENV_VAR)
                    .addKeyValue("Home Dir Path", homeDirTokenFile)
                    .log("Token not found in env var, falling back to user's home directory");
            // Check if the file exists in the home directory
            if (homeDirTokenFile != null && Files.exists(homeDirTokenFile)) {
                try (InputStream inputStream = Files.newInputStream(homeDirTokenFile)) {
                    BufferedReader buf = new BufferedReader(new InputStreamReader(inputStream));
                    token = buf.readLine();
                } catch (Exception e) {
                    logger.atWarn().setCause(e).addKeyValue("path", homeDirTokenFile)
                            .log("Could not read from home directory, falling back to resource");
                    token = null;
                }
            }
        } else {
            logger.atDebug().addKeyValue("env var name", ENV_VAR)
                    .log("Using discord token from environment variable");
        }

        // fallback
        if (token == null || token.isEmpty()) {
            try (InputStream inputStream = classLoader.getResourceAsStream(RESOURCE_NAME)) {
                BufferedReader buf = new BufferedReader(new InputStreamReader(inputStream));
                token = buf.readLine();
            } catch (Exception e) {
                logger.atWarn().setCause(e).addKeyValue("resource name", RESOURCE_NAME)
                        .log("Could not read token from resource");
            }
        }

        // final error
        if (token == null || token.isEmpty()) {
            IllegalStateException ise = new IllegalStateException("No Discord token provided");
            logger.atError().setCause(ise)
                    .log("Attempted environment variable, home directory, and resource, but was not found");
            throw ise;
        }

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
