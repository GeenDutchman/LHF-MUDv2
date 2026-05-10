package com.geendutchman.lhf_mudv2.entities.entity;

import java.net.URI;

import org.springframework.web.util.UriComponentsBuilder;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidFactory;
import com.google.common.base.Preconditions;

public interface IEntityID extends Comparable<IEntityID>, Taggable {
    public Taggable.Tag entityClass();

    public Examinable.Name name();

    public Tsid tsid();

    public default URI uri() {
        return UriComponentsBuilder.newInstance().pathSegment("{class}").pathSegment("{name}")
                .pathSegment(this.tsid().toString()).build(this.entityClass(), this.name());
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
        int nameCompare = this.name().compareTo(o.name());
        if (nameCompare != 0) {
            return nameCompare;
        }
        return this.tsid().compareTo(o.tsid());
    }

    public final static IEntityID BLANK_ID = new EntityID(new Taggable.Tag("blank"), new Examinable.Name("blank"),
            TsidFactory.newInstance1024(0).create());

    /**
     * Meant to be composed into a subclass of IEntityID
     */
    public record EntityID(Taggable.Tag entityClass, Examinable.Name name, Tsid tsid) implements IEntityID {

        public EntityID {
            Preconditions.checkNotNull(entityClass, "entity class must not be null");
            Preconditions.checkNotNull(name, "name must not be null");
            Preconditions.checkNotNull(tsid, "tsid must not be null");
        }

        public static EntityID fromString(final String value) {
            if (value == null) {
                throw new IllegalArgumentException("Could not craft entity ID from null string");
            }
            final String[] splitten = value.strip().split("/", 0);
            Preconditions.checkArgument(splitten.length == 3,
                    "An ID should be three parts separated by a '/', recieved parts '%s' -> '%s'", value, splitten);
            try {
                final Taggable.Tag entityClass = new Taggable.Tag(splitten[0]);
                final Examinable.Name name = new Examinable.Name(splitten[1]);
                final Tsid tsid = Tsid.from(splitten[2]);
                final EntityID id = new EntityID(entityClass, name, tsid);
                return id;
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(String.format("Could not craft entity ID from '%s'", value), e);
            }
        }

        @Override
        public final String toString() {
            return this.uri().toString();
        }

    }

}
