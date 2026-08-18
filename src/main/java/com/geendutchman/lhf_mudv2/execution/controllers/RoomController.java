package com.geendutchman.lhf_mudv2.execution.controllers;

import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.RoomEffect;
import com.geendutchman.lhf_mudv2.entities.room.RoomRepository;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.LHFCommand;
import com.geendutchman.lhf_mudv2.execution.Message;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor;
import com.github.f4b6a3.tsid.Tsid;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import jakarta.annotation.PostConstruct;

@Component
public class RoomController implements MessageProcessor {

    protected final MessageBus bus;
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
        case LHFCommand c -> this.process(context, c);
        case Event e -> this.process(context, e);
        default -> MessageProcessingResult.Failed("unknown message type");
        };
    }

    @Override
    public MessageProcessingResult process(MessageContext context, LHFCommand lhfCommand) {
        if (lhfCommand == null) {
            return MessageProcessingResult.Failed("cannot handle null lhf command");
        }

        final Optional<Room> forRoom = this.roomRepository
                .queryOneRoom(IEntityQuery.entityQueryBuilder().setIdentifier(context.destination().baseId()).build());
        if (forRoom.isEmpty()) {
            return MessageProcessingResult.Failed("addressed room does not exist");
        }

        final Room room = forRoom.get();

        return switch (lhfCommand) {
        case LHFCommand.ReassignProcessor rp -> MessageProcessingResult.Failed("cannot handle processor reassignment");
        case LHFCommand.BuilderFactoryCommand bfc -> MessageProcessingResult.Failed("room cannot build anything");
        case LHFCommand.LineCommand lc -> MessageProcessingResult
                .Failed("This room cannot execute userspace commands...yet");
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
                            bus.publish(
                                    context.toBuilder().setDestination(context.replyTo().orElse(context.getSender()))
                                            .build(),
                                    Event.RoomChangedEvent.ofRoomWithChangeDescription(room, out.build()));
                        } else {
                            RichOutput description = out.build();
                            Event event = Event.RoomChangedEvent.ofRoomWithChangeDescription(room, description);
                            bus.publish(MessageContext.builder().setSenderId(room.roomID())
                                    .setDestination(context.getSender()).build(), event);
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

    @Override
    public MessageProcessingResult process(MessageContext context, Event event) {
        if (event == null) {
            return MessageProcessingResult.Failed("cannot handle null event");
        }

        final Optional<Room> forRoom = this.roomRepository
                .queryOneRoom(IEntityQuery.entityQueryBuilder().setIdentifier(context.destination().baseId()).build());
        if (forRoom.isEmpty()) {
            return MessageProcessingResult.Failed("addressed room does not exist");
        }

        final Room room = forRoom.get();
        Stream.concat(room.items().stream().map(i -> (Entity) i), room.creatures().stream().map(c -> (Entity) c))
                .forEach(entity -> {
                    bus.publish(context.toBuilder().setDestinationId(entity.identifier()).build(), event);
                });

        return MessageProcessingResult.HANDLED;

    }

}
