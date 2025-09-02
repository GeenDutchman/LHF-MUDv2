package com.geendutchman.lhf_mudv2.entities.room;

import java.net.URI;
import java.util.Optional;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.item.ItemInventory;
import com.geendutchman.lhf_mudv2.entities.item.ItemReference;
import com.geendutchman.lhf_mudv2.events.Event;
import com.geendutchman.lhf_mudv2.events.EventBus;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

class ConcreteRoom implements Room {
    final private RoomID roomID = RoomID.make();
    final private String name;
    final private Optional<RichOutput> roomDescription;
    final private Optional<URI> locale;
    final private ItemInventory inventory;

    protected static ConcreteRoom buildRoom(String name, Optional<RichOutput> roomDescription, Optional<URI> locale,
            ItemInventory inventory) {
        Preconditions.checkArgument(ROOMNAME_RULES.asMatchPredicate().test(name), "name '%s' must match expression: %s",
                name, ROOMNAME_RULES);
        Preconditions.checkNotNull(locale, "the locale should not be null");
        Preconditions.checkNotNull(roomDescription, "room description may be empty but must not be null");
        Preconditions.checkNotNull(inventory, "inventory should not be null");

        return new ConcreteRoom(name, roomDescription, locale, inventory);
    }

    private ConcreteRoom(String name, Optional<RichOutput> roomDescription, Optional<URI> locale,
            ItemInventory inventory) {
        this.name = name;
        this.roomDescription = roomDescription;
        this.locale = locale;
        this.inventory = inventory;
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
            delta.item().ifPresent(item -> this.inventory.add(item));
            break;
        case ITEMBUILDER:
            delta.itemBuilder().ifPresent(builder -> this.inventory.add(ItemReference.ofItem(builder.build())));
            break;
        default:
            break;
        }
    }

    @Override
    public void processEvent(Event event, EventBus bus) {
        // TODO: inner handlers
    }

    @Override
    public RoomID roomID() {
        return this.roomID;
    }

    @Override
    public String name() {
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
    public Optional<RichOutput> description() {
        RichOutput.Builder builder = RichOutput.builder();
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
}
