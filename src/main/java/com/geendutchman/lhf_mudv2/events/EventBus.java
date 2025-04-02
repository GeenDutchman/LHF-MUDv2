package com.geendutchman.lhf_mudv2.events;

import reactor.core.publisher.Sinks;

public interface EventBus {
    public abstract Sinks.Many<Event> eventPoster();

}
