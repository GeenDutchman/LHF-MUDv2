package com.geendutchman.lhf_mudv2.entities.item;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import com.geendutchman.lhf_mudv2.entities.EntityContainer;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;

public interface ItemContainer extends EntityContainer<Item> {

    public abstract ImmutableSortedSet<Item> items();

    @Override
    public default ImmutableSortedSet<Item> entities() {
        return this.items();
    }

    public default boolean hasItem(Item item) {
        return this.items().contains(item);
    }

    public default Optional<Item> byItemID(ItemID id) {
        for (final Item item : this.items()) {
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
    public static abstract class ImmutableItemContainer implements ItemContainer {

        public final static Builder builder() {
            return new AutoValue_ItemContainer_ImmutableItemContainer.Builder();
        }

        @AutoValue.Builder
        static abstract class Builder implements Collector<Item, Builder, Builder> {

            @Override
            public final Supplier<Builder> supplier() {
                return ImmutableItemContainer::builder;
            }

            @Override
            public final BiConsumer<Builder, Item> accumulator() {
                return (builder, item) -> builder.addItem(item);
            }

            @Override
            public final BinaryOperator<Builder> combiner() {
                return (b1, b2) -> b2.addItems(b1.build().items());
            }

            @Override
            public final Function<Builder, Builder> finisher() {
                return builder -> builder;
            }

            @Override
            public final Set<Characteristics> characteristics() {
                return EnumSet.of(Characteristics.IDENTITY_FINISH);
            }

            final private static Comparator<Item> itemComparator = Item.getItemComparator();

            private ImmutableSortedSet.Builder<Item> cachedItemsBuilder;

            abstract ImmutableSortedSet.Builder<Item> itemsBuilder(Comparator<Item> comparator);

            /**
             * Gets the cachedItemsBuilder
             * 
             * Note: not thread safe!
             * 
             * @return
             */
            private final ImmutableSortedSet.Builder<Item> itemsBuilder() {
                if (this.cachedItemsBuilder == null) {
                    this.cachedItemsBuilder = this.itemsBuilder(itemComparator);
                }
                return this.cachedItemsBuilder;
            }

            public final Builder addItem(Item item) {
                this.itemsBuilder().add(item);
                return this;
            }

            public final Builder addItems(Iterable<Item> items) {
                this.itemsBuilder().addAll(items);
                return this;
            }

            public abstract Builder setName(String name);

            abstract ImmutableSortedMap.Builder<String, String> attributesBuilder();

            public abstract ImmutableItemContainer build();
        }
    }

    public default Optional<Item> queryOne(ItemQuery query) {
        for (final Item item : this.items()) {
            if (query.test(item)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    public default ImmutableItemContainer queryAll(ItemQuery query) {
        ImmutableItemContainer.Builder builder = ImmutableItemContainer.builder().setName("queryResult");
        return this.items().stream().filter(query).collect(builder).setName("queryResult").build();
    }

}
