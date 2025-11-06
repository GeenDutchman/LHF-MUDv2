package com.geendutchman.lhf_mudv2.execution;

import java.net.URI;

import com.google.auto.value.AutoBuilder;
import com.google.common.base.Preconditions;

public record EventRouting(URI sender, URI destination) implements MessageRouting {
    public EventRouting {
        Preconditions.checkNotNull(sender, "send must not be null");
        Preconditions.checkNotNull(destination, "destination must not be null");
    }

    public static EventRoutingBuilder builder() {
        return new AutoBuilder_EventRouting_EventRoutingBuilder();
    }

    public EventRoutingBuilder toBuilder() {
        return EventRouting.builder().setSender(this.sender).setDestination(this.destination);
    }

    @AutoBuilder(ofClass = EventRouting.class)
    public interface EventRoutingBuilder {
        public abstract EventRoutingBuilder setSender(URI sender);

        public abstract EventRoutingBuilder setDestination(URI destination);

        public abstract URI sender();

        public abstract URI destination();

        public abstract EventRouting build();
    }
}