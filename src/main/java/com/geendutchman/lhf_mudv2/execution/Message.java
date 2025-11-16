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

public final class Message implements Serializable {
    private final UUID uuid = UUID.randomUUID();
    private final LinkedHashMap<Taggable.Tag, IEntityID> destinationTrace = new LinkedHashMap<>();
    private final IEntityID sender;
    private final IEntityQuery<?> forwardingRestrictions;
    private final Event event;
    private final Command command;

    private Message(IEntityID sender, IEntityID destination, IEntityQuery<?> forwarding, Event event, Command command) {
        Preconditions.checkNotNull(sender, "sender must not be null");
        Preconditions.checkNotNull(destination, "destination must not be null");
        Preconditions.checkArgument((event != null && command == null) || (event == null && command != null),
                "either event must be populated xor command, but not both or none");
        this.sender = sender;
        this.forwardingRestrictions = forwarding;
        this.event = event;
        this.command = command;
        this.destinationTrace.put(destination.entityClass(), destination);
    }

    public static Message eventMessage(IEntityID sender, IEntityID destination, Event event) {
        Preconditions.checkNotNull(sender, "sender must not be null");
        Preconditions.checkNotNull(destination, "destination must not be null");
        Preconditions.checkNotNull(event, "event must not be null");
        final Message created = new Message(sender, destination, null, event, null);
        return created;
    }

    public static Message eventMessageWithForwardingRestrictions(IEntityID sender, IEntityID destination, Event event,
            IEntityQuery<?> restrictions) {
        Preconditions.checkNotNull(sender, "sender must not be null");
        Preconditions.checkNotNull(destination, "destination must not be null");
        Preconditions.checkNotNull(event, "event must not be null");
        final Message message = new Message(sender, destination, restrictions, event, null);
        return message;
    }

    public static Message commandMessage(IEntityID sender, IEntityID destination, Command command) {
        Preconditions.checkNotNull(sender, "sender must not be null");
        Preconditions.checkNotNull(destination, "destination must not be null");
        Preconditions.checkNotNull(command, "command must not be null");
        final Message created = new Message(sender, destination, null, null, command);
        return created;
    }

    public synchronized Message forward(IEntityID destination) {
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

    public Optional<Event> getEvent() {
        return Optional.ofNullable(event);
    }

    public Optional<Command> getCommand() {
        return Optional.ofNullable(command);
    }

    public boolean isEventNotCommand() {
        Preconditions.checkState((event != null && command == null) || (event == null && command != null),
                "either event must be populated xor command, but not both or none");
        return event != null ? true : false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, sender, event, command);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Message))
            return false;
        Message other = (Message) obj;
        return Objects.equals(uuid, other.uuid) && Objects.equals(sender, other.sender)
                && Objects.equals(event, other.event) && Objects.equals(command, other.command);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Message [uuid=").append(uuid).append(", destinationTrace=").append(destinationTrace)
                .append(", sender=").append(sender).append(", forwardingRestrictions=").append(forwardingRestrictions)
                .append(", event=").append(event).append(", command=").append(command).append("]");
        return builder.toString();
    }

}
