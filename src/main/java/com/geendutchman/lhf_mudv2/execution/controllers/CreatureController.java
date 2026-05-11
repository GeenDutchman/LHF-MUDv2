package com.geendutchman.lhf_mudv2.execution.controllers;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.Delta;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureEffect;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.EventProcessor;
import com.geendutchman.lhf_mudv2.execution.LHFCommand;
import com.geendutchman.lhf_mudv2.execution.Message;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import jakarta.annotation.PostConstruct;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

@Component
public class CreatureController implements MessageProcessor {
    @Autowired
    protected final MessageBus bus;

    @Autowired
    protected final BiFunction<MessageBus, MessageContext, CommandLine> generator;

    private final MessageProcessorID processorID;

    protected final Logger logger;

    CreatureController(@Autowired MessageBus bus,
            @Autowired BiFunction<MessageBus, MessageContext, CommandLine> generator) {
        Preconditions.checkNotNull(bus, "message bus should not be null");
        Preconditions.checkNotNull(generator, "Command line generator should not be null");
        Examinable.Name name = this.name();
        if (name == null) {
            name = new Examinable.Name("Creature Controller");
        }
        this.processorID = new MessageProcessorID(name, MessageProcessor.messageProcessorTsidFactory.create());
        this.bus = bus;
        this.generator = generator;
        this.logger = LoggerFactory.getLogger(String.format("%s.%s", this.getClass().getName(), name));
    }

    @PostConstruct
    public void register() {
        if (this.getClass() == CreatureController.class) {
            this.bus.registerProcessorDefault(this, Creature.CreatureID.ENTITY_CLASS_CREATURE);
        } else {
            this.bus.registerProcessor(this);
        }
    }

    protected Examinable.Name name() {
        return new Examinable.Name("Creature Controller");
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
        case LHFCommand c -> this.process(context, c);
        case Event e -> this.process(context, e);
        default -> MessageProcessingResult.Failed("unknown message type");
        };
    }

    protected MessageProcessingResult processChangeCreatureCommand(MessageContext context,
            LHFCommand.ChangeEntityCommand.ChangeCreatureCommand changeCreatureCommand, Creature creature) {
        Preconditions.checkNotNull(creature, "creature should not be null");
        Preconditions.checkArgument(creature.identifier().equals(context.destination().baseId()),
                "should be directed to this creature");
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
                    bus.publish(
                            MessageContext.builder().setSenderId(creature.identifier())
                                    .setDestinationId(context.replyTo().orElse(context.getSender()).baseId()).build(),
                            Event.CreatureChangedEvent.ofCreatureWithChangeDescription(creature, out.build()));
                } else {
                    RichOutput description = out.build();
                    Event event = Event.CreatureChangedEvent.ofCreatureWithChangeDescription(creature, description);
                    bus.publish(MessageContext.builder().setSenderId(creature.creatureID())
                            .setDestination(context.getSender()).build(), event);
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

        final Optional<Creature> forCreature = context.sender().creature();
        if (forCreature.isEmpty()) {
            return MessageProcessingResult.Failed("sending creature does not exist");
        }

        final Creature creature = forCreature.get();
        if (!creature.identifier().equals(context.destination().baseId())) {
            return MessageProcessingResult.Failed("sending creature is not the same as the command destination");
        }

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
        case LHFCommand.LineCommand lc -> {
            CommandLine line = this.generator.apply(bus, context);
            // TODO deal with stdout and stderr
            if (line == null) {
                yield MessageProcessingResult.Failed("Could not produce a command line");
            }
            line.execute(lc.commandArray());
            ParseResult parseResult = line.getParseResult();
            List<CommandLine> cmdlist = parseResult.asCommandLineList();
            Object result = cmdlist.getLast().getExecutionResult();
            if (result instanceof MessageProcessingResult mpr) {
                yield mpr;
            } else {
                yield MessageProcessingResult.Failed(String.format("Unknown result -> '%s'", result));
            }
        }

        };
    }

    @Override
    public final MessageProcessingResult process(MessageContext context, Event event) {
        if (event == null) {
            return MessageProcessingResult.Failed("cannot handle null event");
        }

        final Optional<Creature> forCreature = context.destination().creature();
        if (forCreature.isEmpty()) {
            return MessageProcessingResult.Failed("destination creature does not exist");
        }

        final Creature creature = forCreature.get();
        if (!creature.identifier().equals(context.destination().baseId())) {
            return MessageProcessingResult.Failed("addressed creature not in context");
        }

        this.processEvent(context, event, creature);

        forCreature.get().items().stream().forEach(item -> {
            bus.publish(MessageContext.builder().setSenderId(creature.creatureID()).setDestinationId(item.identifier())
                    .build(), event);
        });

        return MessageProcessingResult.HANDLED;
    }

    final void processEvent(MessageContext context, Event event, Creature creature) {
        if (context == null || event == null || creature == null) {
            return;
        }

        ItemizedBehavior behavior = this.itemizedEventBehavior(creature);
        if (behavior == null) {
            behavior = new ItemizedBehavior(creature);
        }

        try (MDC.MDCCloseable resource = MDC.putCloseable("creatureID", creature.creatureID().toString())) {
            EventProcessor.Itemized.distribute(behavior, context, event);
        }
    }

    // Specifically a non-static class, this indicates separate reactions per event
    // type
    public class ItemizedBehavior implements EventProcessor.Itemized {
        protected final Creature creature;

        public ItemizedBehavior(Creature creature) {
            this.creature = creature;
        }

        public void onPlainEvent(MessageContext context, Event.PlainEvent event) {
            // default does nothing
        }

        public void onItemChangedEvent(MessageContext context, Event.ItemChangedEvent event) {
            // default does nothing
        }

        public void onCreatureChangedEvent(MessageContext context, Event.CreatureChangedEvent event) {
            // default does nothing
        }

        public void onInventoryEvent(MessageContext context, Event.InventoryEvent event) {
            // default does nothing
        }

        public void onRoomChangedEvent(MessageContext context, Event.RoomChangedEvent event) {
            // TODO: do something when a creature enters the room
        }

        public void onRoomSeenEvent(MessageContext context, Event.RoomSeenEvent event) {
            // default does nothing
        }

        public void onCreatureSeenEvent(MessageContext context, Event.CreatureSeenEvent event) {
            // default does nothing
        }

        public void onItemSeenEvent(MessageContext context, Event.ItemSeenEvent event) {
            // default does nothing
        }

        public void onSpokenEvent(MessageContext context, Event.SpokenEvent event) {
            CreatureController.this.process(
                    MessageContext.builder().setSenderId(creature.creatureID()).setDestinationId(event.speaker())
                            .build(),
                    new LHFCommand.LineCommand(LHFCommand.idFactory.create(),
                            String.format("say \"I am not sure what to say to you but TODO.\" to \"%s\"",
                                    event.speaker().name()),
                            false));
        }
    }

    protected ItemizedBehavior itemizedEventBehavior(Creature creature) {
        return new ItemizedBehavior(creature);
    }

}
