package com.geendutchman.lhf_mudv2.execution.controllers;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.RichOutputElement;
import com.geendutchman.lhf_mudv2.entities.creatures.AttributeScores;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureContainer;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureQuery;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery.EntityQuery;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemContainer;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.geendutchman.lhf_mudv2.entities.room.Directions;
import com.geendutchman.lhf_mudv2.entities.room.Doorway;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.RoomEffect;
import com.geendutchman.lhf_mudv2.entities.room.RoomRepository;
import com.geendutchman.lhf_mudv2.execution.Command;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.Event.CreatureSeenEvent;
import com.geendutchman.lhf_mudv2.execution.Event.ItemSeenEvent;
import com.geendutchman.lhf_mudv2.execution.Event.PlainEvent;
import com.geendutchman.lhf_mudv2.execution.Event.RoomSeenEvent;
import com.geendutchman.lhf_mudv2.execution.Event.SpokenEvent;
import com.geendutchman.lhf_mudv2.execution.LHFCommand;
import com.geendutchman.lhf_mudv2.execution.Message;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor;
import com.geendutchman.lhf_mudv2.execution.UserCommand;
import com.github.f4b6a3.tsid.Tsid;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

import jakarta.annotation.PostConstruct;

@Component
public class RoomController implements MessageProcessor {

    @Autowired
    protected final MessageBus bus;

    @Autowired
    protected final RoomRepository roomRepository;

    private final MessageProcessorID processorID = new MessageProcessorID(new Examinable.Name("Room Controller"),
            MessageProcessor.messageProcessorTsidFactory.create());

    RoomController(@Autowired MessageBus bus, @Autowired RoomRepository repo) {
        Preconditions.checkNotNull("bus", "message bus should not be null");
        Preconditions.checkNotNull(repo, "Room repository should not be null");
        this.bus = bus;
        this.roomRepository = repo;
    }

    @PostConstruct
    public void register() {
        if (this.getClass() == RoomController.class) {
            this.bus.registerProcessorDefault(this, Room.RoomID.ENTITY_CLASS_ROOM);
        } else {
            this.bus.registerProcessor(this);
        }
    }

    @Override
    public final MessageProcessorID messageProcessorID() {
        return this.processorID;
    }

    @Override
    public MessageProcessingResult process(MessageContext context, Message message) {
        if (message == null) {
            return MessageProcessingResult.Failed("cannot handle null message");
        }
        return switch (message) {
        case Command c -> this.process(context, c);
        case Event e -> this.process(context, e);
        default -> MessageProcessingResult.Failed("unknown message type");
        };
    }

    @Override
    public MessageProcessingResult process(MessageContext context, Command command) {
        if (command == null) {
            return MessageProcessingResult.Failed("cannot handle null command");
        }

        return switch (command) {
        case UserCommand u -> this.process(context, u);
        case LHFCommand l -> this.process(context, l);
        };
    }

    @Override
    public MessageProcessingResult process(MessageContext context, LHFCommand lhfCommand) {
        if (lhfCommand == null) {
            return MessageProcessingResult.Failed("cannot handle null lhf command");
        }

        final Optional<Room> forRoom = this.roomRepository
                .queryOneRoom(IEntityQuery.entityQueryBuilder().setIdentifier(context.destination()).build());
        if (forRoom.isEmpty()) {
            return MessageProcessingResult.Failed("addressed room does not exist");
        }

        final Room room = forRoom.get();

        return switch (lhfCommand) {
        case LHFCommand.ReassignProcessor rp -> MessageProcessingResult.Failed("cannot handle processor reassignment");
        case LHFCommand.BuilderFactoryCommand bfc -> MessageProcessingResult.Failed("room cannot build anything");
        case LHFCommand.ChangeEntityCommand cec -> {
            yield switch (cec) {
            case LHFCommand.ChangeEntityCommand.ChangeItemCommand cic -> MessageProcessingResult
                    .Failed("creature cannot change items");
            case LHFCommand.ChangeEntityCommand.ChangeRoomCommand(Tsid tsid, ImmutableList<RoomEffect> effects) -> {
                if (effects != null) {
                    for (final RoomEffect effect : effects) {
                        if (effect == null) {
                            continue;
                        }
                        for (Room.Delta delta : effect.deltas()) {
                            if (delta != null) {
                                room.applyDelta(delta);
                            }
                        }

                        RichOutput.Builder out = RichOutput.builder().setSequenceName(effect.name().toString());
                        out.addOutput(effect.applicationDescription().orElseGet(() -> RichOutput.builder()
                                .addString("Something has changed with").addTaggable(room).build()));

                        if (context.replyTo().isPresent()) {
                            bus.publish(context.forward(context.replyTo().orElse(context.getSender())),
                                    Event.RoomChangedEvent.ofRoomWithChangeDescription(room, out.build()));
                        } else {
                            RichOutput description = out.build();
                            Event event = Event.RoomChangedEvent.ofRoomWithChangeDescription(room, description);
                            bus.publish(MessageContext.create(room.roomID(), context.getSender()), event);
                            this.process(context, event);
                        }
                    }
                }
                yield MessageProcessingResult.HANDLED;
            }
            case LHFCommand.ChangeEntityCommand.ChangeCreatureCommand ccc -> MessageProcessingResult
                    .Failed("room cannot change creature");
            };
        }
        };
    }

    protected MessageProcessingResult forwardUserCommand(MessageContext context, Room room, UserCommand userCommand) {
        final IEntityID toForward = room.locale().orElse(IEntityID.NULL_ID);
        final MessageProcessor processor = bus.processorForEntity(toForward);
        if (processor == null) {
            return MessageProcessingResult.Failed("no handler to forward request");
        }
        return processor.process(context.forward(toForward), userCommand);
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
        case UserCommand.GoCommand goCommand -> this.processGoCommand(context, room, goCommand);
        case UserCommand.ExitCommand exitCommand -> {
            if (context.getSender().compareTo(room.identifier()) == 0) {
                try {
                    if (room.locale().isPresent()) {
                        yield this.forwardUserCommand(context, room, exitCommand);
                    }
                    this.process(context, Event.PlainEvent.asDescribed(RichOutput.builder().addString("Cataclysm,")
                            .addTaggable(room).addString("is exiting, taking you with it!").build()));
                    for (Creature c : room.creatures()) {
                        if (c != null) {
                            bus.send(MessageContext.create(c.creatureID(), c.creatureID()), exitCommand);
                        }
                    }
                    for (Item i : room.items()) {
                        if (i != null) {
                            bus.send(MessageContext.create(i.itemID(), i.itemID()), exitCommand);
                        }
                    }
                } finally {
                    this.roomRepository.remove(room);
                }
                yield MessageProcessingResult.HANDLED;
            } else if (context.getSender().entityClass().equals(Creature.CreatureID.ENTITY_CLASS_CREATURE)) {
                CreatureContainer creatureContainer = room.queryCreatures(
                        CreatureQuery.builder().setIdentifier(Optional.of(context.getSender())).build());
                RichOutput.Builder out = RichOutput.builder().setIsAndLast(true).setSequenceName("Removed Creatures");
                boolean hasCreatures = false;
                for (final Creature creature : creatureContainer.creatures()) {
                    if (creature == null) {
                        continue;
                    }
                    hasCreatures = true;
                    room.applyDelta(Room.Delta.ofCreatureToRemove(creature));
                    out.addTaggable(creature);
                }
                if (hasCreatures) {
                    bus.publish(MessageContext.create(room.identifier(), room.identifier()),
                            PlainEvent.asDescribed(RichOutput.builder().addString("The following Creatures have exited")
                                    .addTaggable(room).addOutput(out.build()).build()));
                }
                if (room.locale().isPresent()) {
                    yield this.forwardUserCommand(context, room, exitCommand);
                }
                yield MessageProcessingResult.HANDLED;
            } else if (context.getSender().entityClass().equals(Item.ItemID.ENTITY_CLASS_ITEM)) {
                ItemContainer itemContainer = room
                        .queryItems(ItemQuery.builder().setIdentifier(Optional.of(context.getSender())).build());
                RichOutput.Builder out = RichOutput.builder().setIsAndLast(true).setSequenceName("Removed Items");
                boolean hasItems = false;
                for (final Item item : itemContainer.items()) {
                    if (item == null) {
                        continue;
                    }
                    hasItems = true;
                    room.applyDelta(Room.Delta.ofItemToRemove(item));
                    out.addTaggable(item);
                }
                if (hasItems) {
                    bus.publish(MessageContext.create(room.identifier(), room.identifier()),
                            PlainEvent.asDescribed(RichOutput.builder().addString("The following Items have exited")
                                    .addTaggable(room).addOutput(out.build()).build()));
                }
                if (room.locale().isPresent()) {
                    yield this.forwardUserCommand(context, room, exitCommand);
                }
                yield MessageProcessingResult.HANDLED;
            }
            yield MessageProcessingResult.Failed("this room cannot handle requests to exit");

        }
        case UserCommand.StatusCommand statusCommand -> bus.publish(MessageContext.create(room.roomID(), room.roomID()),
                new Event.RoomSeenEvent(room, null, null));
        case UserCommand.InventoryCommand inventoryCommand -> MessageProcessingResult
                .Failed("room cannot handle inventory command");
        };
    }

    protected MessageProcessingResult processSeeCommand(MessageContext context, Room room,
            UserCommand.SeeCommand seeCommand) {
        final IEntityID potentialCreatureID = context.getDestinationTrace()
                .getOrDefault(Creature.CreatureID.ENTITY_CLASS_CREATURE, IEntityID.NULL_ID);
        final Optional<Creature> forCreature = room
                .queryOneCreature(IEntityQuery.entityQueryBuilder().setIdentifier(potentialCreatureID).build());
        if (forCreature.isEmpty()) {
            return MessageProcessingResult.Failed(String.format("No creature '%s' for command", potentialCreatureID));
        }

        final Creature creature = forCreature.get();

        if (seeCommand.what().isPresent()) {
            Optional<Creature> foundC = room
                    .queryOneCreature(CreatureQuery.builder().setName(seeCommand.what().orElse("Nobody")).build());
            if (foundC.isPresent()) {
                return bus.publish(MessageContext.create(room.roomID(), creature.creatureID()),
                        new CreatureSeenEvent(foundC.get()));
            }
            Optional<Item> foundI = room
                    .queryOneItem(ItemQuery.builder().setIsVisible(creature.plainCheck(AttributeScores.SAVVY))
                            .setDisplayName(seeCommand.what().orElse("Nothing")).build());
            if (foundI.isPresent()) {
                return bus.publish(MessageContext.create(room.roomID(), creature.creatureID()),
                        new ItemSeenEvent(foundI.get()));
            }
        }
        return bus.publish(MessageContext.create(room.roomID(), creature.creatureID()),
                new RoomSeenEvent(room, null, null));

    }

    protected MessageProcessingResult processSayCommand(MessageContext context, Room room,
            UserCommand.SayCommand sayCommand) {
        final IEntityID potentialCreatureID = context.getDestinationTrace()
                .getOrDefault(Creature.CreatureID.ENTITY_CLASS_CREATURE, IEntityID.NULL_ID);
        final Optional<Creature> forCreature = room
                .queryOneCreature(IEntityQuery.entityQueryBuilder().setIdentifier(potentialCreatureID).build());
        if (forCreature.isEmpty()) {
            return MessageProcessingResult.Failed(String.format("No creature '%s' for command", potentialCreatureID));
        }

        final Creature creature = forCreature.get();

        if (sayCommand.toWhom().isPresent()) {
            final ImmutableSet<Creature> creatures = room.queryCreatures(CreatureQuery.builder()
                    .adjustEntityQuery(
                            eq -> eq.setNamePattern(Pattern.compile("^" + Pattern.quote(sayCommand.toWhom().get()))))
                    .build()).creatures();
            if (creatures.isEmpty()) {
                bus.publish(MessageContext.create(room.roomID(), creature.creatureID()),
                        Event.PlainEvent.asDescribed(RichOutput.builder()
                                .addString(String.format("No creature found matching \"%s\" in the room",
                                        sayCommand.toWhom().orElse("")))
                                .addTaggable(room).build()));
                return MessageProcessingResult.HANDLED;
            } else if (creatures.size() == 1) {
                final Creature first = creatures.asList().getFirst();
                SpokenEvent voice = SpokenEvent.speakingTo(creature.creatureID(),
                        RichOutput.builder().addString(sayCommand.message()).build(), first.identifier());
                bus.publish(MessageContext.create(creature.creatureID(), first.creatureID()), voice);
                bus.publish(MessageContext.create(creature.creatureID(), creature.creatureID()), voice);
                return MessageProcessingResult.HANDLED;
            } else {
                final Creature first = creatures.asList().getFirst();
                if (first.name().toString().equals(sayCommand.toWhom().get())) {
                    SpokenEvent voice = SpokenEvent.speakingTo(creature.creatureID(),
                            RichOutput.builder().addString(sayCommand.message()).build(), first.identifier());
                    bus.publish(MessageContext.create(creature.creatureID(), first.creatureID()), voice);
                    bus.publish(MessageContext.create(creature.creatureID(), creature.creatureID()), voice);
                    return MessageProcessingResult.HANDLED;
                }
                RichOutput.Builder matches = RichOutput.builder().setSequenceName("Possible Listeners")
                        .setElementSeparator(Optional.of(RichOutputElement.ofString(", "))).setIsAndLast(true);
                creatures.forEach(c -> matches.addTaggable(c));
                bus.publish(MessageContext.create(room.roomID(), creature.creatureID()),
                        Event.PlainEvent
                                .asDescribed(
                                        RichOutput.builder().addTaggable(creature)
                                                .addString(String.format("has many potential matches for \"%s\"",
                                                        sayCommand.toWhom().orElse("")))
                                                .addOutput(matches.build()).build()));
                return MessageProcessingResult.HANDLED;
            }
        }

        SpokenEvent voice = SpokenEvent.speaking(creature.creatureID(),
                RichOutput.builder().addString(sayCommand.message()).build());
        return bus.publish(MessageContext.create(creature.creatureID(), room.roomID()), voice);
    }

    protected MessageProcessingResult processTakeCommand(MessageContext context, Room room,
            UserCommand.TakeCommand takeCommand) {
        final IEntityID potentialCreatureID = context.getDestinationTrace()
                .getOrDefault(Creature.CreatureID.ENTITY_CLASS_CREATURE, IEntityID.NULL_ID);
        final Optional<Creature> forCreature = room
                .queryOneCreature(IEntityQuery.entityQueryBuilder().setIdentifier(potentialCreatureID).build());
        if (forCreature.isEmpty()) {
            return MessageProcessingResult.Failed(String.format("No creature '%s' for command", potentialCreatureID));
        }

        final Creature creature = forCreature.get();

        final ImmutableSet<Item> items = room
                .queryItems(ItemQuery.builder()
                        .setDisplayNamePattern(Pattern.compile("^" + Pattern.quote(takeCommand.what()))).build())
                .items();

        if (items.isEmpty()) {
            bus.publish(MessageContext.create(room.roomID(), creature.creatureID()),
                    Event.PlainEvent.asDescribed(RichOutput.builder()
                            .addString(String.format("No item found matching \"%s\" in the room", takeCommand.what()))
                            .addTaggable(room).build()));
            return MessageProcessingResult.HANDLED;
        } else if (items.size() == 1) {
            final Item item = items.asList().getFirst();
            room.applyDelta(Room.Delta.ofItemToRemove(item));
            creature.applyDelta(Creature.Delta.ofItemToAdd(item));
            return this.bus.publish(MessageContext.create(room.roomID(), room.roomID()),
                    Event.RoomChangedEvent.ofRoomWithChangeDescription(room,
                            RichOutput.builder().addTaggable(creature).addString("took").addTaggable(item).build()));
        } else {
            final Item item = items.asList().getFirst();
            if (item.displayName().toString().equals(takeCommand.what())) {
                room.applyDelta(Room.Delta.ofItemToRemove(item));
                creature.applyDelta(Creature.Delta.ofItemToAdd(item));
                return this.bus.publish(MessageContext.create(room.roomID(), room.roomID()),
                        Event.RoomChangedEvent.ofRoomWithChangeDescription(room, RichOutput.builder()
                                .addTaggable(creature).addString("took").addTaggable(item).build()));
            }
            RichOutput.Builder matches = RichOutput.builder().setSequenceName("Item Matches")
                    .setElementSeparator(Optional.of(RichOutputElement.ofString(", "))).setIsAndLast(true);
            items.forEach(i -> matches.addTaggable(i));
            bus.publish(MessageContext.create(room.roomID(), creature.creatureID()),
                    Event.PlainEvent.asDescribed(RichOutput.builder().addTaggable(creature)
                            .addString(String.format("has many potential matches for \"%s\"", takeCommand.what()))
                            .addOutput(matches.build()).build()));
            return MessageProcessingResult.HANDLED;
        }
    }

    protected MessageProcessingResult processDropCommand(MessageContext context, Room room,
            UserCommand.DropCommand dropCommand) {
        final IEntityID potentialCreatureID = context.getDestinationTrace()
                .getOrDefault(Creature.CreatureID.ENTITY_CLASS_CREATURE, IEntityID.NULL_ID);
        final Optional<Creature> forCreature = room
                .queryOneCreature(IEntityQuery.entityQueryBuilder().setIdentifier(potentialCreatureID).build());
        if (forCreature.isEmpty()) {
            return MessageProcessingResult.Failed(String.format("No creature '%s' for command", potentialCreatureID));
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
            room.applyDelta(Room.Delta.ofItemToAdd(item));
            return this.bus.publish(MessageContext.create(room.roomID(), room.roomID()),
                    Event.RoomChangedEvent.ofRoomWithChangeDescription(room,
                            RichOutput.builder().addTaggable(creature).addString("dropped").addTaggable(item).build()));
        } else {
            final Item first = items.asList().getFirst();
            if (first.displayName().toString().equals(dropCommand.what())) {
                creature.applyDelta(Creature.Delta.ofItemToRemove(first));
                room.applyDelta(Room.Delta.ofItemToAdd(first));
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

    protected MessageProcessingResult processGoCommand(MessageContext context, Room room,
            UserCommand.GoCommand goCommand) {
        final IEntityID potentialCreatureID = context.getDestinationTrace()
                .getOrDefault(Creature.CreatureID.ENTITY_CLASS_CREATURE, IEntityID.NULL_ID);
        final Optional<Creature> forCreature = room
                .queryOneCreature(IEntityQuery.entityQueryBuilder().setIdentifier(potentialCreatureID).build());
        if (forCreature.isEmpty()) {
            return MessageProcessingResult.Failed(String.format("No creature '%s' for command", potentialCreatureID));
        }

        final ImmutableSortedMap<Directions, Doorway> doorways = room.doorways();

        final Creature creature = forCreature.get();

        Function<Directions, MessageProcessingResult> onFail = (goDir) -> {
            RichOutput.Builder desc = RichOutput.builder().addTaggable(creature).addString("- you cannot go");
            if (goDir != null) {
                desc.addTaggable(goDir);
            } else {
                desc.addString(goCommand.direction());
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

            this.bus.publish(MessageContext.create(room.roomID(), creature.creatureID()),
                    Event.PlainEvent.asDescribed(desc.build()));
            return MessageProcessingResult.Failed(String.format("'%s' is not a direction you can go",
                    goDir != null ? goDir.name() : goCommand.direction()));
        };

        Directions dir = null;
        try {
            dir = Directions.insensitiveValueOf(goCommand.direction());
            if (!doorways.containsKey(dir)) {
                throw new IllegalArgumentException(String.format("Cannot go '%s'", dir.name()));
            }
            if (!doorways.get(dir).filter().typedTest(creature)) {
                throw new IllegalArgumentException(String.format("Cannot go '%s'", dir.name()));
            }
        } catch (IllegalArgumentException e) {
            return onFail.apply(dir);
        }

        final Optional<Room> nextRoom = this.roomRepository.byRoomID(doorways.get(dir).target());
        if (nextRoom == null || nextRoom.isEmpty()) {
            return onFail.apply(dir);
        }

        final Room retrieved = nextRoom.get();
        room.applyDelta(Room.Delta.ofCreatureToRemove(creature));
        retrieved.applyDelta(Room.Delta.ofCreatureToAdd(creature));

        return this.processSeeCommand(
                MessageContext.create(creature.creatureID(), creature.creatureID()).forward(retrieved.roomID()),
                retrieved, new UserCommand.SeeCommand(UserCommand.SeeCommand.idFactory.create(), Optional.empty()));
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
