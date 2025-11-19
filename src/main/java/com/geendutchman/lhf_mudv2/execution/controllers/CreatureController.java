package com.geendutchman.lhf_mudv2.execution.controllers;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.Delta;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureEffect;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureRepository;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery.EntityQuery;
import com.geendutchman.lhf_mudv2.execution.Command;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.LHFCommand;
import com.geendutchman.lhf_mudv2.execution.Message;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor;
import com.geendutchman.lhf_mudv2.execution.UserCommand;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import jakarta.annotation.PostConstruct;

public class CreatureController implements MessageProcessor {
    @Autowired
    protected final MessageBus bus;

    @Autowired
    protected final CreatureRepository creatureRepository;

    private final MessageProcessorID processorID = new MessageProcessorID(UUID.randomUUID());

    CreatureController(@Autowired MessageBus bus, @Autowired CreatureRepository repo) {
        Preconditions.checkNotNull(bus, "message bus should not be null");
        Preconditions.checkNotNull(repo, "Creature repository should not be null");
        this.bus = bus;
        this.creatureRepository = repo;
    }

    @PostConstruct
    public void register() {
        this.bus.registerProcessorDefault(this, Creature.CreatureID.ENTITY_CLASS_CREATURE);
    }

    @Override
    public MessageProcessorID messageProcessorID() {
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
            case LHFCommand.ChangeEntityCommand.ChangeCreatureCommand(UUID uuid, ImmutableList<CreatureEffect> effects) -> {
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
                            Event event = Event.CreatureChangedEvent.ofCreatureWithChangeDescription(creature,
                                    description);
                            bus.publish(MessageContext.create(creature.creatureID(), context.getSender()), event);
                            this.process(context, event);
                        }
                    }
                }
                yield MessageProcessingResult.HANDLED;
            }
            };
        }

        };
    }

    @Override
    public MessageProcessingResult process(MessageContext context, UserCommand userCommand) {
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
        case UserCommand.SeeCommand(UUID uuid, Optional<String> what) -> bus
                .send(context.forward(creature.locale().orElse(IEntityID.NULL_ID)), userCommand);
        case UserCommand.SayCommand(UUID uuid, String message, Optional<String> toWhom) -> bus
                .send(context.forward(creature.locale().orElse(IEntityID.NULL_ID)), userCommand);
        case UserCommand.TakeCommand(UUID uuid, String what) -> bus
                .send(context.forward(creature.locale().orElse(IEntityID.NULL_ID)), userCommand);
        case UserCommand.DropCommand(UUID uuid, String what) -> bus
                .send(context.forward(creature.locale().orElse(IEntityID.NULL_ID)), userCommand);

        };

    }

    @Override
    public MessageProcessingResult process(MessageContext context, Event event) {
        if (event == null) {
            return MessageProcessingResult.Failed("cannot handle null event");
        }

        Optional<Creature> forCreature = this.creatureRepository
                .queryOneCreature(IEntityQuery.entityQueryBuilder().setIdentifier(context.destination()).build());
        if (forCreature.isEmpty()) {
            return MessageProcessingResult.Failed("addressed creature does not exist");
        }

        forCreature.get().items().stream()
                .filter(context.getForwardingRestrictions().orElse(EntityQuery.builder().build())).forEach(item -> {
                    bus.publish(context.forward(item.itemID()), event);
                });

        return MessageProcessingResult.HANDLED;
    }

}
