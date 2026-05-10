package com.geendutchman.lhf_mudv2.execution.controllers;

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
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.LHFCommand;
import com.geendutchman.lhf_mudv2.execution.Message;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import jakarta.annotation.PostConstruct;

@Component
public class ItemFactoryController implements MessageProcessor {
    private final static TsidFactory idFactory = TsidFactory
            .newInstance1024(Math.abs("creatureFactory".hashCode() % 1024));
    private final Tsid tsid;

    @Autowired
    private final ItemBuilderFactory factory;

    @Autowired
    private final MessageBus bus;

    private final IEntityID.EntityID id;
    private final MessageProcessorID processorID;

    ItemFactoryController(@Autowired ItemBuilderFactory fact, @Autowired MessageBus bus) {
        Preconditions.checkNotNull(fact, "item builder factory should not be null");
        Preconditions.checkNotNull(bus, "message bus should not be null");
        this.factory = fact;
        this.bus = bus;
        this.tsid = ItemFactoryController.idFactory.create();
        this.id = new IEntityID.EntityID(new Taggable.Tag("builderFactory"), new Examinable.Name("items"), tsid);
        this.processorID = new MessageProcessorID(new Examinable.Name("Item Controller Factory"), tsid);
    }

    @PostConstruct
    public void register() {
        this.bus.registerProcessorDefault(this, id.entityClass());
        this.bus.registerEntity(id, processorID);
    }

    @Override
    public final MessageProcessorID messageProcessorID() {
        return this.processorID;
    }

    @Override
    public MessageProcessingResult process(final MessageContext context, final LHFCommand lhfCommand) {
        return switch (lhfCommand) {
        case LHFCommand.ReassignProcessor rp -> MessageProcessingResult
                .Failed("only processes builder factory commands");
        case LHFCommand.LineCommand lc -> MessageProcessingResult.Failed("Only processes builder factory commands");
        case LHFCommand.BuilderFactoryCommand bfc -> {
            yield switch (bfc) {
            case LHFCommand.BuilderFactoryCommand.CreateCreaturesForRoomCommand ccfrc -> MessageProcessingResult
                    .Failed("Only handles create items commands");
            case LHFCommand.BuilderFactoryCommand.CreateItemsForCreatureCommand(Tsid tsid, ItemBuilderFactory.LockedItemBuilder itemBuilder, CreatureID forCreature) -> {
                Item made = itemBuilder.build(this.factory);
                if (made == null) {
                    yield MessageProcessingResult.Failed("created null item");
                }
                yield bus.send(MessageContext.builder().setSenderId(id).setDestinationId(forCreature).build(),
                        new LHFCommand.ChangeEntityCommand.ChangeCreatureCommand(
                                LHFCommand.ChangeEntityCommand.ChangeCreatureCommand.idFactory.create(),
                                ImmutableList.of(CreatureEffect.builder().addDeltas(Creature.Delta.ofItemToAdd(made))
                                        .setApplicationDescriptionFromBuilder(
                                                RichOutput.builder().addTaggable(forCreature)
                                                        .addString("now has a new item").addTaggable(made))
                                        .build())));
            }
            case LHFCommand.BuilderFactoryCommand.CreateItemsForRoomCommand(Tsid tsid, ItemBuilderFactory.LockedItemBuilder itemBuilder, RoomID forRoom) -> {
                Item made = itemBuilder.build(this.factory);
                if (made == null) {
                    yield MessageProcessingResult.Failed("created null item");
                }
                yield bus.send(MessageContext.builder().setSenderId(id).setDestinationId(forRoom).build(),
                        new LHFCommand.ChangeEntityCommand.ChangeRoomCommand(
                                LHFCommand.ChangeEntityCommand.ChangeRoomCommand.idFactory.create(),
                                ImmutableList.of(RoomEffect.builder().addDeltas(Room.Delta.ofItemToAdd(made))
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
    public MessageProcessingResult process(MessageContext context, Message message) {
        if (message == null) {
            return MessageProcessingResult.Failed("cannot handle null message");
        }
        return MessageProcessingResult.Failed("Only handles create items commands");
    }

    @Override
    public MessageProcessingResult process(MessageContext context, Event event) {
        return this.process(context, (Message) event);
    }
}
