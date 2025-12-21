package com.geendutchman.lhf_mudv2.execution.controllers;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.Delta;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureEffect;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureRepository;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery.EntityQuery;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemContainer;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.geendutchman.lhf_mudv2.execution.Command;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.Event.PlainEvent;
import com.geendutchman.lhf_mudv2.execution.LHFCommand;
import com.geendutchman.lhf_mudv2.execution.Message;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor;
import com.geendutchman.lhf_mudv2.execution.UserCommand;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import jakarta.annotation.PostConstruct;

@Component
public class CreatureController implements MessageProcessor {
    @Autowired
    protected final MessageBus bus;

    @Autowired
    protected final CreatureRepository creatureRepository;

    private final MessageProcessorID processorID = new MessageProcessorID(
            MessageProcessor.messageProcessorTsidFactory.create());

    CreatureController(@Autowired MessageBus bus, @Autowired CreatureRepository repo) {
        Preconditions.checkNotNull(bus, "message bus should not be null");
        Preconditions.checkNotNull(repo, "Creature repository should not be null");
        this.bus = bus;
        this.creatureRepository = repo;
    }

    @PostConstruct
    public void register() {
        if (this.getClass() == CreatureController.class) {
            this.bus.registerProcessorDefault(this, Creature.CreatureID.ENTITY_CLASS_CREATURE);
        } else {
            this.bus.registerProcessor(this);
        }
    }

    @Override
    public final MessageProcessorID messageProcessorID() {
        return this.processorID;
    }

    @Override
    public final MessageProcessingResult process(MessageContext context, Message message) {
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
    public final MessageProcessingResult process(MessageContext context, Command command) {
        if (command == null) {
            return MessageProcessingResult.Failed("cannot handle null command");
        }

        return switch (command) {
        case UserCommand u -> this.process(context, u);
        case LHFCommand l -> this.process(context, l);
        };
    }

    protected MessageProcessingResult processChangeCreatureCommand(MessageContext context,
            LHFCommand.ChangeEntityCommand.ChangeCreatureCommand changeCreatureCommand, Creature creature) {
        Preconditions.checkNotNull(creature, "creature should not be null");
        Preconditions.checkArgument(
                creature.identifier().equals(context.getDestinationTrace()
                        .getOrDefault(CreatureID.ENTITY_CLASS_CREATURE, IEntityID.NULL_ID)),
                "creature should be identified in the context trace");
        final ImmutableList<CreatureEffect> effects = changeCreatureCommand.effects();
        if (effects != null) {
            for (final CreatureEffect effect : effects) {
                if (effect == null) {
                    continue;
                }
                for (Delta delta : effect.deltas()) {
                    if (delta != null) {
                        creature.applyDelta(delta);
                    }
                }

                RichOutput.Builder out = RichOutput.builder().setSequenceName(effect.name().toString());
                out.addOutput(effect.applicationDescription().orElseGet(() -> RichOutput.builder()
                        .addString("Something has changed with").addTaggable(creature).build()));

                if (context.replyTo().isPresent()) {
                    bus.publish(context.forward(context.replyTo().orElse(context.getSender())),
                            Event.CreatureChangedEvent.ofCreatureWithChangeDescription(creature, out.build()));
                } else {
                    RichOutput description = out.build();
                    Event event = Event.CreatureChangedEvent.ofCreatureWithChangeDescription(creature, description);
                    bus.publish(MessageContext.create(creature.creatureID(), context.getSender()), event);
                    this.process(context, event);
                }
            }
        }
        return MessageProcessingResult.HANDLED;
    }

    @Override
    public final MessageProcessingResult process(MessageContext context, LHFCommand lhfCommand) {
        if (lhfCommand == null) {
            return MessageProcessingResult.Failed("cannot handle null lhf command");
        }

        final Optional<Creature> forCreature = this.creatureRepository.queryOneCreature(IEntityQuery
                .entityQueryBuilder()
                .setIdentifier(
                        context.getDestinationTrace().getOrDefault(CreatureID.ENTITY_CLASS_CREATURE, IEntityID.NULL_ID))
                .build());
        if (forCreature.isEmpty()) {
            return MessageProcessingResult.Failed("addressed creature does not exist");
        }

        final Creature creature = forCreature.get();

        return switch (lhfCommand) {
        case LHFCommand.ReassignProcessor rp -> MessageProcessingResult.Failed("cannot handle processor reassignment");
        case LHFCommand.BuilderFactoryCommand bfc -> MessageProcessingResult.Failed("creature cannot build anything");
        case LHFCommand.ChangeEntityCommand cec -> {
            yield switch (cec) {
            case LHFCommand.ChangeEntityCommand.ChangeItemCommand cic -> MessageProcessingResult
                    .Failed("creature cannot change items");
            case LHFCommand.ChangeEntityCommand.ChangeRoomCommand crc -> MessageProcessingResult
                    .Failed("creature cannot change room");
            case LHFCommand.ChangeEntityCommand.ChangeCreatureCommand ccc -> this.processChangeCreatureCommand(context,
                    ccc, creature);
            };

        }

        };
    }

    protected MessageProcessingResult forwardUserCommand(MessageContext context, Creature creature,
            UserCommand userCommand) {
        final IEntityID toForward = creature.locale().orElse(IEntityID.NULL_ID);
        final MessageProcessor processor = bus.processorForEntity(toForward);
        if (processor == null) {
            return MessageProcessingResult.Failed("no handler to forward request");
        }
        return processor.process(context.forward(toForward), userCommand);
    }

    @Override
    public final MessageProcessingResult process(MessageContext context, UserCommand userCommand) {
        if (userCommand == null) {
            return MessageProcessingResult.Failed("cannot handle null user command");
        }

        final Optional<Creature> forCreature = this.creatureRepository.queryOneCreature(IEntityQuery
                .entityQueryBuilder()
                .setIdentifier(
                        context.getDestinationTrace().getOrDefault(CreatureID.ENTITY_CLASS_CREATURE, IEntityID.NULL_ID))
                .build());
        if (forCreature.isEmpty()) {
            return MessageProcessingResult.Failed("addressed creature does not exist");
        }

        final Creature creature = forCreature.get();

        return switch (userCommand) {
        case UserCommand.SeeCommand seeCommand -> this.forwardUserCommand(context, creature, seeCommand);
        case UserCommand.SayCommand sayCommand -> this.forwardUserCommand(context, creature, sayCommand);
        case UserCommand.TakeCommand takeCommand -> this.forwardUserCommand(context, creature, takeCommand);
        case UserCommand.DropCommand dropCommand -> this.forwardUserCommand(context, creature, dropCommand);
        case UserCommand.ExitCommand exitCommand -> {
            if (context.getSender().compareTo(creature.identifier()) == 0) {
                try {
                    if (creature.locale().isPresent()) {
                        yield this.forwardUserCommand(context, creature, exitCommand);
                    }
                    // TODO: send some "I exited" event
                } finally {
                    this.creatureRepository.remove(creature);
                }
                yield MessageProcessingResult.HANDLED;
            } else if (context.getSender().entityClass().equals(Item.ItemID.ENTITY_CLASS_ITEM)) {
                ItemContainer itemContainer = creature
                        .queryItems(ItemQuery.builder().setIdentifier(Optional.of(context.getSender())).build());
                RichOutput.Builder out = RichOutput.builder().setIsAndLast(true).setSequenceName("Removed Items");
                boolean hasItems = false;
                for (final Item item : itemContainer.items()) {
                    if (item == null) {
                        continue;
                    }
                    hasItems = true;
                    creature.applyDelta(Creature.Delta.ofItemToRemove(item));
                    out.addTaggable(item);
                }
                if (hasItems) {
                    bus.publish(MessageContext.create(creature.identifier(), creature.identifier()),
                            PlainEvent.asDescribed(RichOutput.builder().addString("The following have exited")
                                    .addTaggable(creature).addOutput(out.build()).build()));
                }
                if (creature.locale().isPresent()) {
                    yield this.forwardUserCommand(context, creature, exitCommand);
                }
                yield MessageProcessingResult.HANDLED;
            }
            yield MessageProcessingResult.Failed("this creature cannot handle requests to exit");

        }
        case UserCommand.StatusCommand statusCommand -> {
            bus.publish(MessageContext.create(creature.identifier(), creature.creatureID()),
                    new Event.CreatureSeenEvent(creature));
            yield MessageProcessingResult.HANDLED;
        }
        case UserCommand.InventoryCommand inventoryCommand -> {
            bus.publish(MessageContext.create(creature.identifier(), creature.creatureID()),
                    Event.InventoryEvent.ofCreature(creature));
            yield MessageProcessingResult.HANDLED;
        }
        };
    };

    @Override
    public final MessageProcessingResult process(MessageContext context, Event event) {
        if (event == null) {
            return MessageProcessingResult.Failed("cannot handle null event");
        }

        Optional<Creature> forCreature = this.creatureRepository
                .queryOneCreature(IEntityQuery.entityQueryBuilder().setIdentifier(context.destination()).build());
        if (forCreature.isEmpty()) {
            return MessageProcessingResult.Failed("addressed creature does not exist");
        }

        this.processEvent(context, event, forCreature.get());

        forCreature.get().items().stream()
                .filter(context.getForwardingRestrictions().orElse(EntityQuery.builder().build())).forEach(item -> {
                    bus.publish(context.forwardCopy(item.itemID()), event);
                });

        return MessageProcessingResult.HANDLED;
    }

    final void processEvent(MessageContext context, Event event, Creature creature) {
        if (context == null || event == null || creature == null) {
            return;
        }

        final Logger logger = Logger
                .getLogger(creature.getClass().getName() + creature.creatureID().toString().replace("/", "."));
        final Level level = logger.getLevel();

        if (level.intValue() <= Level.FINER.intValue()) {
            final RichOutput desc = event.description();
            logger.log(level, String.format("%s %s", desc.printIt(), context));
        } else if (level.intValue() <= Level.FINE.intValue()) {
            final RichOutput desc = event.description();
            logger.log(level, desc.printIt());
        }

        switch (event) {
        case Event.PlainEvent plainEvent -> onPlainEvent(context, plainEvent, creature);
        case Event.ItemChangedEvent itemChanged -> onItemChangedEvent(context, itemChanged, creature);
        case Event.CreatureChangedEvent creatureChanged -> onCreatureChangedEvent(context, creatureChanged, creature);
        case Event.InventoryEvent inventory -> onInventoryEvent(context, inventory, creature);
        case Event.RoomChangedEvent roomChanged -> onRoomChangedEvent(context, roomChanged, creature);
        case Event.RoomSeenEvent roomSeen -> onRoomSeenEvent(context, roomSeen, creature);
        case Event.CreatureSeenEvent creatureSeen -> onCreatureSeenEvent(context, creatureSeen, creature);
        case Event.ItemSeenEvent itemSeen -> onItemSeenEvent(context, itemSeen, creature);
        case Event.SpokenEvent speaking -> onSpokenEvent(context, speaking, creature);
        }
    }

    protected void onPlainEvent(MessageContext context, Event.PlainEvent event, Creature creature) {
        // default does nothing
    }

    protected void onItemChangedEvent(MessageContext context, Event.ItemChangedEvent event, Creature creature) {
        // default does nothing
    }

    protected void onCreatureChangedEvent(MessageContext context, Event.CreatureChangedEvent event, Creature creature) {
        // default does nothing
    }

    protected void onInventoryEvent(MessageContext context, Event.InventoryEvent event, Creature creature) {
        // default does nothing
    }

    protected void onRoomChangedEvent(MessageContext context, Event.RoomChangedEvent event, Creature creature) {
        // TODO: do something when a creature enters the room
    }

    protected void onRoomSeenEvent(MessageContext context, Event.RoomSeenEvent event, Creature creature) {
        // default does nothing
    }

    protected void onCreatureSeenEvent(MessageContext context, Event.CreatureSeenEvent event, Creature creature) {
        // default does nothing
    }

    protected void onItemSeenEvent(MessageContext context, Event.ItemSeenEvent event, Creature creature) {
        // default does nothing
    }

    protected void onSpokenEvent(MessageContext context, Event.SpokenEvent event, Creature creature) {
        UserCommand.SayCommand response = new UserCommand.SayCommand(UserCommand.SayCommand.idFactory.create(),
                "I am not sure what to say to you but TODO.", Optional.of(event.speaker().name().toString()));
        this.bus.send(MessageContext.create(creature.creatureID(), event.speaker()), response);
    }

}
