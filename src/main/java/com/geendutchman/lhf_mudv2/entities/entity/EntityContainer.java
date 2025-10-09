package com.geendutchman.lhf_mudv2.entities.entity;

import java.util.Optional;
import java.util.concurrent.ConcurrentNavigableMap;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.google.common.collect.ImmutableSortedMap;

public interface EntityContainer extends Examinable {
    public abstract ConcurrentNavigableMap<IEntityID, Entity> entities();

    public default boolean hasEntity(Entity entity) {
        return this.entities().containsValue(entity);
    }

    public default Optional<Entity> byID(IEntityID id) {
        return Optional.ofNullable(this.entities().getOrDefault(id, null));
    }

    @Override
    public default Optional<RichOutput> description() {
        RichOutput.Builder builder = RichOutput.builder().setOnEmpty(Optional.of("It is empty"))
                .setTag(this.tag() + "-description");
        for (final Entity entity : this.entities().values()) {
            builder.addTaggable(entity);
        }
        return Optional.of(builder.build());
    }

    final static Taggable.Tag ENTITY_CONTAINER_TAG = new Taggable.Tag("Entities");

    @Override
    public default Taggable.Tag tag() {
        return ENTITY_CONTAINER_TAG;
    }

    @Override
    public default String content() {
        return this.name().toString();
    }

    public default Optional<Entity> queryOneEntity(IEntityQuery<? super Entity> query) {
        if (query != null) {
            for (final Entity entity : this.entities().values()) {
                if (query.test(entity)) {
                    return Optional.of(entity);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public abstract ImmutableSortedMap<String, String> attributes();

    /**
     * Returns an immutable map that only holds the results of the query
     * 
     * @param query
     * @param comparator
     * @return
     */
    public default ImmutableSortedMap<IEntityID, Entity> queryEntities(IEntityQuery<? super Entity> query) {
        if (query == null) {
            return ImmutableSortedMap.copyOf(this.entities());
        }
        ImmutableSortedMap.Builder<IEntityID, Entity> builder = ImmutableSortedMap.naturalOrder();
        this.entities().entrySet().stream().filter(entry -> query.test(entry.getValue()))
                .forEach(element -> builder.put(element));
        return builder.build();
    }

}
