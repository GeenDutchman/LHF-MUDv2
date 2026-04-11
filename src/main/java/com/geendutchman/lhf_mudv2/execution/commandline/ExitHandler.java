package com.geendutchman.lhf_mudv2.execution.commandline;

import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureRepository;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery.EntityQuery;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemRepository;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.RoomRepository;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.Event.PlainEvent;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

@Component
@Scope("prototype")
@Command(name = "exit", description = "Lets you leave Ibaif", subcommands = { HelpCommand.class })
public final class ExitHandler extends SwitchedHandler {
    @Autowired
    private final ItemRepository itemRepository;
    @Autowired
    private final CreatureRepository creatureRepository;
    @Autowired
    private final RoomRepository roomRepository;

    public ExitHandler(MessageBus bus, MessageContext context, ItemRepository itemRepository,
            CreatureRepository creatureRepository, RoomRepository roomRepository) {
        super(bus, context);
        this.itemRepository = itemRepository;
        this.creatureRepository = creatureRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    public void run() {
        contextSplit();
    }

    @Override
    protected void onItem(Item item) {
        try (final Defer onClose = new Defer()) {
            context.room().ifPresent(r -> {
                r.applyDelta(Room.Delta.ofItemToRemove(item));
            });
            context.creature().ifPresent(c -> {
                c.applyDelta(Creature.Delta.ofItemToRemove(item));
            });
            onClose.addLast(() -> itemRepository.remove(item));
        }
    }

    @Override
    protected void onCreature(Creature creature) {
        try (final Defer onClose = new Defer()) {
            bus.publish(
                    context.toBuilder().setCreature(creature).setSender(creature.identifier())
                            .setDestination(creature.identifier()).build(),
                    Event.PlainEvent
                            .asDescribed(RichOutput.builder().addString("Goodbye,").addTaggable(creature).build()));
            final RichOutput message = RichOutput.builder().addString("Cataclysm,").addTaggable(creature)
                    .addString("is exiting, taking you with them!").build();
            creature.items().stream().forEach(i -> {
                onClose.addLast(() -> creature.applyDelta(Creature.Delta.ofItemToRemove(i)));
                onClose.addLast(() -> itemRepository.remove(i));
                bus.publish(
                        MessageContext.builder().setSender(creature.identifier()).setDestination(i.identifier())
                                .addOther("lostItem:" + i.identifier().toString(), i).build(),
                        Event.PlainEvent.asDescribed(message));
            });
            context.room().ifPresent(r -> {
                r.applyDelta(Room.Delta.ofCreatureToRemove(creature));
                bus.publish(MessageContext.builder().setSender(r.identifier()).setDestination(r.identifier()).build(),
                        PlainEvent.asDescribed(
                                RichOutput.builder().addTaggable(creature).addString("has exited Ibaif").build()));
            });
            onClose.addLast(() -> creatureRepository.remove(creature));
        }
    }

    @Override
    protected void onRoom(Room room) {
        try (final Defer onClose = new Defer()) {
            final RichOutput message = RichOutput.builder().addString("Cataclysm,").addTaggable(room)
                    .addString("is exiting, taking you with it!").build();
            Stream.concat(room.items().stream().map(i -> {
                onClose.addLast(() -> itemRepository.remove(i));
                return (Entity) i;
            }), room.creatures().stream().map(c -> {
                onClose.addLast(() -> creatureRepository.remove(c));
                return (Entity) c;
            })).filter(EntityQuery.builder().build()).forEach(entity -> {
                bus.publish(MessageContext.builder().setSender(room.identifier()).setDestination(entity.identifier())
                        .build(), Event.PlainEvent.asDescribed(message));
            });
            onClose.addLast(() -> roomRepository.remove(room));
        }
    }

    @Override
    protected void unrecognized() {
        bus.publish(MessageContext.builder().setSender(context.sender()).setDestination(context.sender()).build(),
                PlainEvent.asDescribed(RichOutput.builder()
                        .addString("Somehow, you can't exit right now. That is a heckin' huge problem!").build()));
    }

}
