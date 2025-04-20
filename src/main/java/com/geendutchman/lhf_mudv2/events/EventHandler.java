package com.geendutchman.lhf_mudv2.events;

public interface EventHandler<E extends Event> {
    Class<E> handledType();

    void handle(E event, EventProcessor processor, EventBus bus);

}
