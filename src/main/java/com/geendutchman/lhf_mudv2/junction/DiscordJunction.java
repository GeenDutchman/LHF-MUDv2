package com.geendutchman.lhf_mudv2.junction;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

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
        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (event == null || event.getAuthor().isBot())
                return;
            Message message = event.getMessage();
            String content = message.getContentRaw();
            if (content.contains("ping")) {
                MessageChannel channel = event.getChannel();
                channel.sendMessage("Soul Pong!").queue();
            }

        }
    }

}
