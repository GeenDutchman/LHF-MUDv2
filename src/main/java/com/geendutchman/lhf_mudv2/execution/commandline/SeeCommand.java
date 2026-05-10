package com.geendutchman.lhf_mudv2.execution.commandline;

import java.util.Optional;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.entities.creatures.AttributeScores;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.execution.Event.CreatureSeenEvent;
import com.geendutchman.lhf_mudv2.execution.Event.ItemSeenEvent;
import com.geendutchman.lhf_mudv2.execution.Event.RoomSeenEvent;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Parameters;

@Component
@Scope("prototype")
@Command(name = "see", description = "Lets you see or examine something", subcommands = { HelpCommand.class })
public final class SeeCommand extends SwitchedHandler {

    protected SeeCommand(MessageBus messbus, MessageContext context) {
        super(messbus, context);
    }

    @Parameters(arity = "0..1", index = "0", description = "What you want to examine")
    protected Optional<Entity> what;

    @Override
    public void run() {
        if (this.what != null && this.what.isPresent()) {
            if (this.what.get() instanceof Item item) {
                this.onItem(item);
            } else if (this.what.get() instanceof Creature creature) {
                this.onCreature(creature);
            } else if (this.what.get() instanceof Room room) {
                this.onRoom(room);
            } else if (this.context.sender().room().isPresent()) {
                this.onRoom(this.context.sender().room().get());
            } else {
                this.unrecognized();
            }
        } else if (this.context.sender().room().isPresent()) {
            this.onRoom(this.context.sender().room().get());
        } else {
            this.unrecognized();
        }
    }

    @Override
    protected void onItem(Item item) {
        context.sender().creature()
                .filter(creature -> creature.hasItem(item)
                        || item.visibility().test(creature.plainCheck(AttributeScores.SAVVY)))
                .ifPresentOrElse(creature -> {
                    bus.publish(
                            MessageContext.builder().setSenderId(context.sender().room().get().roomID())
                                    .setDestination(context.sender()).addOther("seenItem", item).build(),
                            new ItemSeenEvent(item));
                }, () -> {
                    // TODO: failure message
                });
    }

    @Override
    protected void onCreature(Creature creature) {
        bus.publish(MessageContext.builder().setSenderId(context.sender().room().get().roomID())
                .setDestination(context.sender()).build(), new CreatureSeenEvent(creature));
    }

    @Override
    protected void onRoom(Room room) {
        bus.publish(MessageContext.builder().setSenderId(room.roomID()).setDestination(context.sender()).build(),
                new RoomSeenEvent(room, null, null));
    }

    @Override
    protected void unrecognized() {
        // TODO: does nothing for now
    }

}
