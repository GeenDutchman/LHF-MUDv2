package com.geendutchman.lhf_mudv2.entities.room;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.graph.SuccessorsFunction;

public interface RoomContainer extends Examinable, SuccessorsFunction<Room> {
    // (e.g. graph of RoomID nodes paired with a hashmap // of RoomID -> Room)
    public abstract ImmutableSet<Room> rooms();

    public abstract ImmutableMap<RoomID, Room> roomMap();

    public abstract boolean hasRoom(Room room);

    public abstract Optional<Room> byRoomID(RoomID id);

    @Override
    public default Optional<RichOutput> description() {
        RichOutput.Builder builder = RichOutput.builder().setOnEmpty(Optional.of("It is empty"))
                .setTag(this.tag() + "-description");
        this.rooms().forEach(room -> builder.addTaggable(room));
        return Optional.of(builder.build());
    }

    final static Taggable.Tag ROOM_CONTAINER_TAG = new Taggable.Tag("Rooms");

    @Override
    public default Taggable.Tag tag() {
        return ROOM_CONTAINER_TAG;
    }

    @Override
    public default String content() {
        return this.name().toString();
    }

    @Override
    public abstract ImmutableSortedMap<String, String> properties();

    public default Optional<Room> queryOneRoom(IEntityQuery<? super Room> query) {
        return this.rooms().stream().sequential().filter(room -> query != null ? query.test(room) : room != null)
                .findFirst();
    }

    public default Optional<Room> queryOneRoom(RoomQuery query) {
        return this.rooms().stream().sequential().filter(room -> query != null ? query.test(room) : room != null)
                .findFirst();
    }

    public default RoomContainer queryRooms(IEntityQuery<? super Room> query) {
        ImmutableSortedMap.Builder<RoomID, Room> builder = ImmutableSortedMap.naturalOrder();
        this.rooms().stream().sequential().filter(room -> query != null ? query.test(room) : room != null)
                .forEachOrdered(room -> builder.put(room.roomID(), room));
        ImmutableSortedMap<RoomID, Room> built = builder.build();
        final Examinable.Name queryName = new Examinable.Name("RoomQueryResult");
        return new RoomContainer() {

            @Override
            public Examinable.Name name() {
                return queryName;
            }

            @Override
            public ImmutableSet<Room> rooms() {
                return ImmutableSet.copyOf(built.values());
            }

            @Override
            public ImmutableMap<RoomID, Room> roomMap() {
                return built;
            }

            @Override
            public boolean hasRoom(Room room) {
                return built.containsValue(room);
            }

            @Override
            public Optional<Room> byRoomID(RoomID id) {
                return Optional.ofNullable(built.get(id));
            }

            @Override
            public ImmutableSortedMap<String, String> properties() {
                return ImmutableSortedMap.of();
            }

        };
    }

    @Override
    public default Iterable<? extends Room> successors(Room node) {
        if (node == null) {
            return Set.of();
        }
        final LinkedHashSet<Room> next = new LinkedHashSet<>();
        final ImmutableSortedMap<Directions, Doorway> outs = node.doorways();
        if (outs == null) {
            return next;
        }
        for (final Doorway door : outs.values()) {
            if (door == null || door.target() == null) {
                continue;
            }
            Optional<Room> room = this.byRoomID(door.target());
            room.ifPresent(r -> next.add(r));
        }
        return next;
    }

}
