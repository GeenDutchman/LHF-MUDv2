package com.geendutchman.lhf_mudv2.entities.room;

import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.Optional;

import org.springframework.lang.NonNull;

import com.geendutchman.lhf_mudv2.entities.EntityContainer;
import com.geendutchman.lhf_mudv2.entities.IEntityID;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;

public interface RoomContainer<R extends Room> extends EntityContainer<R> {
    public abstract ImmutableSortedMap<RoomID, R> rooms();

    @Override
    public default ImmutableSortedSet<R> entities() {
        return ImmutableSortedSet.copyOf(Room.getRoomComparator(), this.rooms().values());
    }

    @Override
    public default boolean hasEntity(R entity) {
        return this.rooms().values().contains(entity);
    }

    @Override
    public default Optional<R> byID(IEntityID id) {
        return Optional.ofNullable(this.rooms().getOrDefault(id, null));
    }

    @Override
    public default boolean isEmpty() {
        return this.rooms().isEmpty();
    }

    @Override
    public default int size() {
        return this.rooms().size();
    }

    public default boolean hasRoom(Room room) {
        return this.rooms().values().contains(room);
    }

    public default Optional<Room> byRoomID(RoomID id) {
        return Optional.ofNullable(this.rooms().get(id));
    }

    @Override
    public default String tag() {
        return "Rooms";
    }

    @Override
    public default String content() {
        return this.name();
    }

    @Override
    public abstract ImmutableSortedMap<String, String> attributes();

    @AutoValue
    public static abstract class ImmutableRoomContainer<R extends Room>
            implements RoomContainer<R>, EntityContainer.ImmutableEntityContainer<R> {
        public final static <R extends Room> Builder<R> builder() {
            return new AutoValue_RoomContainer_ImmutableRoomContainer.Builder<>();
        }

        @AutoValue.Builder
        static abstract class Builder<R extends Room> implements ImmutableEntityContainerBuilder<R> {

            abstract ImmutableSortedMap.Builder<RoomID, R> roomsBuilder();

            public final Builder<R> add(R room) {
                this.roomsBuilder().put(room.roomID(), room);
                return this;
            }

            public final Builder<R> add(Iterable<R> rooms) {
                return this.addRooms(rooms);
            }

            public final Builder<R> addRooms(Iterable<R> rooms) {
                final ImmutableSortedMap.Builder<RoomID, R> b = this.roomsBuilder();
                for (R room : rooms) {
                    b.put(room.roomID(), room);
                }
                return this;
            }

            public abstract Builder<R> setName(String name);

            public abstract ImmutableRoomContainer<R> build();
        }
    }

    public default Optional<Room> queryOne(RoomQuery query) {
        for (final Room room : this.rooms().values()) {
            if (query.test(room)) {
                return Optional.of(room);
            }
        }
        return Optional.empty();
    }

    public default ImmutableRoomContainer<R> queryAll(RoomQuery query) {
        ImmutableRoomContainer.Builder<R> builder = ImmutableRoomContainer.<R>builder().setName("queryResult");
        this.rooms().values().stream().filter(query).forEach(element -> builder.add(element));
        return builder.build();
    }

    public static interface MutableRoomContainer<R extends Room> extends RoomContainer<R>, MutableEntityContainer<R> {
        public abstract NavigableMap<RoomID, R> chambers();

        @Override
        public default ImmutableSortedMap<RoomID, R> rooms() {
            return ImmutableSortedMap.copyOf(this.chambers());
        }

        @Override
        public default ImmutableSortedSet<R> cargo() {
            return ImmutableSortedSet.copyOf(Room.getRoomComparator(), this.chambers().values());
        }

        @Override
        public default boolean add(@NonNull R reference) {
            return this.chambers().put(reference.roomID(), reference) == null;
        }

        @Override
        public default boolean add(Collection<R> references) {
            boolean changed = false;
            if (references != null) {
                for (final R ref : references) {
                    if (ref != null) {
                        changed |= this.chambers().put(ref.roomID(), ref) == null;
                    }
                }
            }
            return changed;
        }

        @Override
        public default int size() {
            return this.chambers().size();
        }

        @Override
        default boolean isEmpty() {
            return this.chambers().isEmpty();
        }

        @Override
        public default boolean remove(R ref) {
            return this.chambers().remove(ref.roomID()) != null;
        }

        public default Optional<R> removeOne(RoomQuery query) {
            for (Iterator<R> iterator = this.chambers().values().iterator(); iterator.hasNext();) {
                final R ref = iterator.next();
                if (query.test(ref)) {
                    iterator.remove();
                    return Optional.ofNullable(ref);
                }
            }
            return Optional.empty();
        }

        public default ImmutableRoomContainer<R> removeAll(RoomQuery query) {
            ImmutableRoomContainer.Builder<R> builder = ImmutableRoomContainer.<R>builder().setName("QueryResult");
            for (Iterator<R> iterator = this.chambers().values().iterator(); iterator.hasNext();) {
                final R ref = iterator.next();
                if (query.test(ref)) {
                    iterator.remove();
                    builder.add(ref);
                }
            }
            return builder.build();
        }
    }
}
