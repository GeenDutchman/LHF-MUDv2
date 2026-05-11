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
import com.geendutchman.lhf_mudv2.execution.commandline.converters.SenderRoomItemsOnly;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Parameters;

@Component
@Scope("prototype")
@Command(name = "take", description = "Lets you take an item", subcommands = { HelpCommand.class })
public final class TakeCommand extends UserCommandHandler {

    public TakeCommand(MessageBus messbus, MessageContext context) {
        super(messbus, context);
    }

    @Parameters(arity = "1", index = "0", description = "What you want to take. Provide the name \"In quotes\" for the best results.", converter = SenderRoomItemsOnly.class)
    protected Item target;

    @Override
    public MessageProcessingResult call() {
        if (context.sender().room().isPresent() && context.sender().creature().isPresent()) {
            final Room room = context.sender().room().get();
            final Creature creature = context.sender().creature().get();

            room.applyDelta(Room.Delta.ofItemToRemove(target));
            creature.applyDelta(Creature.Delta.ofItemToAdd(target));
            return this.bus.publish(
                    MessageContext.builder().setSenderId(room.roomID()).setDestinationId(room.roomID())
                            .addOther("takenItem", target).build(),
                    Event.RoomChangedEvent.ofRoomWithChangeDescription(room,
                            RichOutput.builder().addTaggable(creature).addString("took").addTaggable(target).build()));
        } else {
            return MessageProcessingResult.Failed("You likely are not in a room where you can take stuff");
        }
    }

}
