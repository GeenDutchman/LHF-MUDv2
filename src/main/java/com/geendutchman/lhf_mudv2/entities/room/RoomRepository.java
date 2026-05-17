package com.geendutchman.lhf_mudv2.entities.room;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

import org.springframework.stereotype.Repository;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

@Repository
public final class RoomRepository implements RoomContainer {

    private final ConcurrentSkipListMap<RoomID, ConcreteRoom> chambers = new ConcurrentSkipListMap<>();

    @Override
    public ImmutableMap<RoomID, Room> roomMap() {
        return ImmutableMap.copyOf(chambers);
    }

    public synchronized RoomRepository add(ConcreteRoom... rooms) {
        if (rooms != null) {
            for (final ConcreteRoom room : rooms) {
                if (room != null) {
                    this.chambers.put(room.roomID(), room);
                }
            }
        }
        return this;
    }

    public synchronized RoomRepository add(Collection<ConcreteRoom> rooms) {
        if (rooms != null) {
            for (final ConcreteRoom room : rooms) {
                if (room != null) {
                    this.chambers.put(room.roomID(), room);
                }
            }
        }
        return this;
    }

    public synchronized RoomRepository addAll(Map<RoomID, ConcreteRoom> rooms) {
        if (rooms != null) {
            this.chambers.putAll(rooms);
        }
        return this;
    }

    public synchronized Optional<Room> remove(RoomID id) {
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

    final static Examinable.Name ROOM_REPO_NAME = new Examinable.Name("RoomRepository");

    @Override
    public Examinable.Name name() {
        return ROOM_REPO_NAME;
    }

    @Override
    public ImmutableSet<Room> rooms() {
        return ImmutableSet.copyOf(this.chambers.values());
    }

    @Override
    public ImmutableSortedMap<String, String> properties() {
        return ImmutableSortedMap.of();
    }

}
