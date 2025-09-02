package com.geendutchman.lhf_mudv2.entities.room;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.events.EventBus;

@Component
public final class RoomBuilderFactory {
    private final RoomRepository repository;
    private final EventBus bus;

    @Autowired
    public RoomBuilderFactory(RoomRepository repository, EventBus bus) {
        this.repository = repository;
        this.bus = bus;
    }

    @Bean
    @Scope("prototype")
    public Room.BuilderStart builder() {
        final Room.BuilderStart builder = Room.builder().setEventBus(bus).setRoomRepository(repository);
        return builder;
    }
}
