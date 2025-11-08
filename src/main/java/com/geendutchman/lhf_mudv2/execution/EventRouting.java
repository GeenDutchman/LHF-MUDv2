package com.geendutchman.lhf_mudv2.execution;

import java.util.Optional;

import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery;
import com.google.auto.value.AutoBuilder;
import com.google.common.base.Preconditions;

public record EventRouting(IEntityID sender, IEntityID destination, Optional<IEntityQuery<?>> forwarding)
        implements MessageRouting {
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
        public abstract EventRoutingBuilder setSender(IEntityID sender);

        public abstract EventRoutingBuilder setDestination(IEntityID destination);

        public abstract EventRoutingBuilder setForwarding(Optional<IEntityQuery<?>> forwarding);

        public abstract EventRoutingBuilder setForwarding(IEntityQuery<?> forwarding);

        public abstract IEntityID sender();

        public abstract IEntityID destination();

        public abstract EventRouting build();
    }
}