package com.geendutchman.lhf_mudv2.execution.controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemEffect;
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
public class ItemController implements MessageProcessor {

    protected final MessageBus bus;

    private final MessageProcessorID processorID = new MessageProcessorID(new Examinable.Name("Item Controller"),
            MessageProcessor.messageProcessorTsidFactory.create());

    ItemController(@Autowired MessageBus bus) {
        Preconditions.checkNotNull(bus, "message bus should not be null");
        this.bus = bus;
    }

    @PostConstruct
    public void register() {
        if (this.getClass() == ItemController.class) {
            this.bus.registerProcessorDefault(this, Item.ItemID.ENTITY_CLASS_ITEM);
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
        final Optional<Item> forItem = context.sender().item();
        if (forItem.isEmpty()) {
            return MessageProcessingResult.Failed("addressed item does not exist");
        }

        return switch (lhfCommand) {
        case LHFCommand.ReassignProcessor rp -> MessageProcessingResult.Failed("cannot reassign processors");
        case LHFCommand.BuilderFactoryCommand bfc -> MessageProcessingResult.Failed("this item cannot build anything");
        case LHFCommand.LineCommand lc -> MessageProcessingResult
                .Failed("This item cannot execute userspace commands...yet");
        case LHFCommand.ChangeEntityCommand cec -> {
            yield switch (cec) {
            case LHFCommand.ChangeEntityCommand.ChangeCreatureCommand ccc -> MessageProcessingResult
                    .Failed("this item is not a creature to be changed");
            case LHFCommand.ChangeEntityCommand.ChangeRoomCommand crc -> MessageProcessingResult
                    .Failed("this item is not a room to be changed");
            case LHFCommand.ChangeEntityCommand.ChangeItemCommand(Tsid tsid, ImmutableList<ItemEffect> effects) -> {
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
                Event event = new Event.ItemChangedEvent(item);
                bus.publish(
                        MessageContext.builder().setSenderId(item.itemID()).setDestination(context.getSender()).build(),
                        event);
                yield MessageProcessingResult.HANDLED;

            }
            };

        }
        };
    }

    @Override
    public MessageProcessingResult process(MessageContext context, Event event) {
        if (event == null) {
            return MessageProcessingResult.Failed("cannot handle null event");
        }
        Optional<Item> forItem = context.destination().item();
        if (forItem.isEmpty()) {
            return MessageProcessingResult.Failed("addressed item does not exist");
        }

        // Most Items don't care about Events
        return MessageProcessingResult.HANDLED;
    }

}
