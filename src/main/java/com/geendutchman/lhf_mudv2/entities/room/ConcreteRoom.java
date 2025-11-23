package com.geendutchman.lhf_mudv2.entities.room;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.geendutchman.lhf_mudv2.entities.item.ItemInventory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

class ConcreteRoom implements Room {
    final private RoomID roomID;
    final private Examinable.Name name;
    final private Optional<RichOutput> roomDescription;
    final private Optional<IEntityID> locale;
    final private ItemInventory inventory;
    final private LinkedHashMap<CreatureID, Creature> creatures;

    protected static ConcreteRoom buildRoom(Examinable.Name name, Optional<RichOutput> roomDescription,
            Optional<IEntityID> locale, ItemInventory inventory) {
        Preconditions.checkNotNull(name, "name should not be null");
        Preconditions.checkNotNull(locale, "the locale should not be null");
        Preconditions.checkNotNull(roomDescription, "room description may be empty but must not be null");
        Preconditions.checkNotNull(inventory, "inventory should not be null");

        return new ConcreteRoom(name, roomDescription, locale, inventory);
    }

    private ConcreteRoom(Examinable.Name name, Optional<RichOutput> roomDescription, Optional<IEntityID> locale,
            ItemInventory inventory) {
        this.name = name;
        this.roomDescription = roomDescription;
        this.locale = locale;
        this.inventory = inventory;
        this.roomID = RoomID.make(name);
        this.creatures = new LinkedHashMap<>();
    }

    @Override
    public Optional<IEntityID> locale() {
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
                item.applyDelta(Item.Delta.ofLocale(Optional.of(this.identifier())));
            }
            case Delta.AddCreatureDelta(Creature creature) -> {
                this.creatures.put(creature.creatureID(), creature);
                creature.applyDelta(Creature.Delta.ofLocale(Optional.of(this.identifier())));
            }
            case Delta.RemoveCreatureDelta(Creature creature) -> {
                this.creatures.remove(creature.creatureID(), creature);

            }
            case null -> {
            }
            default -> {
            }

        }

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
