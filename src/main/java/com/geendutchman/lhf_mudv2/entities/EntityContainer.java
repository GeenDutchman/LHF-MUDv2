package com.geendutchman.lhf_mudv2.entities;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;

public interface EntityContainer<E extends Entity> extends Examinable {
    public abstract ImmutableSortedSet<E> entities();

    public default boolean hasEntity(E entity) {
        return this.entities().contains(entity);
    }

    public default Optional<E> byID(IEntityID id) {
        for (final E entity : this.entities()) {
            if (entity != null && id.equals(entity.identifier())) {
                return Optional.of(entity);
            }
        }
        return Optional.empty();
    }

    public default boolean isEmpty() {
        return this.entities().isEmpty();
    }

    public default int size() {
        return this.entities().size();
    }

    @Override
    default Optional<RichOutput> description() {
        RichOutput.Builder builder = RichOutput.builder().setOnEmpty(Optional.of("It is empty"))
                .setSequenceName(this.name()).setTag(Optional.ofNullable(this.tag()));
        for (final Entity entity : this.entities()) {
            builder.addTaggable(entity);
        }
        return Optional.of(builder.build());
    }

    @Override
    public default String tag() {
        return "Entities";
    }

    @Override
    public default String content() {
        return this.name();
    }

    public default Optional<E> queryOne(IEntityQuery<E> query) {
        for (final E entity : this.entities()) {
            if (query.test(entity)) {
                return Optional.of(entity);
            }
        }
        return Optional.empty();
    }

    @Override
    public abstract ImmutableSortedMap<String, String> attributes();

    @AutoValue
    public static abstract class ImmutableEntityContainer<E extends Entity> implements EntityContainer<E> {

        public static <E extends Entity> BuilderStart<E> builder() {
            return new AutoValue_EntityContainer_ImmutableEntityContainer.Builder<>();
        }

        public interface BuilderStart<E extends Entity> {
            public abstract ImmutableEntityContainerBuilder<E> setComparator(Comparator<E> comparator);
        }

        @AutoValue.Builder
        static abstract class ImmutableEntityContainerBuilder<E extends Entity> implements
                Collector<E, ImmutableEntityContainerBuilder<E>, ImmutableEntityContainerBuilder<E>>, BuilderStart<E> {
            protected Comparator<E> entityComparator;
            private ImmutableSortedSet.Builder<E> cachedEntitiesBuilder;

            @Override
            public ImmutableEntityContainerBuilder<E> setComparator(Comparator<E> comparator) {
                this.entityComparator = comparator;
                return this;
            }

            @Override
            public Supplier<ImmutableEntityContainerBuilder<E>> supplier() {
                return () -> {
                    BuilderStart<E> starter = ImmutableEntityContainer.builder();
                    return starter.setComparator(this.entityComparator);
                };
            }

            @Override
            public BiConsumer<ImmutableEntityContainerBuilder<E>, E> accumulator() {
                return (builder, entity) -> builder.addEntity(entity);
            }

            @Override
            public BinaryOperator<ImmutableEntityContainerBuilder<E>> combiner() {
                return (b1, b2) -> b2.addEntities(b1.build().entities());
            }

            @Override
            public Function<ImmutableEntityContainerBuilder<E>, ImmutableEntityContainerBuilder<E>> finisher() {
                return builder -> builder;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return EnumSet.of(Characteristics.IDENTITY_FINISH);

            }

            protected abstract ImmutableSortedSet.Builder<E> entitiesBuilder(Comparator<E> comparator);

            /**
             * Gets the cachedEntityBuilder
             * 
             * Note: not thread safe!
             * 
             * @return
             */
            protected final ImmutableSortedSet.Builder<E> entitiesBuilder() {
                if (this.cachedEntitiesBuilder != null) {
                    this.cachedEntitiesBuilder = this.entitiesBuilder(this.entityComparator);
                }
                return this.cachedEntitiesBuilder;
            }

            public ImmutableEntityContainerBuilder<E> addEntity(E entity) {
                this.entitiesBuilder().add(entity);
                return this;
            }

            public ImmutableEntityContainerBuilder<E> addEntities(Iterable<E> entities) {
                this.entitiesBuilder().addAll(entities);
                return this;
            }

            public abstract ImmutableEntityContainerBuilder<E> setName(String name);

            protected abstract ImmutableSortedMap.Builder<String, String> attributesBuilder();

            public abstract ImmutableEntityContainer<E> build();

        }

    }

    /**
     * Returns a new container that only holds the results of the query
     * 
     * @param query
     * @param comparator
     * @return
     */
    public default EntityContainer<E> queryAll(IEntityQuery<E> query, Comparator<E> comparator) {
        ImmutableEntityContainer.ImmutableEntityContainerBuilder<E> builder = ImmutableEntityContainer.<E>builder()
                .setComparator(comparator).setName("queryResult");
        return this.entities().stream().filter(query).collect(builder).setName("queryResult").build();
    }

    /**
     * Returns a new container that only holds the results of the query
     * 
     * @param query
     * @return
     */
    public default EntityContainer<E> queryAll(IEntityQuery<E> query) {
        return this.queryAll(query, Entity.getEntityComparator());
    }

}
