package com.geendutchman.lhf_mudv2.execution.controllers;

import java.util.function.Function;

import org.slf4j.Logger;
import org.springframework.boot.test.context.TestComponent;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.Examinable.Name;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
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

import picocli.CommandLine;

@TestComponent
public class TestCreatureController extends CreatureController {

    @FunctionalInterface
    public interface EventPredicate<E> {
        boolean test(MessageContext mc, Creature c, E e);

    }

    EventPredicate<CreatureChangedEvent> creatureChangedHook;
    EventPredicate<CreatureSeenEvent> creatureSeenHook;
    EventPredicate<InventoryEvent> inventoryHook;
    EventPredicate<ItemChangedEvent> itemChangedHook;
    EventPredicate<ItemSeenEvent> itemSeenHook;
    EventPredicate<PlainEvent> plainEventHook;
    EventPredicate<RoomChangedEvent> roomChangedHook;
    EventPredicate<RoomSeenEvent> roomSeenHook;
    EventPredicate<SpokenEvent> spokenHook;

    public TestCreatureController(MessageBus bus, Function<MessageContext, CommandLine> generator) {
        super(bus, generator);
    }

    @Override
    protected Name name() {
        return new Examinable.Name("Test Creature Controller");
    }

    protected final Logger logger() {
        return this.logger;
    }

    @Override
    protected ItemizedBehavior itemizedEventBehavior(Creature creature) {
        return new TestItemizedBehavior(creature);
    }

    public class TestItemizedBehavior extends CreatureController.ItemizedBehavior {

        public TestItemizedBehavior(Creature creature) {
            super(creature);
        }

        @Override
        public void onCreatureChangedEvent(MessageContext context, CreatureChangedEvent event) {
            logger.info("Creature changed: {}", event);
            if (TestCreatureController.this.creatureChangedHook != null
                    && !TestCreatureController.this.creatureChangedHook.test(context, creature, event)) {
                super.onCreatureChangedEvent(context, event);
            }
        }

        @Override
        public void onCreatureSeenEvent(MessageContext context, CreatureSeenEvent event) {
            logger.info("Creature seen: {}", event);
            if (TestCreatureController.this.creatureSeenHook != null
                    && !TestCreatureController.this.creatureSeenHook.test(context, creature, event)) {
                super.onCreatureSeenEvent(context, event);
            }

        }

        @Override
        public void onInventoryEvent(MessageContext context, InventoryEvent event) {
            logger.info("Inventory requested: {}", event);
            if (TestCreatureController.this.inventoryHook != null
                    && !TestCreatureController.this.inventoryHook.test(context, creature, event)) {
                super.onInventoryEvent(context, event);
            }
        }

        @Override
        public void onItemChangedEvent(MessageContext context, ItemChangedEvent event) {
            logger.info("Item changed: {}", event);
            if (TestCreatureController.this.itemChangedHook != null
                    && !TestCreatureController.this.itemChangedHook.test(context, creature, event)) {
                super.onItemChangedEvent(context, event);
            }
        }

        @Override
        public void onItemSeenEvent(MessageContext context, ItemSeenEvent event) {
            logger.info("Item seen: {}", event);
            if (TestCreatureController.this.itemSeenHook != null
                    && !TestCreatureController.this.itemSeenHook.test(context, creature, event)) {
                super.onItemSeenEvent(context, event);
            }
        }

        @Override
        public void onPlainEvent(MessageContext context, PlainEvent event) {
            logger.atInfo().addKeyValue("description", event.description().printIt())
                    .setMessage(() -> String.format("Plain event: %s", event.description().printIt())).log();
            if (TestCreatureController.this.plainEventHook != null
                    && !TestCreatureController.this.plainEventHook.test(context, creature, event)) {
                super.onPlainEvent(context, event);
            }
        }

        @Override
        public void onRoomChangedEvent(MessageContext context, RoomChangedEvent event) {
            logger.info("Room changed: {}", event);
            if (TestCreatureController.this.roomChangedHook != null
                    && !TestCreatureController.this.roomChangedHook.test(context, creature, event)) {
                super.onRoomChangedEvent(context, event);
            }
        }

        @Override
        public void onRoomSeenEvent(MessageContext context, RoomSeenEvent event) {
            logger.atInfo().addKeyValue("description", event.description().printIt())
                    .log(String.format("Room seen: %s", event));
            if (TestCreatureController.this.roomSeenHook != null
                    && !TestCreatureController.this.roomSeenHook.test(context, creature, event)) {
                super.onRoomSeenEvent(context, event);
            }
        }

        @Override
        public void onSpokenEvent(MessageContext context, SpokenEvent event) {
            logger.atInfo().addKeyValue("message", event.message()).log(String.format("Message spoken: %s", event));
            if (TestCreatureController.this.spokenHook != null
                    && !TestCreatureController.this.spokenHook.test(context, creature, event)) {
                super.onSpokenEvent(context, event);
            }
        }
    }

    @Override
    protected MessageProcessingResult processChangeCreatureCommand(MessageContext context,
            ChangeCreatureCommand changeCreatureCommand, Creature creature) {
        logger.info("Creature changed command");
        return super.processChangeCreatureCommand(context, changeCreatureCommand, creature);
    }

    public void setCreatureChangedHook(EventPredicate<CreatureChangedEvent> creatureChangedHook) {
        this.creatureChangedHook = creatureChangedHook;
    }

    public void setCreatureSeenHook(EventPredicate<CreatureSeenEvent> creatureSeenHook) {
        this.creatureSeenHook = creatureSeenHook;
    }

    public void setInventoryHook(EventPredicate<InventoryEvent> inventoryHook) {
        this.inventoryHook = inventoryHook;
    }

    public void setItemChangedHook(EventPredicate<ItemChangedEvent> itemChangedHook) {
        this.itemChangedHook = itemChangedHook;
    }

    public void setItemSeenHook(EventPredicate<ItemSeenEvent> itemSeenHook) {
        this.itemSeenHook = itemSeenHook;
    }

    public void setPlainEventHook(EventPredicate<PlainEvent> plainEventHook) {
        this.plainEventHook = plainEventHook;
    }

    public void setRoomChangedHook(EventPredicate<RoomChangedEvent> roomChangedHook) {
        this.roomChangedHook = roomChangedHook;
    }

    public void setRoomSeenHook(EventPredicate<RoomSeenEvent> roomSeenHook) {
        this.roomSeenHook = roomSeenHook;
    }

    public void setSpokenHook(EventPredicate<SpokenEvent> spokenHook) {
        this.spokenHook = spokenHook;
    }

}