package com.geendutchman.lhf_mudv2.execution.controllers;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemEffect;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.geendutchman.lhf_mudv2.entities.item.ItemRepository;
import com.geendutchman.lhf_mudv2.execution.Command;
import com.geendutchman.lhf_mudv2.execution.CommandRouting;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.LHFCommand;
import com.geendutchman.lhf_mudv2.execution.Message;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor;
import com.geendutchman.lhf_mudv2.execution.UserCommand;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import jakarta.annotation.PostConstruct;

public class ItemController implements MessageProcessor {

    @Autowired
    protected final MessageBus bus;

    @Autowired
    protected final ItemRepository itemRepository;

    private final MessageProcessorID processorID = new MessageProcessorID(UUID.randomUUID());

    ItemController(@Autowired MessageBus bus, @Autowired ItemRepository repo) {
        Preconditions.checkNotNull(bus, "message bus should not be null");
        Preconditions.checkNotNull(repo, "Item repository should not be null");
        this.bus = bus;
        this.itemRepository = repo;
    }

    @PostConstruct
    public void register() {
        this.bus.registerProcessorDefault(this, Item.ItemID.ENTITY_CLASS_ITEM);
    }

    @Override
    public MessageProcessorID messageProcessorID() {
        return this.processorID;
    }

    @Override
    public MessageProcessingResult process(Message message) {
        if (message == null) {
            return MessageProcessingResult.Failed("cannot handle null message");
        }
        return switch (message) {
        case Command c -> this.process(c);
        case Event e -> this.process(e);
        default -> MessageProcessingResult.Failed("unknown message type");
        };
    }

    @Override
    public MessageProcessingResult process(Command command) {
        if (command == null) {
            return MessageProcessingResult.Failed("cannot handle null command");
        }

        return switch (command) {
        case UserCommand u -> this.process(u);
        case LHFCommand l -> this.process(l);
        };
    }

    @Override
    public MessageProcessingResult process(LHFCommand lhfCommand) {
        if (lhfCommand == null) {
            return MessageProcessingResult.Failed("cannot handle null lhf command");
        }
        final Optional<Item> forItem = this.itemRepository.queryOneItem(ItemQuery.builder()
                .adjustEntityQuery(eqb -> eqb.setIdentifier(lhfCommand.routing().destination())).build());
        if (forItem.isEmpty()) {
            return MessageProcessingResult.Failed("addressed item does not exist");
        }

        return switch (lhfCommand) {
        case LHFCommand.ChangeItemCommand(CommandRouting routing, UUID uuid, ImmutableList<ItemEffect> effects) -> {
            final Item item = forItem.get();
            RichOutput.Builder output = RichOutput.builder();
            for (final ItemEffect effect : effects) {
                if (effect == null) {
                    continue;
                }
                RichOutput.Builder forEffect = RichOutput.builder().setSequenceName(effect.name().toString());
                effect.description().ifPresent(desc -> forEffect.addOutput(desc));
                for (final Item.Delta delta : effect.deltas()) {
                    if (delta != null) {
                        item.applyDelta(delta);
                    }
                }
                effect.applicationDescription().ifPresent(appl -> forEffect.addOutput(appl));
                output.addOutput(forEffect.build());
            }
            Event event = Event.ItemChangedEvent.builder().setItem(item).setDescription(output.build())
                    .setSender(item.itemID()).setDestination(routing.sender()).build();
            bus.publish(event);
            yield MessageProcessingResult.HANDLED;
        }
        case LHFCommand.ChangeCreatureCommand ccc -> MessageProcessingResult
                .Failed("this item is not a creature to be changed");
        case LHFCommand.ChangeRoomCommand crc -> MessageProcessingResult
                .Failed("this item is not a room to be changed");
        case LHFCommand.CreateCreaturesForRoomCommand ccfrc -> MessageProcessingResult
                .Failed("this item cannot create creatures");
        case LHFCommand.CreateItemsForCreatureCommand cifcc -> MessageProcessingResult
                .Failed("this item cannot create items");
        case LHFCommand.CreateItemsForRoomCommand cifrc -> MessageProcessingResult
                .Failed("this item cannot create items");
        };
    }

    @Override
    public MessageProcessingResult process(UserCommand userCommand) {
        if (userCommand == null) {
            return MessageProcessingResult.Failed("cannot handle null user command");
        }
        Optional<Item> forItem = this.itemRepository.queryOneItem(ItemQuery.builder()
                .adjustEntityQuery(eqb -> eqb.setIdentifier(userCommand.routing().destination())).build());
        if (forItem.isEmpty()) {
            return MessageProcessingResult.Failed("addressed item does not exist");
        }

        return switch (userCommand) {
        case UserCommand.SeeCommand seeCommand -> this.seeCommand(seeCommand, forItem.get());
        case UserCommand.SayCommand sayCommand -> this.sayCommand(sayCommand, forItem.get());
        case UserCommand.TakeCommand takeCommand -> MessageProcessingResult.Failed("this item has nothing to be taken");
        case UserCommand.DropCommand dropCommand -> MessageProcessingResult
                .Failed("this item cannot have things dropped in it");
        };
    }

    protected MessageProcessingResult seeCommand(UserCommand.SeeCommand seeCommand, Item item) {
        Event seen = Event.ItemSeenEvent.builder().setItem(item).adjustDescription(null).setSender(item.itemID())
                .setDestination(seeCommand.routing().sender()).build();
        bus.publish(seen);
        return MessageProcessingResult.HANDLED;
    }

    protected MessageProcessingResult sayCommand(UserCommand.SayCommand sayCommand, Item item) {
        return MessageProcessingResult.Failed("this item cannot hold conversation");
    }

    @Override
    public MessageProcessingResult process(Event event) {
        if (event == null) {
            return MessageProcessingResult.Failed("cannot handle null event");
        }
        Optional<Item> forItem = this.itemRepository.queryOneItem(
                ItemQuery.builder().adjustEntityQuery(eqb -> eqb.setIdentifier(event.routing().destination())).build());
        if (forItem.isEmpty()) {
            return MessageProcessingResult.Failed("addressed item does not exist");
        }

        // Most Items don't care about Events
        return MessageProcessingResult.HANDLED;
    }

}
