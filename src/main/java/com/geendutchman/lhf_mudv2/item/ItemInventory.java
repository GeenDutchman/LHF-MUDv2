package com.geendutchman.lhf_mudv2.item;

import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;

public interface ItemInventory extends ItemContainer {
    abstract NavigableSet<Item> contents();

    @Override
    public default ImmutableSortedSet<Item> items() {
        return ImmutableSortedSet.copyOf(this.contents());
    }

    public default boolean addItem(Item item) {
        if (item == null) {
            throw new NullPointerException("cannot add a null Item");
        }
        return this.contents().add(item);
    }

    public default boolean addAll(Item... items) {
        boolean changed = false;
        for (final Item item : items) {
            if (item != null) {
                changed |= this.contents().add(item);
            }
        }
        return changed;
    }

    public default boolean addAll(Collection<Item> items) {
        boolean changed = false;
        for (final Item item : items) {
            if (item != null) {
                changed |= this.contents().add(item);
            }
        }
        return changed;
    }

    public default boolean removeItem(Item item) {
        return this.contents().remove(item);
    }

    public default Optional<Item> removeOne(Query query) {
        for (Iterator<Item> iterator = this.contents().iterator(); iterator.hasNext();) {
            final Item item = iterator.next();
            if (query.test(item)) {
                iterator.remove();
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    public default ImmutableItemContainer removeAll(Query query) {
        ImmutableItemContainer.Builder builder = ImmutableItemContainer.builder().setName("queryRemoval");
        for (Iterator<Item> iterator = this.contents().iterator(); iterator.hasNext();) {
            final Item item = iterator.next();
            if (query.test(item)) {
                iterator.remove();
                builder.addItem(item);
            }
        }
        return builder.build();
    }

    public final static class Inventory implements ItemInventory {
        private final String name = "Inventory";
        private final TreeSet<Item> contents = new TreeSet<>(Item.getItemComparator());

        @Override
        public NavigableSet<Item> contents() {
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
