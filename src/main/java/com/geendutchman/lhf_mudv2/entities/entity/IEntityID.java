package com.geendutchman.lhf_mudv2.entities.entity;

import java.net.URI;
import java.util.UUID;

import org.springframework.web.util.UriComponentsBuilder;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.google.common.base.Preconditions;

public interface IEntityID extends Comparable<IEntityID>, Taggable {
    public Taggable.Tag entityClass();

    public Examinable.Name name();

    public UUID uuid();

    public default URI uri() {
        return UriComponentsBuilder.newInstance().pathSegment("{class}").pathSegment("{name}")
                .pathSegment(this.uuid().toString()).build(this.entityClass(), this.name());
    }

    @Override
    public default Tag tag() {
        return this.entityClass();
    }

    @Override
    public default String content() {
        return this.name().toString();
    }

    @Override
    default int compareTo(IEntityID o) {
        if (o == null) {
            throw new NullPointerException("cannot compare to nil EntityID");
        }
        if (this == o) {
            return 0;
        }
        int classCompare = this.entityClass().compareTo(o.entityClass());
        if (classCompare != 0) {
            return classCompare;
        }
        return this.uuid().compareTo(o.uuid());
    }

    /**
     * Meant to be composed into a subclass of IEntityID
     */
    public record EntityID(Taggable.Tag entityClass, Examinable.Name name, UUID uuid) implements IEntityID {

        public EntityID {
            Preconditions.checkNotNull(entityClass, "entity class must not be null");
            Preconditions.checkNotNull(name, "name must not be null");
            Preconditions.checkNotNull(uuid, "uuid must not be null");
        }

    }

}
