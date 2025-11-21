package com.geendutchman.lhf_mudv2.execution.controllers;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.RichOutputElement;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery.EntityQuery;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemContainer;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.RoomRepository;
import com.geendutchman.lhf_mudv2.execution.Command;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.LHFCommand;
import com.geendutchman.lhf_mudv2.execution.Message;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor;
import com.geendutchman.lhf_mudv2.execution.UserCommand;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import jakarta.annotation.PostConstruct;

public class RoomController implements MessageProcessor {

    @Autowired
    protected final MessageBus bus;

    @Autowired
    protected final RoomRepository roomRepository;

    private final MessageProcessorID processorID = new MessageProcessorID(UUID.randomUUID());

    RoomController(@Autowired MessageBus bus, @Autowired RoomRepository repo) {
        Preconditions.checkNotNull("bus", "message bus should not be null");
        Preconditions.checkNotNull(repo, "Room repository should not be null");
        this.bus = bus;
        this.roomRepository = repo;
    }

    @PostConstruct
    public void register() {
        this.bus.registerProcessorDefault(this, Room.RoomID.ENTITY_CLASS_ROOM);
    }

    @Override
    public MessageProcessorID messageProcessorID() {
        return this.processorID;
    }

    @Override
    public MessageProcessingResult process(MessageContext context, Message message) {
        // TODO Auto-generated method stub
    }

    @Override
    public MessageProcessingResult process(MessageContext context, Command command) {
        // TODO Auto-generated method stub
    }

    @Override
    public MessageProcessingResult process(MessageContext context, LHFCommand lhfCommand) {
        // TODO Auto-generated method stub
    }

    @Override
    public MessageProcessingResult process(MessageContext context, UserCommand userCommand) {
        if (userCommand == null) {
            return MessageProcessingResult.Failed("cannot handle null user command");
        }

        final Optional<Room> forRoom = this.roomRepository
                .queryOneRoom(IEntityQuery.entityQueryBuilder().setIdentifier(context.destination()).build());
        if (forRoom.isEmpty()) {
            return MessageProcessingResult.Failed("addressed room does not exist");
        }

        final Room room = forRoom.get();

        return switch (userCommand) {
        case UserCommand.SeeCommand seeCommand -> this.processSeeCommand(context, room, seeCommand);
        case UserCommand.SayCommand sayCommand -> this.processSayCommand(context, room, sayCommand);
        case UserCommand.TakeCommand takeCommand -> this.processTakeCommand(context, room, takeCommand);
        case UserCommand.DropCommand dropCommand -> this.processDropCommand(context, room, dropCommand);
        };
    }

    protected MessageProcessingResult processSeeCommand(MessageContext context, Room room,
            UserCommand.SeeCommand seeCommand) {

    }

    protected MessageProcessingResult processSayCommand(MessageContext context, Room room,
            UserCommand.SayCommand sayCommand) {

    }

    protected MessageProcessingResult processTakeCommand(MessageContext context, Room room,
            UserCommand.TakeCommand takeCommand) {

    }

    protected MessageProcessingResult processDropCommand(MessageContext context, Room room,
            UserCommand.DropCommand dropCommand) {
        final IEntityID potentialCreatureID = context.getDestinationTrace()
                .getOrDefault(Creature.CreatureID.ENTITY_CLASS_CREATURE, IEntityID.NULL_ID);
        final Optional<Creature> forCreature = room
                .queryOneCreature(IEntityQuery.entityQueryBuilder().setIdentifier(potentialCreatureID).build());
        if (forCreature.isEmpty()) {
            return MessageProcessingResult.Failed("No creature for command");
        }

        final Creature creature = forCreature.get();

        final ImmutableSet<Item> items = creature
                .queryItems(ItemQuery.builder()
                        .setDisplayNamePattern(Pattern.compile("^" + Pattern.quote(dropCommand.what()))).build())
                .items();

        if (items.isEmpty()) {
            bus.publish(MessageContext.create(room.roomID(), creature.creatureID()),
                    Event.PlainEvent.asDescribed(RichOutput.builder().addString(
                            String.format("No item found matching \"%s\" in the inventory of", dropCommand.what()))
                            .addTaggable(creature).build()));
            return MessageProcessingResult.HANDLED;
        } else if (items.size() == 1) {
            final Item item = items.asList().getFirst();
            creature.applyDelta(Creature.Delta.ofItemToRemove(item));
            room.applyDelta(Room.Delta.ofItem(item));
            return this.bus.publish(MessageContext.create(room.roomID(), room.roomID()),
                    Event.RoomChangedEvent.ofRoomWithChangeDescription(room,
                            RichOutput.builder().addTaggable(creature).addString("dropped").addTaggable(item).build()));
        } else {
            final Item first = items.asList().getFirst();
            if (first.displayName().toString().equals(dropCommand.what())) {
                creature.applyDelta(Creature.Delta.ofItemToRemove(first));
                room.applyDelta(Room.Delta.ofItem(first));
                return this.bus.publish(MessageContext.create(room.roomID(), room.roomID()),
                        Event.RoomChangedEvent.ofRoomWithChangeDescription(room, RichOutput.builder()
                                .addTaggable(creature).addString("dropped").addTaggable(first).build()));
            }
            RichOutput.Builder matches = RichOutput.builder().setSequenceName("Item Matches")
                    .setElementSeparator(Optional.of(RichOutputElement.ofString(", "))).setIsAndLast(true);
            items.forEach(i -> matches.addTaggable(i));
            bus.publish(MessageContext.create(room.roomID(), creature.creatureID()),
                    Event.PlainEvent.asDescribed(RichOutput.builder().addTaggable(creature)
                            .addString(String.format("has many potential matches for \"%s\"", dropCommand.what()))
                            .addOutput(matches.build()).build()));
            return MessageProcessingResult.HANDLED;
        }
    }

    @Override
    public MessageProcessingResult process(MessageContext context, Event event) {
        if (event == null) {
            return MessageProcessingResult.Failed("cannot handle null event");
        }

        final Optional<Room> forRoom = this.roomRepository
                .queryOneRoom(IEntityQuery.entityQueryBuilder().setIdentifier(context.destination()).build());
        if (forRoom.isEmpty()) {
            return MessageProcessingResult.Failed("addressed room does not exist");
        }

        final Room room = forRoom.get();
        Stream.concat(room.items().stream().map(i -> (Entity) i), room.creatures().stream().map(c -> (Entity) c))
                .filter(context.getForwardingRestrictions().orElse(EntityQuery.builder().build())).forEach(entity -> {
                    bus.publish(context.forwardCopy(entity.identifier()), event);
                });

        return MessageProcessingResult.HANDLED;

    }

}
