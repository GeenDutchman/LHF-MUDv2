package com.geendutchman.lhf_mudv2.execution.commandline;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Parameters;

@Component
@Scope("prototype")
@Command(name = "drop", description = "Lets you drop an item", subcommands = { HelpCommand.class })
public final class DropCommand extends UserCommandHandler {

    public DropCommand(MessageBus messbus, MessageContext context) {
        super(messbus, context);
    }

    // TODO: correct converter limited to Creature
    @Parameters(arity = "1", index = "0", description = "What you want to drop. Provide the name \"In quotes\" for the best results")
    protected Item target;

    @Override
    public void run() {
        if (context.room().isPresent() && context.creature().isPresent()) {
            final Room room = context.room().get();
            final Creature creature = context.creature().get();

            creature.applyDelta(Creature.Delta.ofItemToRemove(target));
            room.applyDelta(Room.Delta.ofItemToAdd(target));
            this.bus.publish(
                    MessageContext.builder().setRoom(room).addOther("itemDropper", creature).setSender(room.roomID())
                            .setDestination(room.roomID()).build(),
                    Event.RoomChangedEvent.ofRoomWithChangeDescription(room, RichOutput.builder().addTaggable(creature)
                            .addString("dropped").addTaggable(target).build()));
        } else {
            // TODO: some warning
        }
    }

}
