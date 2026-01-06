package com.geendutchman.lhf_mudv2.execution.controllers;

import org.slf4j.Logger;
import org.springframework.boot.test.context.TestComponent;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.Examinable.Name;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureRepository;
import com.geendutchman.lhf_mudv2.execution.Event.CreatureChangedEvent;
import com.geendutchman.lhf_mudv2.execution.Event.CreatureSeenEvent;
import com.geendutchman.lhf_mudv2.execution.Event.InventoryEvent;
import com.geendutchman.lhf_mudv2.execution.Event.ItemChangedEvent;
import com.geendutchman.lhf_mudv2.execution.Event.ItemSeenEvent;
import com.geendutchman.lhf_mudv2.execution.Event.PlainEvent;
import com.geendutchman.lhf_mudv2.execution.Event.RoomChangedEvent;
import com.geendutchman.lhf_mudv2.execution.Event.RoomSeenEvent;
import com.geendutchman.lhf_mudv2.execution.Event.SpokenEvent;
import com.geendutchman.lhf_mudv2.execution.LHFCommand.ChangeEntityCommand.ChangeCreatureCommand;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.UserCommand;

@TestComponent
public class TestCreatureController extends CreatureController {

    public TestCreatureController(MessageBus bus, CreatureRepository repo) {
        super(bus, repo);
    }

    @Override
    protected Name name() {
        return new Examinable.Name("Test Creature Controller");
    }

    protected final Logger logger() {
        return this.logger;
    }

    @Override
    protected MessageProcessingResult forwardUserCommand(MessageContext context, Creature creature,
            UserCommand userCommand) {
        logger.info(String.format("Forwarding usercommand %s", userCommand));
        return super.forwardUserCommand(context, creature, userCommand);
    }

    @Override
    protected void onCreatureChangedEvent(MessageContext context, CreatureChangedEvent event, Creature creature) {
        logger.info(String.format("Creature changed: %s", event));
        super.onCreatureChangedEvent(context, event, creature);
    }

    @Override
    protected void onCreatureSeenEvent(MessageContext context, CreatureSeenEvent event, Creature creature) {
        logger.info(String.format("Creature seen: %s", event));
        super.onCreatureSeenEvent(context, event, creature);
    }

    @Override
    protected void onInventoryEvent(MessageContext context, InventoryEvent event, Creature creature) {
        logger.info(String.format("Inventory requested: %s", event));
        super.onInventoryEvent(context, event, creature);
    }

    @Override
    protected void onItemChangedEvent(MessageContext context, ItemChangedEvent event, Creature creature) {
        logger.info(String.format("Item changed: %s", event));
        super.onItemChangedEvent(context, event, creature);
    }

    @Override
    protected void onItemSeenEvent(MessageContext context, ItemSeenEvent event, Creature creature) {
        logger.info(String.format("Item seen: %s", event));
        super.onItemSeenEvent(context, event, creature);
    }

    @Override
    protected void onPlainEvent(MessageContext context, PlainEvent event, Creature creature) {
        logger.info(String.format("Plain event: %s", event));
        super.onPlainEvent(context, event, creature);
    }

    @Override
    protected void onRoomChangedEvent(MessageContext context, RoomChangedEvent event, Creature creature) {
        logger.info(String.format("Room changed: %s", event));
        super.onRoomChangedEvent(context, event, creature);
    }

    @Override
    protected void onRoomSeenEvent(MessageContext context, RoomSeenEvent event, Creature creature) {
        logger.info(String.format("Room seen: %s", event));
        super.onRoomSeenEvent(context, event, creature);
    }

    @Override
    protected void onSpokenEvent(MessageContext context, SpokenEvent event, Creature creature) {
        logger.info(String.format("Message spoken: %s", event));
        super.onSpokenEvent(context, event, creature);
    }

    @Override
    protected MessageProcessingResult processChangeCreatureCommand(MessageContext context,
            ChangeCreatureCommand changeCreatureCommand, Creature creature) {
        logger.info("Creature changed command");
        return super.processChangeCreatureCommand(context, changeCreatureCommand, creature);
    }

}