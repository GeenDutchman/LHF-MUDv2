package com.geendutchman.lhf_mudv2.events;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public interface EventProcessor {
    public String processorId();

    public void listen(Flux<Event> eventStream);

    abstract Sinks.Many<Event> sink();

    public default Flux<Event> eventsOut() {
        return this.sink().asFlux().name(this.processorId());
    }
}
