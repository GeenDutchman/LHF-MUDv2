package com.geendutchman.lhf_mudv2.entities.item;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Repository;

import com.geendutchman.lhf_mudv2.entities.EntityRepository;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ImmutableSortedSet.Builder;

@Repository
public final class ItemRepository implements ItemContainer, EntityRepository<Item> {

    private final ConcurrentMap<ItemID, ConcreteItem> mapping = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "ItemRepository";
    }

    @Override
    public ImmutableSortedSet<Item> items() {
        final Comparator<Item> comparator = Item.getItemComparator();
        final Builder<Item> builder = ImmutableSortedSet.orderedBy(comparator);
        builder.addAll(mapping.values());
        return builder.build();
    }

    @Override
    public ImmutableSortedMap<String, String> attributes() {
        return ImmutableSortedMap.of();
    }

    public ItemReference addItem(ConcreteItem item) {
        if (item == null) {
            throw new NullPointerException("cannot add a null Item");
        }
        this.mapping.put(item.itemID(), item);
        return ItemReference.ofItem(item);
    }

    public Optional<Item> removeOne(ItemQuery query) {
        for (Iterator<ConcreteItem> iterator = this.mapping.values().iterator(); iterator.hasNext();) {
            final ConcreteItem item = iterator.next();
            if (query.test(item)) {
                iterator.remove();
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    public ImmutableItemContainer removeAll(ItemQuery query) {
        ImmutableItemContainer.Builder builder = ImmutableItemContainer.builder().setName("queryRemoval");
        for (Iterator<ConcreteItem> iterator = this.mapping.values().iterator(); iterator.hasNext();) {
            final ConcreteItem item = iterator.next();
            if (query.test(item)) {
                iterator.remove();
                builder.addItem(item);
            }
        }
        return builder.build();
    }

}
