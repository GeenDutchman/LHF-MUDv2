package com.geendutchman.lhf_mudv2.execution.commandline;

import java.util.Optional;
import java.util.function.Function;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.RichOutputElement;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.room.Directions;
import com.geendutchman.lhf_mudv2.entities.room.Doorway;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.RoomRepository;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessingResult;
import com.google.common.collect.ImmutableSortedMap;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Parameters;

@Component
@Scope("prototype")
@Command(name = "go", description = "Leave this room to go to another.", subcommands = { HelpCommand.class })
public final class GoCommand extends UserCommandHandler {

    protected final RoomRepository roomRepository;

    public GoCommand(MessageBus messbus, MessageContext context, RoomRepository rooms) {
        super(messbus, context);
        this.roomRepository = rooms;
    }

    @Parameters(arity = "1", index = "0", description = "What direction do you want to travel")
    protected Directions direction;

    @Override
    public MessageProcessingResult call() {
        if (context.sender().room().isEmpty()) {
            return MessageProcessingResult.Failed("You are not currently in a room to be able to \"go\" anywhere");
        }
        final Room room = context.sender().room().get();
        if (context.sender().creature().isEmpty()) {
            return MessageProcessingResult.Failed("Only creatures can \"go\"");
        }
        final Creature creature = context.sender().creature().get();

        final ImmutableSortedMap<Directions, Doorway> doorways = room.doorways();

        Function<Directions, MessageProcessingResult> onFail = (goDir) -> {
            RichOutput.Builder desc = RichOutput.builder().addTaggable(creature).addString("- you cannot go");
            if (goDir != null) {
                desc.addTaggable(goDir);
            } else {
                desc.addString(direction.toString());
            }
            desc.addString("as that direction is not available.");

            RichOutput.Builder out = RichOutput.builder().setSequenceName("Available Directions")
                    .setOnEmpty(Optional.of("No Directions Available")).setIsAndLast(true)
                    .setElementSeparator(Optional.of(RichOutputElement.ofString(", ")));

            doorways.keySet().forEach(d -> {
                final Doorway door = doorways.get(d);
                if (door.filter().typedTest(creature)) {
                    out.addTaggable(d);
                } else if (!this.roomRepository.byRoomID(door.target()).isPresent()) {
                    desc.addTaggable(d).addString("is present, but is not connected to anything. ");
                } else {
                    desc.addTaggable(d).addString("is present, but not available for you. ");
                }
            });

            desc.addOutput(out.build());

            this.bus.publish(
                    MessageContext.builder().setSenderId(room.roomID()).setDestinationId(creature.creatureID()).build(),
                    Event.PlainEvent.asDescribed(desc.build()));
            return MessageProcessingResult.Failed(String.format("Cannot go \"%s\"", goDir));
        };

        final Doorway door = doorways.getOrDefault(direction, null);
        if (door == null) {
            return onFail.apply(direction);
        }

        if (!door.filter().typedTest(creature)) {
            return onFail.apply(direction);
        }

        final Optional<Room> nextRoom = this.roomRepository.byRoomID(door.target());
        if (nextRoom == null || nextRoom.isEmpty()) {
            return onFail.apply(direction);
        }

        final Room retrieved = nextRoom.get();
        room.applyDelta(Room.Delta.ofCreatureToRemove(creature));
        retrieved.applyDelta(Room.Delta.ofCreatureToAdd(creature));

        final SeeCommand seecommand = new SeeCommand(bus, context);
        seecommand.setSpec(this.spec);
        seecommand.what = Optional.empty();
        return seecommand.call();
    }

}
