package com.geendutchman.lhf_mudv2.entities.room;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

import org.springframework.stereotype.Repository;

import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

@Repository
public final class RoomRepository implements RoomContainer {
    private final ConcurrentSkipListMap<RoomID, ConcreteRoom> chambers = new ConcurrentSkipListMap<>();

    @Override
    public ImmutableMap<RoomID, Room> roomMap() {
        return ImmutableMap.copyOf(chambers);
    }

    public RoomRepository add(ConcreteRoom... rooms) {
        if (rooms != null) {
            for (final ConcreteRoom room : rooms) {
                if (room != null) {
                    this.chambers.put(room.roomID(), room);
                }
            }
        }
        return this;
    }

    public RoomRepository add(Collection<ConcreteRoom> rooms) {
        if (rooms != null) {
            for (final ConcreteRoom room : rooms) {
                if (room != null) {
                    this.chambers.put(room.roomID(), room);
                }
            }
        }
        return this;
    }

    public RoomRepository addAll(Map<RoomID, ConcreteRoom> rooms) {
        if (rooms != null) {
            this.chambers.putAll(rooms);
        }
        return this;
    }

    public Optional<Room> remove(RoomID id) {
        return Optional.ofNullable(this.chambers.remove(id));
    }

    public Optional<Room> remove(Room room) {
        return this.remove(room.roomID());
    }

    @Override
    public boolean hasRoom(Room room) {
        return this.chambers.containsValue(room);
    }

    @Override
    public Optional<Room> byRoomID(RoomID id) {
        return Optional.ofNullable(this.chambers.get(id));
    }

    @Override
    public String name() {
        return "RoomRepository";
    }

    @Override
    public Stream<Room> rooms() {
        return this.chambers.values().stream().sequential().map(concrete -> (Room) concrete);
    }

    @Override
    public ImmutableSortedMap<String, String> attributes() {
        return ImmutableSortedMap.of();
    }

}
