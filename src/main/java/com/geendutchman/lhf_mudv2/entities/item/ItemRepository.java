package com.geendutchman.lhf_mudv2.entities.item;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

import org.springframework.stereotype.Repository;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

@Repository
public final class ItemRepository implements ItemContainer {

    private final ConcurrentSkipListMap<ItemID, ConcreteItem> cargo = new ConcurrentSkipListMap<>();

    public ItemRepository add(ConcreteItem... items) {
        if (items != null) {
            for (final ConcreteItem item : items) {
                if (item != null) {
                    this.cargo.put(item.itemID(), item);
                }
            }
        }
        return this;
    }

    public ItemRepository add(Collection<ConcreteItem> items) {
        if (items != null) {
            for (final ConcreteItem item : items) {
                if (item != null) {
                    this.cargo.put(item.itemID(), item);
                }
            }
        }
        return this;
    }

    public ItemRepository addAll(Map<ItemID, ConcreteItem> items) {
        if (items != null) {
            this.cargo.putAll(items);
        }
        return this;
    }

    public Optional<Item> remove(ItemID id) {
        return Optional.ofNullable(this.cargo.remove(id));
    }

    public Optional<Item> remove(Item item) {
        return this.remove(item.itemID());
    }

    @Override
    public ImmutableSet<Item> items() {
        return ImmutableSet.copyOf(this.cargo.values());
    }

    @Override
    public boolean hasItem(Item item) {
        return this.cargo.containsValue(item);
    }

    @Override
    public Optional<Item> byItemID(ItemID id) {
        return Optional.ofNullable(this.cargo.get(id));
    }

    final static Examinable.Name ITEM_REPO_NAME = new Examinable.Name("ItemRepository");

    @Override
    public Examinable.Name name() {
        return ITEM_REPO_NAME;
    }

    @Override
    public ImmutableSortedMap<String, String> attributes() {
        return ImmutableSortedMap.of();
    }

}
