package com.geendutchman.lhf_mudv2.execution.controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemEffect;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.geendutchman.lhf_mudv2.entities.item.ItemRepository;
import com.geendutchman.lhf_mudv2.execution.Command;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.LHFCommand;
import com.geendutchman.lhf_mudv2.execution.Message;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor;
import com.geendutchman.lhf_mudv2.execution.UserCommand;
import com.github.f4b6a3.tsid.Tsid;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import jakarta.annotation.PostConstruct;

@Component
public class ItemController implements MessageProcessor {

    @Autowired
    protected final MessageBus bus;

    @Autowired
    protected final ItemRepository itemRepository;

    private final MessageProcessorID processorID = new MessageProcessorID(
            MessageProcessor.messageProcessorTsidFactory.create());

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
        final Optional<Item> forItem = this.itemRepository.queryOneItem(
                ItemQuery.builder().adjustEntityQuery(eqb -> eqb.setIdentifier(context.destination())).build());
        if (forItem.isEmpty()) {
            return MessageProcessingResult.Failed("addressed item does not exist");
        }

        return switch (lhfCommand) {
        case LHFCommand.ReassignProcessor rp -> MessageProcessingResult.Failed("cannot reassign processors");
        case LHFCommand.BuilderFactoryCommand bfc -> MessageProcessingResult.Failed("this item cannot build anything");
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
                bus.publish(MessageContext.create(item.itemID(), context.getSender()), event);
                yield MessageProcessingResult.HANDLED;

            }
            };

        }
        };
    }

    protected MessageProcessingResult forwardUserCommand(MessageContext context, Item item, UserCommand userCommand) {
        final IEntityID toForward = item.locale().orElse(IEntityID.NULL_ID);
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
        Optional<Item> forItem = this.itemRepository.queryOneItem(
                ItemQuery.builder().adjustEntityQuery(eqb -> eqb.setIdentifier(context.destination())).build());
        if (forItem.isEmpty()) {
            return MessageProcessingResult.Failed("addressed item does not exist");
        }

        return switch (userCommand) {
        case UserCommand.SeeCommand seeCommand -> this.seeCommand(context, seeCommand, forItem.get());
        case UserCommand.SayCommand sayCommand -> this.sayCommand(context, sayCommand, forItem.get());
        case UserCommand.TakeCommand takeCommand -> MessageProcessingResult.Failed("this item has nothing to be taken");
        case UserCommand.DropCommand dropCommand -> MessageProcessingResult
                .Failed("this item cannot have things dropped in it");
        case UserCommand.ExitCommand exitCommand -> {
            final Item item = forItem.get();
            if (context.getSender().compareTo(item.identifier()) == 0) {
                this.itemRepository.remove(forItem.get());
                if (item.locale().isPresent()) {
                    yield this.forwardUserCommand(context, item, exitCommand);
                }
                yield MessageProcessingResult.HANDLED;
            }
            yield MessageProcessingResult.Failed("this item cannot handle requests to exit");
        }
        case UserCommand.StatusCommand statusCommand -> {
            final Item item = forItem.get();
            bus.publish(MessageContext.create(item.identifier(), item.itemID()), new Event.ItemSeenEvent(item));
            yield MessageProcessingResult.HANDLED;
        }
        case UserCommand.InventoryCommand inventoryCommand -> MessageProcessingResult
                .Failed("this item does not have an inventory");
        };
    }

    protected MessageProcessingResult seeCommand(MessageContext context, UserCommand.SeeCommand seeCommand, Item item) {
        Event seen = new Event.ItemSeenEvent(item);
        bus.publish(MessageContext.create(item.itemID(), context.getSender()), seen);
        return MessageProcessingResult.HANDLED;
    }

    protected MessageProcessingResult sayCommand(MessageContext context, UserCommand.SayCommand sayCommand, Item item) {
        return MessageProcessingResult.Failed("this item cannot hold conversation");
    }

    @Override
    public MessageProcessingResult process(MessageContext context, Event event) {
        if (event == null) {
            return MessageProcessingResult.Failed("cannot handle null event");
        }
        Optional<Item> forItem = this.itemRepository.queryOneItem(
                ItemQuery.builder().adjustEntityQuery(eqb -> eqb.setIdentifier(context.destination())).build());
        if (forItem.isEmpty()) {
            return MessageProcessingResult.Failed("addressed item does not exist");
        }

        // Most Items don't care about Events
        return MessageProcessingResult.HANDLED;
    }

}
