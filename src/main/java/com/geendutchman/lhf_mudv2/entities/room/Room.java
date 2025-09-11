package com.geendutchman.lhf_mudv2.entities.room;

import java.io.Serializable;
import java.net.URI;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.Item.LockedItemBuilder;
import com.geendutchman.lhf_mudv2.entities.item.ItemContainer;
import com.geendutchman.lhf_mudv2.entities.item.ItemInventory;
import com.geendutchman.lhf_mudv2.events.EventBus;
import com.geendutchman.lhf_mudv2.events.EventProcessor;
import com.google.auto.value.AutoBuilder;
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

    /**
     * The room name must adhere to this regex
     */
    public static final Pattern ROOMNAME_RULES = Examinable.EXAMINABLE_NAME;

    @Override
    default Taggable.Tag tag() {
        return new Taggable.Tag("ROOM");
    }

    // Rooms can hold items in an inventory
    public abstract ItemInventory inventory();

    public abstract Optional<RichOutput> roomDescription();

    @Override
    public default String content() {
        return this.name();
    }

    @AutoOneOf(Delta.Kind.class)
    public static abstract class Delta implements Serializable {
        public enum Kind {
            ITEM, ITEMBUILDER
        }

        public abstract Kind kind();

        public abstract Optional<Item> item();

        public abstract Optional<Item.LockedItemBuilder> itemBuilder();

        public static Delta ofItem(Item item) {
            return AutoOneOf_Room_Delta.item(Optional.of(item));
        }

        public static Delta ofItemBuilder(Item.BuildItem itemBuilder) {
            return AutoOneOf_Room_Delta.itemBuilder(Optional.of(itemBuilder.lock()));
        }

        public static Delta ofItemBuilder(Item.LockedItemBuilder itemBuilder) {
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

    public static sealed interface BuilderStart extends Serializable permits Room.BuildRoom {
        public Room.BuildRoom setName(String name);

        @Autowired
        public BuilderStart setRoomRepository(RoomRepository roomRepository);

        @Autowired
        public BuilderStart setEventBus(EventBus eventBus);
    }

    public static sealed interface BuildRoom extends BuilderStart permits Room.Builder {
        public String getName();

        public BuildRoom setLocale(Optional<URI> locale);

        public BuildRoom setRoomDescription(Optional<RichOutput> roomDescription);

        public BuildRoom addItem(LockedItemBuilder... builder);

        public BuildRoom setEventFunction(@Nullable EventProcessor.EventFunction<Room> eventFunction);

        @Autowired
        public BuildRoom setRoomRepository(RoomRepository roomRepository);

        @Autowired
        public BuildRoom setEventBus(EventBus eventBus);

        public Room build();
    }

    @AutoBuilder(callMethod = "buildRoom", ofClass = Room.class)
    public abstract non-sealed static class Builder implements BuildRoom {
        final private UUID builderUuid = UUID.randomUUID();

        protected Builder() {
        }

        public final UUID builderUuid() {
            return this.builderUuid;
        }

        public abstract ItemInventory.Builder inventoryBuilder();

        // public abstract BuildRoom setInventory(ItemInventory inv);

        @Override
        public final BuildRoom addItem(LockedItemBuilder... builder) {
            final ItemInventory.Builder set = this.inventoryBuilder();
            set.addContents(builder);
            return this;
        }

        abstract Room autoBuild();

        @Override
        public final Room build() {
            Preconditions.checkState(Room.EXAMINABLE_NAME.matcher(this.getName()).matches(),
                    "Room name '%s' must match '%s'", this.getName(), Room.EXAMINABLE_NAME.toString());
            return this.autoBuild();
        }
    }

    public static Room.BuilderStart builder() {
        final Room.Builder builder = new AutoBuilder_Room_Builder();
        return builder;
    }

    public static Room buildRoom(EventBus eventBus, RoomRepository roomRepository, String name,
            Optional<RichOutput> roomDescription, Optional<URI> locale, ItemInventory inventory,
            @Nullable EventProcessor.EventFunction<Room> eventFunction) {
        Preconditions.checkNotNull(eventBus, "event bus must not be null");
        Preconditions.checkNotNull(roomRepository, "room repository must be available to store room into");
        final ConcreteRoom room = ConcreteRoom.buildRoom(name, roomDescription, locale, inventory, eventFunction);
        eventBus.register(room);
        roomRepository.add(room);
        return room;
    }
}
