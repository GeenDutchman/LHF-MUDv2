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
@Command(name = "inventory", description = "Lets you check on your inventory", subcommands = { HelpCommand.class })
public final class InventoryCommand extends SwitchedHandler {

    public InventoryCommand(MessageBus messbus, MessageContext context) {
        super(messbus, context);
    }

    @Override
    protected void onItem(Item item) {
        // items generally don't have an inventory
    }

    @Override
    protected void onCreature(Creature creature) {
        bus.publish(
                MessageContext.builder().setSender(creature.identifier()).setDestination(creature.creatureID()).build(),
                Event.InventoryEvent.ofCreature(creature));
    }

    @Override
    protected void onRoom(Room room) {
        // room cannot handle inventory command
    }

    @Override
    protected void unrecognized() {
        // this really can't handle inventory command
    }
}
