package com.geendutchman.lhf_mudv2.entities.room;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;

import org.springframework.lang.Nullable;

import com.geendutchman.lhf_mudv2.commands.Command;
import com.geendutchman.lhf_mudv2.commands.LHFCommand;
import com.geendutchman.lhf_mudv2.commands.UserCommand;
import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureQuery;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.geendutchman.lhf_mudv2.entities.item.ItemInventory;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.geendutchman.lhf_mudv2.events.Event;
import com.geendutchman.lhf_mudv2.events.EventProcessor;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

class ConcreteRoom implements Room {
    final private RoomID roomID;
    final private Examinable.Name name;
    final private Optional<RichOutput> roomDescription;
    final private Optional<URI> locale;
    final private ItemInventory inventory;
    final private LinkedHashMap<CreatureID, Creature> creatures;
    @Nullable
    final private transient EventProcessor.EventFunction<Room> eventFunction;

    protected static ConcreteRoom buildRoom(Examinable.Name name, Optional<RichOutput> roomDescription,
            Optional<URI> locale, ItemInventory inventory, @Nullable EventProcessor.EventFunction<Room> eventFunction) {
        Preconditions.checkNotNull(name, "name should not be null");
        Preconditions.checkNotNull(locale, "the locale should not be null");
        Preconditions.checkNotNull(roomDescription, "room description may be empty but must not be null");
        Preconditions.checkNotNull(inventory, "inventory should not be null");

        return new ConcreteRoom(name, roomDescription, locale, inventory, eventFunction);
    }

    private ConcreteRoom(Examinable.Name name, Optional<RichOutput> roomDescription, Optional<URI> locale,
            ItemInventory inventory, @Nullable EventProcessor.EventFunction<Room> eventFunction) {
        this.name = name;
        this.roomDescription = roomDescription;
        this.locale = locale;
        this.inventory = inventory;
        this.eventFunction = eventFunction;
        this.roomID = RoomID.make(name);
        this.creatures = new LinkedHashMap<>();
    }

    @Override
    public URI processorURI() {
        return this.roomID.uri();
    }

    @Override
    public Optional<URI> locale() {
        return this.locale;
    }

    @Override
    public void applyDelta(Delta delta) {
        if (delta == null) {
            return;
        }

        switch (delta) {
        case Delta.AddItemDelta(Item item) -> {
            this.inventory.add(item);
            item.applyDelta(Item.Delta.ofLocale(Optional.of(this.identifier().uri())));
        }
        case Delta.AddCreatureDelta(Creature creature) -> {
            this.creatures.put(creature.creatureID(), creature);
            creature.applyDelta(Creature.Delta.ofLocale(Optional.of(this.identifier().uri())));
        }
        case null -> {
        }
        default -> {
        }

        }

    }

    @Override
    public ProcessingResult processEvent(Event event) {
        if (this.eventFunction != null) {
            return this.eventFunction.apply(event, this);
        }
        return ProcessingResult.UNHANDLED;
    }

    @Override
    public CommandResult processCommand(Command command) {
        if (command != null && command instanceof LHFCommand.ChangeRoomCommand crc) {
            RichOutput.Builder out = RichOutput.builder().setOnEmpty(Optional.of("The room has changed"));
            for (final RoomEffect roomEffect : crc.effects()) {
                roomEffect.description().ifPresent(desc -> out.addOutput(desc));
                for (final Room.Delta delta : roomEffect.deltas()) {
                    this.applyDelta(delta);
                }
                roomEffect.applicationDescription().ifPresent(ad -> out.addOutput(ad));
            }
            return new CommandResult.Handled(Event.RoomChangedEvent.builder().setRoom(this)
                    .adjustDescription(dout -> dout.addOutput(out.build()))
                    .setSender(this.locale().orElse(this.processorURI())).setDestination(this.processorURI()).build());
        } else if (command != null && command instanceof UserCommand.SeeCommand seeCommand) {
            if (seeCommand.what().isPresent()) {
                // final EntityQuery query =
                // EntityQuery.builder().setName(seeCommand.what().orElse("")).build();
                final Optional<Creature> findCreature = this
                        .queryOneCreature(CreatureQuery.builder().setName(seeCommand.what().orElse("")).build());
                if (findCreature != null && findCreature.isPresent()) {
                    return new CommandResult.Handled(
                            Event.CreatureSeenEvent.builder().setCreature(findCreature.get()).adjustDescription(out -> {
                            }).setSender(this.processorURI()).setDestination(seeCommand.routing().sender()).build());
                }
                final Optional<Item> findItem = this
                        .queryOneItem(ItemQuery.builder().setNickname(seeCommand.what().orElse("")).build());
                if (findItem != null && findItem.isPresent()) {
                    return new CommandResult.Handled(
                            Event.ItemSeenEvent.builder().setItem(findItem.get()).adjustDescription(out -> {
                            }).setSender(this.processorURI()).setDestination(seeCommand.routing().sender()).build());
                }
            }

            // TODO: need to make checks
            return new CommandResult.Handled(
                    Event.RoomSeenEvent.builder().autoRoom(this, item -> item != null, creature -> creature != null)
                            .setSender(this.processorURI()).setDestination(seeCommand.routing().sender()).build());
        }
        return Room.super.processCommand(command);
    }

    @Override
    public RoomID roomID() {
        return this.roomID;
    }

    @Override
    public Examinable.Name name() {
        return this.name;
    }

    @Override
    public synchronized ImmutableSet<Item> items() {
        return this.inventory.items();
    }

    @Override
    public synchronized ImmutableSet<Creature> creatures() {
        return ImmutableSet.copyOf(this.creatures.values());
    }

    @Override
    public synchronized boolean hasCreature(Creature creature) {
        return this.creatures.containsValue(creature);
    }

    @Override
    public synchronized Optional<Creature> byCreatureID(CreatureID id) {
        return Optional.ofNullable(this.creatures.get(id));
    }

    @Override
    public synchronized boolean hasItem(Item item) {
        return this.inventory.hasItem(item);
    }

    @Override
    public synchronized Optional<Item> byItemID(ItemID id) {
        return this.inventory.byItemID(id);
    }

    @Override
    public Optional<RichOutput> description() {
        return this.roomDescription;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ConcreteRoom [roomID=").append(roomID).append(", name=").append(name);
        if (locale != null && locale.isPresent()) {
            builder.append(", locale=").append(locale.get());
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomID);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof ConcreteRoom))
            return false;
        ConcreteRoom other = (ConcreteRoom) obj;
        return Objects.equals(roomID, other.roomID);
    }

}
