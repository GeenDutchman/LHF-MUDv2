package com.geendutchman.lhf_mudv2.entities.item;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.events.EventBus;

@Component
public final class ItemBuilderFactory {

    private final ItemRepository repository;
    private final EventBus bus;

    @Autowired
    public ItemBuilderFactory(ItemRepository repository, EventBus bus) {
        this.repository = repository;
        this.bus = bus;
    }

    @Bean({ "itembuilder", "itemBuilder" })
    @Scope("prototype")
    public Item.BuilderStart builder() {
        final Item.BuilderStart builder = Item.builder().setItemRepository(repository).setEventBus(bus);
        return builder;
    }

    public ItemReference build(Item.Builder builder) {
        return builder.setItemRepository(repository).setEventBus(bus).build();
    }

    @Bean
    public Item aRock() {
        return this.builder().setName("defaultRock").build();
    }

}