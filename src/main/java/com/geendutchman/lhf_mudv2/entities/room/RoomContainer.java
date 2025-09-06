package com.geendutchman.lhf_mudv2.entities.room;

import java.util.Optional;
import java.util.stream.Stream;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.IEntityQuery;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

public interface RoomContainer extends Examinable {
    // TODO: change this to graphs
    public abstract Stream<Room> rooms();

    public abstract ImmutableMap<RoomID, Room> roomMap();

    public abstract boolean hasRoom(Room room);

    public abstract Optional<Room> byRoomID(RoomID id);

    @Override
    public default Optional<RichOutput> description() {
        RichOutput.Builder builder = RichOutput.builder().setOnEmpty(Optional.of("It is empty"))
                .setTag(Optional.ofNullable(this.tag() + "-description"));
        this.rooms().forEachOrdered(room -> builder.addTaggable(room));
        return Optional.of(builder.build());
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

    public default Optional<Room> queryOneRoom(IEntityQuery<? super Room> query) {
        return this.rooms().sequential().filter(room -> query != null ? query.test(room) : room != null).findFirst();
    }

    public default Optional<Room> queryOneRoom(RoomQuery query) {
        return this.rooms().sequential().filter(room -> query != null ? query.test(room) : room != null).findFirst();
    }

    public default RoomContainer queryRooms(IEntityQuery<? super Room> query) {
        ImmutableSortedMap.Builder<RoomID, Room> builder = ImmutableSortedMap.naturalOrder();
        this.rooms().sequential().filter(room -> query != null ? query.test(room) : room != null)
                .forEachOrdered(room -> builder.put(room.roomID(), room));
        ImmutableSortedMap<RoomID, Room> built = builder.build();
        return new RoomContainer() {

            @Override
            public String name() {
                return "RoomQueryResult";
            }

            @Override
            public Stream<Room> rooms() {
                return built.values().stream().sequential();
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
            public ImmutableSortedMap<String, String> attributes() {
                return ImmutableSortedMap.of();
            }

        };
    }

}
