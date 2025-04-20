package com.geendutchman.lhf_mudv2.events;

import java.net.URI;
import java.util.Optional;

public interface EventProcessor {
    public URI processorURI();

    public Optional<URI> locale();

    public default void processEvent(Event event, EventBus bus) {
        // give us the option
    }

}
