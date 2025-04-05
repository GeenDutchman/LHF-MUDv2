package com.geendutchman.lhf_mudv2.item;

import java.io.Serializable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collector;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;

public interface ItemContainer extends Examinable {

    @AutoValue
    public static abstract class Query implements Predicate<Item> {
        public abstract Optional<UUID> uuid();

        public abstract Optional<String> name();

        public abstract ImmutableList<Pattern> namePatterns();

        public abstract Optional<Boolean> isVisible();

        public abstract boolean checkOnlyName();

        public abstract ImmutableList<Pattern> toStringPatterns();

        public final static Builder builder() {
            final Builder builder = new AutoValue_ItemContainer_Query.Builder().setIsVisible(true)
                    .setCheckOnlyName(false).setToStringPatterns(ImmutableList.of());
            builder.namePatternsBuilder();
            return builder;
        }

        abstract Builder toBuilder();

        public final Query withNamePattern(String pattern) {
            return toBuilder().addNamePattern(pattern).build();
        }

        public final Query withCheckOnlyName(boolean check) {
            return toBuilder().setCheckOnlyName(check).build();
        }

        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder setUuid(Optional<UUID> uuid);

            public abstract Builder setName(Optional<String> name);

            public abstract Builder setName(String name);

            abstract ImmutableList.Builder<Pattern> namePatternsBuilder();

            public final Builder addNamePattern(Pattern pattern) {
                this.namePatternsBuilder().add(pattern);
                return this;
            }

            public final Builder addNamePattern(String pattern) {
                this.namePatternsBuilder().add(Pattern.compile(pattern));
                return this;
            }

            public abstract Builder setIsVisible(boolean isVisible);

            public abstract Builder setIsVisible(Optional<Boolean> isVisible);

            public abstract Builder setCheckOnlyName(boolean check);

            public abstract Builder setToStringPatterns(ImmutableList<Pattern> patterns);

            public abstract Query build();

        }

        private final boolean testNames(Item t) {
            if (!this.checkOnlyName()) {
                if (this.name().isPresent()) {
                    if (!this.name().get().equals(t.displayName())) {
                        return false;
                    }
                }
                for (final Pattern pattern : this.namePatterns()) {
                    if (!pattern.asPredicate().test(t.displayName())) {
                        return false;
                    }
                }
            } else {
                if (this.name().isPresent()) {
                    if (!this.name().get().equals(t.name())) {
                        return false;
                    }
                }
                for (final Pattern pattern : this.namePatterns()) {
                    if (!pattern.asPredicate().test(t.name())) {
                        return false;
                    }
                }
            }

            return true;
        }

        @Override
        public final boolean test(Item t) {
            if (t == null) {
                return false;
            }
            if (this.uuid().isPresent()) {
                if (this.uuid().get().equals(t.uuid())) {
                    return true;
                }
            }
            if (this.isVisible().isPresent()) {
                if (t.isVisible() != this.isVisible().get()) {
                    return false;
                }
            }
            if (!this.testNames(t)) {
                return false;
            }
            final ImmutableList<Pattern> toStringPatterns = this.toStringPatterns();
            if (toStringPatterns.size() > 0) {
                final String asStr = t.toString();
                for (final Pattern pattern : this.toStringPatterns()) {
                    if (!pattern.asMatchPredicate().test(asStr)) {
                        return false;
                    }
                }
            }
            return true;
        }

    }

    public abstract ImmutableSortedSet<Item> items();

    public default boolean hasItem(Item item) {
        return this.items().contains(item);
    }

    public default Optional<Item> byUUID(UUID id) {
        for (final Item item : this.items()) {
            if (item != null && id.equals(item.uuid())) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    public default boolean isEmpty() {
        return this.items().isEmpty();
    }

    public default int size() {
        return this.items().size();
    }

    @Override
    public default Optional<RichOutput> description() {
        RichOutput.Builder builder = RichOutput.builder().setOnEmpty(Optional.of("It is empty"))
                .setSequenceName(this.name()).setTag(Optional.of(this.tag()));
        for (final Item item : this.items()) {
            builder.addTaggable(item);
        }
        return Optional.of(builder.build());
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

    public default Optional<Item> queryOne(Query query) {
        for (final Item item : this.items()) {
            if (query.test(item)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    public default ImmutableItemContainer queryAll(Query query) {
        ImmutableItemContainer.Builder builder = ImmutableItemContainer.builder().setName("queryResult");
        return this.items().stream().filter(query).collect(builder).setName("queryResult").build();
    }
}
