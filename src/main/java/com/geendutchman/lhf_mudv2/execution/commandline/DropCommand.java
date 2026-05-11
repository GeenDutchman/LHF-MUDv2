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
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessingResult;
import com.geendutchman.lhf_mudv2.execution.commandline.converters.SenderCreatureItemsOnly;

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

    @Parameters(arity = "1", index = "0", description = "What you want to drop. Provide the name \"In quotes\" for the best results", converter = SenderCreatureItemsOnly.class)
    protected Item target;

    @Override
    public MessageProcessingResult call() {
        if (context.sender().room().isPresent() && context.sender().creature().isPresent()) {
            final Room room = context.sender().room().get();
            final Creature creature = context.sender().creature().get();

            creature.applyDelta(Creature.Delta.ofItemToRemove(target));
            room.applyDelta(Room.Delta.ofItemToAdd(target));
            this.bus.publish(
                    MessageContext.builder().addOther("itemDropper", creature).setSenderId(room.roomID())
                            .setDestinationId(room.roomID()).setDestinationDetails(e -> e.room(room)).build(),
                    Event.RoomChangedEvent.ofRoomWithChangeDescription(room, RichOutput.builder().addTaggable(creature)
                            .addString("dropped").addTaggable(target).build()));
            return MessageProcessingResult.HANDLED;
        } else {
            return MessageProcessingResult.Failed("You must be in a room");
        }
    }

}
