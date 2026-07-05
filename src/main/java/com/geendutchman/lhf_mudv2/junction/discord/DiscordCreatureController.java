package com.geendutchman.lhf_mudv2.junction.discord;

import java.util.function.BiFunction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.Examinable.Name;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.Event.CreatureChangedEvent;
import com.geendutchman.lhf_mudv2.execution.Event.CreatureSeenEvent;
import com.geendutchman.lhf_mudv2.execution.Event.InventoryEvent;
import com.geendutchman.lhf_mudv2.execution.Event.ItemChangedEvent;
import com.geendutchman.lhf_mudv2.execution.Event.ItemSeenEvent;
import com.geendutchman.lhf_mudv2.execution.Event.PlainEvent;
import com.geendutchman.lhf_mudv2.execution.Event.RoomChangedEvent;
import com.geendutchman.lhf_mudv2.execution.Event.RoomSeenEvent;
import com.geendutchman.lhf_mudv2.execution.Event.SpokenEvent;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.controllers.CreatureController;

import net.dv8tion.jda.api.entities.User;
import picocli.CommandLine;

@Component
public class DiscordCreatureController extends CreatureController {
    private final static Examinable.Name NAME = new Examinable.Name("Discord Creature Controller");
    @Autowired
    private final DiscordApi api;

    DiscordCreatureController(MessageBus bus, BiFunction<MessageBus, MessageContext, CommandLine> generator,
            DiscordApi discord) {
        super(bus, generator);
        this.api = discord;
    }

    @Override
    public void register() {
        this.bus.registerProcessor(this);
    }

    @Override
    protected Name name() {
        return DiscordCreatureController.NAME;
    }

    protected class DiscordBehavior extends CreatureController.ItemizedBehavior {
        protected final User user;

        public DiscordBehavior(Creature creature) {
            super(creature);
            this.user = DiscordCreatureController.this.api.getUser(creature.identifier()).orElse(null);
        }

        protected void forUser(MessageContext context, Event event) {
            if (this.user == null) {
                DiscordCreatureController.this.logger.atWarn().addKeyValue("event", event)
                        .addKeyValue("Creature", creature.identifier()).log("No user found for creature!");
                return;
            }
            this.user.openPrivateChannel().map(channel -> {
                // TODO: some sort of AbstractFactory for message strategies
                return channel.sendMessage(event.toString());
            }).queue(null, fail -> {
                DiscordCreatureController.this.logger.atError().setCause(fail)
                        .addKeyValue("DiscordTag", user.getAsTag()).addKeyValue("event", event)
                        .addKeyValue("Creature", creature.identifier()).log("Failed to send message to user");
            });
        }

        @Override
        public void onCreatureChangedEvent(MessageContext context, CreatureChangedEvent event) {
            this.forUser(context, event);
        }

        @Override
        public void onCreatureSeenEvent(MessageContext context, CreatureSeenEvent event) {
            this.forUser(context, event);
        }

        @Override
        public void onInventoryEvent(MessageContext context, InventoryEvent event) {
            this.forUser(context, event);
        }

        @Override
        public void onItemChangedEvent(MessageContext context, ItemChangedEvent event) {
            this.forUser(context, event);
        }

        @Override
        public void onItemSeenEvent(MessageContext context, ItemSeenEvent event) {
            this.forUser(context, event);
        }

        @Override
        public void onPlainEvent(MessageContext context, PlainEvent event) {
            this.forUser(context, event);
        }

        @Override
        public void onRoomChangedEvent(MessageContext context, RoomChangedEvent event) {
            this.forUser(context, event);
        }

        @Override
        public void onRoomSeenEvent(MessageContext context, RoomSeenEvent event) {
            this.forUser(context, event);
        }

        @Override
        public void onSpokenEvent(MessageContext context, SpokenEvent event) {
            this.forUser(context, event);
        }

        @Override
        public MessageProcessingResult process(MessageContext context, Event event) {
            this.forUser(context, event);
            return MessageProcessingResult.HANDLED;
        }

    }

    @Override
    protected DiscordBehavior itemizedEventBehavior(Creature creature) {
        return new DiscordBehavior(creature);
    }
}