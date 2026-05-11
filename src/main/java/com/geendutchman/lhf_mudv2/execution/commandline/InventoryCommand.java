package com.geendutchman.lhf_mudv2.execution.commandline;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessingResult;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

@Component
@Scope("prototype")
@Command(name = "inventory", description = "Lets you check on your inventory", subcommands = { HelpCommand.class })
public final class InventoryCommand extends SwitchedHandler {

    public InventoryCommand(MessageBus messbus, MessageContext context) {
        super(messbus, context);
    }

    @Override
    protected MessageProcessingResult onItem(Item item) {
        return MessageProcessingResult.Failed("Items don't generally have an inventory");
    }

    @Override
    protected MessageProcessingResult onCreature(Creature creature) {
        return bus.publish(MessageContext.builder().setSenderId(creature.identifier())
                .setDestinationId(creature.creatureID()).build(), Event.InventoryEvent.ofCreature(creature));
    }

    @Override
    protected MessageProcessingResult onRoom(Room room) {
        return MessageProcessingResult.Failed("Room cannot handle inventory command");
    }

    @Override
    protected MessageProcessingResult unrecognized() {
        return MessageProcessingResult.Failed("Whatever this is *really* cannot handle the inventory command");
    }
}
