package com.geendutchman.lhf_mudv2.execution.controllers;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureEffect;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.geendutchman.lhf_mudv2.entities.room.RoomEffect;
import com.geendutchman.lhf_mudv2.execution.Command;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.LHFCommand;
import com.geendutchman.lhf_mudv2.execution.Message;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor;
import com.geendutchman.lhf_mudv2.execution.UserCommand;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import jakarta.annotation.PostConstruct;

@Component
public class ItemFactoryController implements MessageProcessor {

    @Autowired
    private final ItemBuilderFactory factory;

    @Autowired
    private final MessageBus bus;

    private final IEntityID.EntityID id = new IEntityID.EntityID(new Taggable.Tag("builderFactory"),
            new Examinable.Name("items"), UUID.randomUUID());
    private final MessageProcessorID processorID = new MessageProcessorID(UUID.randomUUID());

    ItemFactoryController(@Autowired ItemBuilderFactory fact, @Autowired MessageBus bus) {
        Preconditions.checkNotNull(fact, "item builder factory should not be null");
        Preconditions.checkNotNull(bus, "message bus should not be null");
        this.factory = fact;
        this.bus = bus;
    }

    @PostConstruct
    public void register() {
        this.bus.registerProcessor(this);
    }

    @Override
    public MessageProcessorID messageProcessorID() {
        return this.processorID;
    }

    @Override
    public MessageProcessingResult process(final MessageContext context, final LHFCommand lhfCommand) {
        return switch (lhfCommand) {
        case LHFCommand.ReassignProcessor rp -> MessageProcessingResult
                .Failed("only processes builder factory commands");
        case LHFCommand.BuilderFactoryCommand bfc -> {
            yield switch (bfc) {
            case LHFCommand.BuilderFactoryCommand.CreateCreaturesForRoomCommand ccfrc -> MessageProcessingResult
                    .Failed("Only handles create items commands");
            case LHFCommand.BuilderFactoryCommand.CreateItemsForCreatureCommand(UUID uuid, ItemBuilderFactory.LockedItemBuilder itemBuilder, CreatureID forCreature) -> {
                Item made = itemBuilder.build(this.factory);
                if (made == null) {
                    yield MessageProcessingResult.Failed("created null item");
                }
                yield bus.send(MessageContext.create(id, forCreature),
                        new LHFCommand.ChangeEntityCommand.ChangeCreatureCommand(UUID.randomUUID(),
                                ImmutableList.of(CreatureEffect.builder().addDeltas(Creature.Delta.ofItemToAdd(made))
                                        .setApplicationDescriptionFromBuilder(
                                                RichOutput.builder().addTaggable(forCreature)
                                                        .addString("now has a new item").addTaggable(made))
                                        .build())));
            }
            case LHFCommand.BuilderFactoryCommand.CreateItemsForRoomCommand(UUID uuid, ItemBuilderFactory.LockedItemBuilder itemBuilder, RoomID forRoom) -> {
                Item made = itemBuilder.build(this.factory);
                if (made == null) {
                    yield MessageProcessingResult.Failed("created null item");
                }
                yield bus.send(MessageContext.create(id, forRoom),
                        new LHFCommand.ChangeEntityCommand.ChangeRoomCommand(UUID.randomUUID(),
                                ImmutableList.of(RoomEffect.builder().addDeltas(Room.Delta.ofItem(made))
                                        .setApplicationDescriptionFromBuilder(RichOutput.builder().addTaggable(forRoom)
                                                .addString("now has a new item").addTaggable(made))
                                        .build())));
            }

            };
        }
        case LHFCommand.ChangeEntityCommand cec -> MessageProcessingResult
                .Failed("only processes builder factory commands");
        case null -> MessageProcessingResult.Failed("cannot process null command");

        };

    }

    @Override
    public MessageProcessingResult process(MessageContext context, Command command) {
        return switch (command) {
        case LHFCommand l -> this.process(context, l);
        case null -> MessageProcessingResult.Failed("Only handles create items commands");
        default -> this.process(context, (Message) command);

        };
    }

    @Override
    public MessageProcessingResult process(MessageContext context, Message message) {
        if (message == null) {
            return MessageProcessingResult.Failed("cannot handle null message");
        }
        return MessageProcessingResult.Failed("Only handles create items commands");
    }

    @Override
    public MessageProcessingResult process(MessageContext context, UserCommand userCommand) {
        return this.process(context, (Message) userCommand);
    }

    @Override
    public MessageProcessingResult process(MessageContext context, Event event) {
        return this.process(context, (Message) event);
    }
}
