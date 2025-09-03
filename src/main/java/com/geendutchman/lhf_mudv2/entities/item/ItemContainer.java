package com.geendutchman.lhf_mudv2.entities.item;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;

import com.geendutchman.lhf_mudv2.entities.EntityContainer;
import com.geendutchman.lhf_mudv2.entities.IEntityQuery;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;

public interface ItemContainer<I extends Item> extends EntityContainer<I> {

    public abstract ImmutableSortedSet<I> items();

    @Override
    public default ImmutableSortedSet<I> entities() {
        return this.items();
    }

    public default boolean hasItem(I item) {
        return this.items().contains(item);
    }

    public default Optional<I> byItemID(ItemID id) {
        for (final I item : this.items()) {
            if (item != null && id.equals(item.identifier())) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    @Override
    public default String tag() {
        return "Items";
    }

    @Override
    public default String content() {
        return this.name();
    }

    @Override
    public abstract ImmutableSortedMap<String, String> attributes();

    @AutoValue
    public static abstract class ImmutableItemContainer<I extends Item>
            implements ItemContainer<I>, EntityContainer.ImmutableEntityContainer<I> {

        public final static <I extends Item> Builder<I> builder() {
            return new AutoValue_ItemContainer_ImmutableItemContainer.Builder<>();
        }

        @AutoValue.Builder
        static abstract class Builder<I extends Item>
                implements EntityContainer.ImmutableEntityContainer.ImmutableEntityContainerBuilder<I> {

            final private static Comparator<Item> itemComparator = Item.getItemComparator();

            private ImmutableSortedSet.Builder<I> cachedItemsBuilder;

            abstract ImmutableSortedSet.Builder<I> itemsBuilder(Comparator<Item> comparator);

            /**
             * Gets the cachedItemsBuilder
             * 
             * Note: not thread safe!
             * 
             * @return
             */
            private final ImmutableSortedSet.Builder<I> itemsBuilder() {
                if (this.cachedItemsBuilder == null) {
                    this.cachedItemsBuilder = this.itemsBuilder(itemComparator);
                }
                return this.cachedItemsBuilder;
            }

            @Override
            public final Builder<I> add(I item) {
                this.itemsBuilder().add(item);
                return this;
            }

            @Override
            public final Builder<I> add(Iterable<I> items) {
                this.itemsBuilder().addAll(items);
                return this;
            }

            @Override
            public abstract Builder<I> setName(String name);

            @Override
            public abstract ImmutableItemContainer<I> build();
        }
    }

    @Override
    public default Optional<I> queryOne(IEntityQuery<? super I> query) {
        for (final I item : this.items()) {
            if (query.test(item)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    @Override
    public default ImmutableItemContainer<I> queryAll(IEntityQuery<? super I> query) {
        ImmutableItemContainer.Builder<I> builder = ImmutableItemContainer.<I>builder().setName("queryResult");
        this.items().stream().filter(query).forEach(element -> builder.add(element));
        return builder.build();
    }

    public static interface MutableItemContainer<I extends Item> extends ItemContainer<I>, MutableEntityContainer<I> {

        @Override
        public default ImmutableSortedSet<I> items() {
            return ImmutableSortedSet.copyOfSorted(this.cargo());
        }

        @Override
        public default int size() {
            return this.cargo().size();
        }

        @Override
        default boolean isEmpty() {
            return this.cargo().isEmpty();
        }

        @Override
        public default ImmutableItemContainer<I> removeAll(IEntityQuery<? super I> query) {
            ImmutableItemContainer.Builder<I> builder = ImmutableItemContainer.<I>builder().setName("QueryResult");
            for (Iterator<I> iterator = this.cargo().iterator(); iterator.hasNext();) {
                final I ref = iterator.next();
                if (query.test(ref)) {
                    iterator.remove();
                    builder.add(ref);
                }
            }
            return builder.build();
        }

    }

}
