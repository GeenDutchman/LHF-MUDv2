package com.geendutchman.lhf_mudv2.entities;

import java.net.URI;
import java.util.UUID;

public interface IEntityID extends Comparable<IEntityID> {
    public String entityClass();

    public UUID uuid();

    public default URI uri() {
        return URI.create(String.format("%s/%s", this.entityClass(), this.uuid()));
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

    public static EntityID ofEntityClass(String entityClass) {
        return new EntityID(entityClass, UUID.randomUUID());
    }

    /**
     * Meant to be composed into a subclass of IEntityID
     */
    record EntityID(String entityClass, UUID uuid) implements IEntityID {
        public final URI uri() {
            return URI.create(String.format("%s/%s", this.entityClass(), this.uuid()));
        }
    }

}
