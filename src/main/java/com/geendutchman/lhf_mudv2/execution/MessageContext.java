package com.geendutchman.lhf_mudv2.execution;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;

public final class MessageContext implements Serializable {
    private final UUID uuid = UUID.randomUUID();
    private final LinkedHashMap<Taggable.Tag, IEntityID> destinationTrace = new LinkedHashMap<>();
    private final IEntityID sender;
    private final IEntityQuery<?> forwardingRestrictions;

    private MessageContext(IEntityID sender, IEntityID destination, IEntityQuery<?> forwarding) {
        Preconditions.checkNotNull(sender, "sender must not be null");
        Preconditions.checkNotNull(destination, "destination must not be null");
        this.sender = sender;
        this.forwardingRestrictions = forwarding;
        this.destinationTrace.put(destination.entityClass(), destination);
    }

    public static MessageContext create(IEntityID sender, IEntityID destination) {
        return new MessageContext(sender, destination, null);
    }

    public synchronized MessageContext forward(IEntityID destination) {
        this.destinationTrace.put(destination.entityClass(), destination);
        return this;
    }

    public UUID uuid() {
        return uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public synchronized ImmutableBiMap<Taggable.Tag, IEntityID> getDestinationTrace() {
        return ImmutableBiMap.copyOf(destinationTrace);
    }

    public synchronized IEntityID destination() {
        return this.destinationTrace.lastEntry().getValue();
    }

    public IEntityID getSender() {
        return sender;
    }

    public Optional<IEntityQuery<?>> getForwardingRestrictions() {
        return Optional.ofNullable(forwardingRestrictions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, sender);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof MessageContext))
            return false;
        MessageContext other = (MessageContext) obj;
        return Objects.equals(uuid, other.uuid) && Objects.equals(sender, other.sender);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Message [uuid=").append(uuid).append(", destinationTrace=").append(destinationTrace)
                .append(", sender=").append(sender).append(", forwardingRestrictions=").append(forwardingRestrictions)
                .append("]");
        return builder.toString();
    }

}
