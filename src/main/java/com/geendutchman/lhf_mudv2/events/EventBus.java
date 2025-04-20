package com.geendutchman.lhf_mudv2.events;

import java.net.URI;

import com.google.common.collect.ImmutableSet;

public interface EventBus {
    public abstract void register(final EventProcessor processor);

    public abstract void unregister(final EventProcessor processor);

    public abstract void publish(final Event event);

    public abstract ImmutableSet<URI> listProcessors();

}
