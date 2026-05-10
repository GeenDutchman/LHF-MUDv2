package com.geendutchman.lhf_mudv2.execution.commandline;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

@Component
@Scope("prototype")
@Command(name = "status", description = "Lets you check on yourself", subcommands = { HelpCommand.class })
public final class StatusCommand extends SwitchedHandler {
    protected StatusCommand(MessageBus messbus, MessageContext context) {
        super(messbus, context);
    }

    @Override
    public void run() {
        this.contextSplit();
    }

    @Override
    protected void onCreature(Creature creature) {
        this.bus.publish(MessageContext.builder().setSenderId(creature.identifier())
                .setDestinationId(creature.creatureID()).build(), new Event.CreatureSeenEvent(creature));
    }

    @Override
    protected void onItem(Item item) {
        bus.publish(MessageContext.builder().setSenderId(item.identifier()).setDestinationId(item.itemID()).build(),
                new Event.ItemSeenEvent(item));
    }

    @Override
    protected void onRoom(Room room) {
        bus.publish(MessageContext.builder().setSenderId(room.roomID()).setDestinationId(room.roomID()).build(),
                new Event.RoomSeenEvent(room, null, null));
    }

    @Override
    protected void unrecognized() {
        // does nothing
    }
}
