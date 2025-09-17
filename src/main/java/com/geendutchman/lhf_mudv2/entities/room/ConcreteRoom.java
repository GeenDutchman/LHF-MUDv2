package com.geendutchman.lhf_mudv2.entities.room;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemInventory;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.geendutchman.lhf_mudv2.events.Event;
import com.geendutchman.lhf_mudv2.events.EventBus;
import com.geendutchman.lhf_mudv2.events.EventProcessor;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

class ConcreteRoom implements Room {
    final private RoomID roomID;
    final private Examinable.Name name;
    final private Optional<RichOutput> roomDescription;
    final private Optional<URI> locale;
    final private ItemInventory inventory;
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
        this.roomID = RoomID.make(name.toString());
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

        switch (delta.kind()) {
        case ITEM:
            delta.item().ifPresent(item -> {
                this.inventory.add(item);
                item.applyDelta(Item.Delta.ofLocale(Optional.of(this.identifier().uri())));
            });
            break;
        case ITEMBUILDER:
            delta.itemBuilder().ifPresent(builder -> this.inventory.add(builder.build()));
            break;
        default:
            break;
        }
    }

    @Override
    public ProcessingResult processEvent(Event event, EventBus bus) {
        return this.eventFunction != null ? this.eventFunction.apply(event, bus, this)
                : new ProcessingResult.Unhandled();
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
    public ItemInventory inventory() {
        return this.inventory;
    }

    @Override
    public Stream<Item> items() {
        return this.inventory.items();
    }

    @Override
    public boolean hasItem(Item item) {
        return this.inventory.hasItem(item);
    }

    @Override
    public Optional<Item> byItemID(ItemID id) {
        return this.inventory.byItemID(id);
    }

    @Override
    public Optional<RichOutput> description() {
        RichOutput.Builder builder = RichOutput.builder().setTag(Optional.ofNullable(this.tag() + "-description"));
        if (this.roomDescription.isPresent()) {
            builder.addOutput(this.roomDescription.get());
        }
        builder.addExaminable(this.inventory);
        return Optional.of(builder.build());
    }

    @Override
    public ImmutableSortedMap<String, String> attributes() {
        return ImmutableSortedMap.copyOf(Taggable.produceBasicTagAttributes());
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
