package com.geendutchman.lhf_mudv2.entities.room;

import java.io.Serializable;
import java.net.URI;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemContainer;
import com.geendutchman.lhf_mudv2.entities.item.ItemInventory;
import com.google.auto.value.AutoOneOf;
import com.google.common.base.Preconditions;

public interface Room extends Entity, ItemContainer {
    public record RoomID(EntityID delegate) implements IEntityID {

        public RoomID {
            Preconditions.checkNotNull(delegate, "RoomID should not have a null delegate");
            Preconditions.checkState(delegate.entityClass().equals("rooms"),
                    "an room id must be about rooms, but was %s", delegate.entityClass());
        }

        public static RoomID make(String name) {
            return new RoomID(new EntityID("rooms", name, UUID.randomUUID()));
        }

        public String entityClass() {
            return this.delegate.entityClass();
        }

        public String name() {
            return this.delegate.name();
        }

        public URI uri() {
            return this.delegate.uri();
        }

        @Override
        public UUID uuid() {
            return this.delegate.uuid();
        }

        @Override
        public String toString() {
            return this.uri().toString();
        }

    }

    /**
     * A uuid to specify the room
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

    // Rooms can hold items in an inventory
    public abstract ItemInventory inventory();

    public abstract Optional<RichOutput> roomDescription();

    @Override
    public default String content() {
        return this.name().toString();
    }

    @AutoOneOf(Delta.Kind.class)
    public static abstract class Delta implements Serializable {
        public enum Kind {
            ITEM, ITEMBUILDER
        }

        public abstract Kind kind();

        public abstract Optional<Item> item();

        public abstract Optional<ItemBuilderFactory.LockedItemBuilder> itemBuilder();

        public static Delta ofItem(Item item) {
            return AutoOneOf_Room_Delta.item(Optional.of(item));
        }

        public static Delta ofItemBuilder(ItemBuilderFactory.BuildItem itemBuilder) {
            return AutoOneOf_Room_Delta.itemBuilder(Optional.of(itemBuilder.lock()));
        }

        public static Delta ofItemBuilder(ItemBuilderFactory.LockedItemBuilder itemBuilder) {
            return AutoOneOf_Room_Delta.itemBuilder(Optional.of(itemBuilder));
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
