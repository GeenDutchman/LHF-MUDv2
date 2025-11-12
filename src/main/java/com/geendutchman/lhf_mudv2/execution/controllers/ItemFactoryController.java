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
import com.geendutchman.lhf_mudv2.execution.CommandRouting;
import com.geendutchman.lhf_mudv2.execution.LHFCommand;
import com.geendutchman.lhf_mudv2.execution.Message;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor;
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
    public MessageProcessingResult process(LHFCommand lhfCommand) {
        return switch (lhfCommand) {
        case null -> MessageProcessingResult.Failed("cannot process null command");
        case LHFCommand.CreateItemsForCreatureCommand(CommandRouting routing, UUID uuid, ItemBuilderFactory.LockedItemBuilder itemBuilder, CreatureID forCreature) -> {
            Item made = itemBuilder.build(this.factory);
            if (made == null) {
                yield MessageProcessingResult.Failed("created null item");
            }
            yield bus.send(new LHFCommand.ChangeCreatureCommand(new CommandRouting(id, forCreature), UUID.randomUUID(),
                    ImmutableList.of(CreatureEffect.builder().addDeltas(Creature.Delta.ofItem(made))
                            .setApplicationDescriptionFromBuilder(RichOutput.builder().addTaggable(forCreature)
                                    .addString("now has a new item").addTaggable(made))
                            .build())));
        }
        case LHFCommand.CreateItemsForRoomCommand(CommandRouting routing, UUID uuid, ItemBuilderFactory.LockedItemBuilder itemBuilder, RoomID forRoom) -> {
            Item made = itemBuilder.build(this.factory);
            if (made == null) {
                yield MessageProcessingResult.Failed("created null item");
            }
            yield bus.send(new LHFCommand.ChangeRoomCommand(
                    new CommandRouting(id, forRoom), UUID.randomUUID(), ImmutableList.of(RoomEffect.builder()
                            .addDeltas(Room.Delta.ofItem(made)).setApplicationDescriptionFromBuilder(RichOutput
                                    .builder().addTaggable(forRoom).addString("now has a new item").addTaggable(made))
                            .build())));
        }
        default -> MessageProcessingResult.Failed("Only handles create items commands");

        };
    }

    @Override
    public MessageProcessingResult process(Command command) {
        return switch (command) {
        case LHFCommand l -> this.process(l);
        case null -> MessageProcessingResult.Failed("Only handles create items commands");
        default -> this.process((Message) command);

        };
    }

    @Override
    public MessageProcessingResult process(Message message) {
        if (message == null) {
            return MessageProcessingResult.Failed("cannot handle null message");
        }
        return MessageProcessingResult.Failed("Only handles create items commands");
    }
}
