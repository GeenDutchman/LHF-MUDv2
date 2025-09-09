package com.geendutchman.lhf_mudv2.entities;

import java.net.URI;
import java.util.UUID;

import org.springframework.web.util.UriComponentsBuilder;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.google.common.base.Preconditions;

public interface IEntityID extends Comparable<IEntityID> {
    public String entityClass();

    public String name();

    public UUID uuid();

    public default URI uri() {
        return UriComponentsBuilder.newInstance().pathSegment(this.entityClass()).pathSegment(this.name())
                .pathSegment(this.uuid().toString()).build().toUri();
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
    public record EntityID(String entityClass, String name, UUID uuid) implements IEntityID {

        public EntityID {
            Preconditions.checkNotNull(entityClass, "entity class must not be null");
            Preconditions.checkArgument(!entityClass.isEmpty() && !entityClass.isBlank(),
                    "entity class must not be empty or blank");
            Preconditions.checkNotNull(name, "name must not be null");
            Preconditions.checkArgument(!name.isEmpty() && !name.isBlank(), "name must not be empty or blank");
            Preconditions.checkArgument(Examinable.EXAMINABLE_NAME.matcher(name).matches(), "name must match '%s'",
                    Examinable.EXAMINABLE_NAME.toString());
            Preconditions.checkNotNull(uuid, "uuid must not be null");
        }

    }

}
