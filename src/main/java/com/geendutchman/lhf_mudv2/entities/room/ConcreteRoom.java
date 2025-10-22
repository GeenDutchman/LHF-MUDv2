package com.geendutchman.lhf_mudv2.entities.room;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.springframework.lang.Nullable;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.creatures.Faction;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.geendutchman.lhf_mudv2.entities.item.ItemInventory;
import com.geendutchman.lhf_mudv2.events.Event;
import com.geendutchman.lhf_mudv2.events.EventBus;
import com.geendutchman.lhf_mudv2.events.EventProcessor;
import com.geendutchman.lhf_mudv2.events.Events;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;

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
    public ProcessingResult processEvent(Event event, EventBus bus) {
        if (this.eventFunction != null) {
            ProcessingResult result = this.eventFunction.apply(event, bus, this);
            if (result instanceof ProcessingResult.Handled) {
                return result;
            }
        }
        if (event != null && event instanceof Events.RoomChangeEvent rce) {
            for (final RoomEffect effect : rce.effects()) {
                for (final Room.Delta delta : effect.deltas()) {
                    this.applyDelta(delta);
                }
            }
            return new ProcessingResult.Handled();
        }
        return new ProcessingResult.Unhandled();
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
    public Optional<RichOutput> roomDescription() {
        return this.roomDescription;
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
        RichOutput.Builder builder = RichOutput.builder().setTag(this.tag() + "-description");
        if (this.roomDescription.isPresent()) {
            builder.addOutput(this.roomDescription.get());
        }
        builder.addExaminable(this.inventory);
        ListMultimap<Faction, BasicTaggable> creatureMapping = this.creatures().stream().collect(Multimaps.toMultimap(
                c -> c.faction(), c -> c.basicTaggable(), MultimapBuilder.treeKeys().arrayListValues()::build));
        for (Entry<Faction, Collection<BasicTaggable>> entry : creatureMapping.asMap().entrySet()) {
            Collection<BasicTaggable> entities = entry.getValue();
            if (entities.size() > 0) {
                RichOutput.Builder entitiyBuilder = RichOutput.builder()
                        .setSequenceName(String.format("%s you can see", entry.getKey().toString()));
                entities.forEach(ent -> entitiyBuilder.addTaggable(ent));
                builder.addOutput(entitiyBuilder.build());
            }
        }
        return Optional.of(builder.build());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ConcreteRoom [roomID=").append(roomID).append(", name=").append(name).append(", locale=");
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
