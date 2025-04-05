package com.geendutchman.lhf_mudv2.item;

import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;

public interface ItemInventory extends ItemContainer {
    abstract NavigableSet<ItemReference> contents();

    @Override
    public default ImmutableSortedSet<Item> items() {
        return ImmutableSortedSet.copyOf(this.contents());
    }

    public default boolean addItem(ItemReference item) {
        if (item == null) {
            throw new NullPointerException("cannot add a null Item");
        }
        return this.contents().add(item);
    }

    public default boolean addAll(ItemReference... items) {
        boolean changed = false;
        for (final ItemReference item : items) {
            if (item != null) {
                changed |= this.contents().add(item);
            }
        }
        return changed;
    }

    public default boolean addAll(Collection<ItemReference> items) {
        boolean changed = false;
        for (final ItemReference item : items) {
            if (item != null) {
                changed |= this.contents().add(item);
            }
        }
        return changed;
    }

    public default boolean removeItem(ItemReference item) {
        return this.contents().remove(item);
    }

    public default Optional<ItemReference> removeOne(ItemQuery query) {
        for (Iterator<ItemReference> iterator = this.contents().iterator(); iterator.hasNext();) {
            final ItemReference item = iterator.next();
            if (query.test(item)) {
                iterator.remove();
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    public default ImmutableItemContainer removeAll(ItemQuery query) {
        ImmutableItemContainer.Builder builder = ImmutableItemContainer.builder().setName("queryRemoval");
        for (Iterator<ItemReference> iterator = this.contents().iterator(); iterator.hasNext();) {
            final ItemReference item = iterator.next();
            if (query.test(item)) {
                iterator.remove();
                builder.addItem(item);
            }
        }
        return builder.build();
    }

    public final static class Inventory implements ItemInventory {
        private final String name = "Inventory";
        private final TreeSet<ItemReference> contents = new TreeSet<>(Item.getItemComparator());

        @Override
        public NavigableSet<ItemReference> contents() {
            return this.contents;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public ImmutableSortedMap<String, String> attributes() {
            return ImmutableSortedMap.of();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Inventory [name=").append(name).append(", contents=").append(contents).append("]");
            return builder.toString();
        }

    }

}
