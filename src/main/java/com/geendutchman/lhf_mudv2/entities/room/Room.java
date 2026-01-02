package com.geendutchman.lhf_mudv2.entities.room;

import java.io.Serializable;
import java.net.URI;
import java.util.Comparator;
import java.util.Optional;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureContainer;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemContainer;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

public interface Room extends Entity, ItemContainer, CreatureContainer {
    public record RoomID(EntityID delegate) implements IEntityID {

        public RoomID {
            Preconditions.checkNotNull(delegate, "RoomID should not have a null delegate");
            Preconditions.checkState(delegate.entityClass().equals(ENTITY_CLASS_ROOM),
                    "an room id must be about rooms, but was %s", delegate.entityClass());
        }

        public static final Taggable.Tag ENTITY_CLASS_ROOM = new Tag("rooms");
        protected static final TsidFactory tsidFactory = TsidFactory
                .newInstance1024(Math.abs("rooms".hashCode() % 1024));

        public static RoomID make(Examinable.Name name) {
            return new RoomID(new EntityID(ENTITY_CLASS_ROOM, name, tsidFactory.create()));
        }

        public Taggable.Tag entityClass() {
            return this.delegate.entityClass();
        }

        public Examinable.Name name() {
            return this.delegate.name();
        }

        public URI uri() {
            return this.delegate.uri();
        }

        @Override
        public Tsid tsid() {
            return this.delegate.tsid();
        }

        @Override
        public String toString() {
            return this.uri().toString();
        }

    }

    /**
     * An identifer to specify the room
     */
    @Override
    public default IEntityID identifier() {
        return this.roomID();
    }

    public abstract RoomID roomID();

    final static Taggable.Tag ROOM_TAG = new Taggable.Tag("ROOM");

    @Override
    default Taggable.Tag tag() {
        return ROOM_TAG;
    }

    /**
     * Shows how this Room is connected to other Rooms
     * 
     * @return
     */
    public abstract ImmutableSortedMap<Directions, Doorway> doorways();

    @Override
    public default ImmutableSortedMap<String, String> attributes() {
        return ImmutableSortedMap.<String, String>naturalOrder().putAll(Entity.super.attributes()).build();
    }

    @Override
    public abstract Optional<RichOutput> description();

    @Override
    public default String content() {
        return this.name().toString();
    }

    public sealed static interface Delta extends Serializable {

        public record AddItemDelta(Item item) implements Delta {
            public AddItemDelta {
                Preconditions.checkNotNull(item, "item to add must not be null");
            }
        }

        public record RemoveItemDelta(Item item) implements Delta {
            public RemoveItemDelta {
                Preconditions.checkNotNull(item, "item to remove must not be null");
            }
        }

        public record AddCreatureDelta(Creature creature) implements Delta {
            public AddCreatureDelta {
                Preconditions.checkNotNull(creature, "creature to add must not be null");
            }
        }

        public record RemoveCreatureDelta(Creature creature) implements Delta {
            public RemoveCreatureDelta {
                Preconditions.checkNotNull(creature, "creature to remove must not be null");
            }
        }

        public record AddDoorway(Directions to, Doorway doorway) implements Delta {
            public AddDoorway {
                Preconditions.checkNotNull(to, "direction to doorway must not be null");
                Preconditions.checkNotNull(doorway, "doorway must not be null");
            }
        }

        public record RemoveDoorway(Directions to) implements Delta {
            public RemoveDoorway {
                Preconditions.checkNotNull(to, "direction from which to remove the doorway must not be null");
            }
        }

        public static Delta ofItemToAdd(Item item) {
            return new AddItemDelta(item);
        }

        public static Delta ofItemToRemove(Item item) {
            return new RemoveItemDelta(item);
        }

        public static Delta ofCreatureToAdd(Creature creature) {
            return new AddCreatureDelta(creature);
        }

        public static Delta ofCreatureToRemove(Creature creature) {
            return new RemoveCreatureDelta(creature);
        }

    }

    public abstract void applyDelta(Delta delta);

    public static class RoomComparator implements Comparator<Room>, Serializable {
        private static Comparator<Entity> delegate = Entity.getEntityComparator();

        @Override
        public int compare(Room o1, Room o2) {
            if (o1 == null || o2 == null) {
                throw new NullPointerException("Cannot compare null Rooms");
            }
            if (o1.equals(o2)) {
                return 0;
            }
            return delegate.compare(o1, o2);
        }
    }

    public static Comparator<Room> getRoomComparator() {
        return new RoomComparator();
    }

}
