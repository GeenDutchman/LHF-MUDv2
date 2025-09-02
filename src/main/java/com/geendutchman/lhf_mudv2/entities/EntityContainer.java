package com.geendutchman.lhf_mudv2.entities;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Optional;

import org.springframework.lang.NonNull;

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

    public default Optional<E> queryOne(IEntityQuery<? super E> query) {
        for (final E entity : this.entities()) {
            if (query.test(entity)) {
                return Optional.of(entity);
            }
        }
        return Optional.empty();
    }

    @Override
    public abstract ImmutableSortedMap<String, String> attributes();

    public static interface ImmutableEntityContainer<E extends Entity> extends EntityContainer<E> {

        public static <E extends Entity> ImmutableEntityContainer.ImmutableEntityContainerBuilderStart<E> builder() {
            return ImmutableEntityContainerImpl.<E>builder();
        }

        public static interface ImmutableEntityContainerBuilderStart<E extends Entity> {
            public abstract ImmutableEntityContainerBuilder<E> setComparator(Comparator<? super E> comparator);
        }

        public static interface ImmutableEntityContainerBuilder<E extends Entity> {
            public abstract ImmutableEntityContainerBuilder<E> add(E entity);

            public abstract ImmutableEntityContainerBuilder<E> add(Iterable<E> entities);

            public abstract ImmutableEntityContainerBuilder<E> setName(String name);

            public abstract ImmutableSortedMap.Builder<String, String> attributesBuilder();

            public abstract ImmutableEntityContainer<E> build();
        }
    }

    @AutoValue
    static abstract class ImmutableEntityContainerImpl<E extends Entity> implements ImmutableEntityContainer<E> {

        public static <E extends Entity> ImmutableEntityContainerBuilderStart<E> builder() {
            return new AutoValue_EntityContainer_ImmutableEntityContainerImpl.Builder<>();
        }

        @AutoValue.Builder
        static abstract class ImmutableEntityContainerBuilder<E extends Entity>
                implements ImmutableEntityContainer.ImmutableEntityContainerBuilder<E>,
                ImmutableEntityContainer.ImmutableEntityContainerBuilderStart<E> {
            protected Comparator<? super E> entityComparator;
            private ImmutableSortedSet.Builder<E> cachedEntitiesBuilder;

            @Override
            public ImmutableEntityContainer.ImmutableEntityContainerBuilder<E> setComparator(
                    Comparator<? super E> comparator) {
                this.entityComparator = comparator;
                return this;
            }

            protected abstract ImmutableSortedSet.Builder<E> entitiesBuilder(Comparator<? super E> comparator);

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

            @Override
            public ImmutableEntityContainerBuilder<E> add(E entity) {
                this.entitiesBuilder().add(entity);
                return this;
            }

            @Override
            public ImmutableEntityContainerBuilder<E> add(Iterable<E> entities) {
                this.entitiesBuilder().addAll(entities);
                return this;
            }

            @Override
            public abstract ImmutableEntityContainerBuilder<E> setName(String name);

            public abstract ImmutableEntityContainerImpl<E> build();

        }

    }

    /**
     * Returns a new container that only holds the results of the query
     * 
     * @param query
     * @param comparator
     * @return
     */
    public default ImmutableEntityContainer<E> queryAll(IEntityQuery<? super E> query,
            Comparator<? super E> comparator) {
        ImmutableEntityContainer.ImmutableEntityContainerBuilder<E> builder = ImmutableEntityContainerImpl.<E>builder()
                .setComparator(comparator).setName("queryResult");
        this.entities().stream().filter(query).forEach(element -> builder.add(element));
        return builder.build();
    }

    /**
     * Returns a new container that only holds the results of the query
     * 
     * @param query
     * @return
     */
    public default ImmutableEntityContainer<E> queryAll(IEntityQuery<? super E> query) {
        return this.queryAll(query, this.entities().comparator());
    }

    public static interface MutableEntityContainer<E extends Entity> extends EntityContainer<E> {
        public abstract NavigableSet<E> cargo();

        public default boolean add(@NonNull E reference) {
            return this.cargo().add(reference);
        }

        public default boolean add(Collection<E> references) {
            boolean changed = false;
            if (references != null) {
                for (final E ref : references) {
                    if (ref != null) {
                        changed |= this.cargo().add(ref);
                    }
                }
            }
            return changed;
        }

        public default boolean remove(E ref) {
            return this.cargo().remove(ref);
        }

        public default Optional<E> removeOne(IEntityQuery<? super E> query) {
            for (Iterator<E> iterator = this.cargo().iterator(); iterator.hasNext();) {
                final E ref = iterator.next();
                if (query.test(ref)) {
                    iterator.remove();
                    return Optional.ofNullable(ref);
                }
            }
            return Optional.empty();
        }

        public default ImmutableEntityContainer<E> removeAll(IEntityQuery<? super E> query,
                Comparator<? super E> comparator) {
            ImmutableEntityContainer.ImmutableEntityContainerBuilder<E> builder = ImmutableEntityContainerImpl
                    .<E>builder().setComparator(comparator).setName("QueryResult");
            for (Iterator<E> iterator = this.cargo().iterator(); iterator.hasNext();) {
                final E ref = iterator.next();
                if (query.test(ref)) {
                    iterator.remove();
                    builder.add(ref);
                }
            }
            return builder.build();
        }

        public default ImmutableEntityContainer<E> removeAll(IEntityQuery<? super E> query) {
            return this.removeAll(query, this.cargo().comparator());
        }
    }

}
