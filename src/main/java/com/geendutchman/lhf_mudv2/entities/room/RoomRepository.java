package com.geendutchman.lhf_mudv2.entities.room;

import java.util.NavigableMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.springframework.stereotype.Repository;

import com.geendutchman.lhf_mudv2.entities.EntityRepository;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.google.common.collect.ImmutableSortedMap;

@Repository
public final class RoomRepository
        implements RoomContainer.MutableRoomContainer<ConcreteRoom>, EntityRepository<ConcreteRoom> {
    private final ConcurrentNavigableMap<RoomID, ConcreteRoom> chambers = new ConcurrentSkipListMap<>();

    @Override
    public String name() {
        return "RoomRepository";
    }

    @Override
    public NavigableMap<RoomID, ConcreteRoom> chambers() {
        return this.chambers;
    }

    @Override
    public ImmutableSortedMap<String, String> attributes() {
        return ImmutableSortedMap.of();
    }

}
