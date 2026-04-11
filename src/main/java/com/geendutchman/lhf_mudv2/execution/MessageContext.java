package com.geendutchman.lhf_mudv2.execution;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidFactory;
import com.google.auto.value.AutoBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public final class MessageContext implements Serializable {
    private static final TsidFactory idFactory = TsidFactory
            .newInstance1024(Math.abs("messageContext".hashCode() % 1024));
    private final Tsid tsid;
    private final IEntityID sender;
    private final IEntityID destination;
    private final Optional<IEntityID> replyTo;
    private final Optional<Item> item;
    private final Optional<Creature> creature;
    private final Optional<Room> room;
    private final ImmutableBiMap<String, Entity> others;

    MessageContext(IEntityID sender, IEntityID destination, Optional<IEntityID> replyTo, Optional<Item> item,
            Optional<Creature> creature, Optional<Room> room, Optional<Tsid> tsid, BiMap<String, Entity> others) {
        Preconditions.checkNotNull(sender, "sender must not be null");
        Preconditions.checkNotNull(destination, "destination must not be null");
        Preconditions.checkNotNull(replyTo, "replyTo may be empty but must not be null");
        Preconditions.checkNotNull(item, "item may be empty but must not be null");
        Preconditions.checkNotNull(creature, "creature may be empty but must not be null");
        Preconditions.checkNotNull(room, "room may be empty but must not be null");
        this.sender = sender;
        this.destination = destination;
        this.replyTo = replyTo;
        this.item = item;
        this.creature = creature;
        this.room = room;
        this.tsid = tsid == null ? MessageContext.idFactory.create() : tsid.orElse(MessageContext.idFactory.create());
        this.others = others != null ? ImmutableBiMap.copyOf(others) : ImmutableBiMap.of();
    }

    @AutoBuilder
    public static abstract class MessageContextBuilder {
        public abstract MessageContextBuilder setSender(IEntityID sender);

        public abstract MessageContextBuilder setDestination(IEntityID destination);

        public abstract MessageContextBuilder setReplyTo(IEntityID replyto);

        public abstract MessageContextBuilder setItem(Item item);

        public abstract MessageContextBuilder setItem(Optional<Item> item);

        public abstract Optional<Item> item();

        public abstract MessageContextBuilder setCreature(Creature creature);

        public abstract MessageContextBuilder setCreature(Optional<Creature> creature);

        public abstract Optional<Creature> creature();

        public abstract MessageContextBuilder setRoom(Room room);

        public abstract MessageContextBuilder setRoom(Optional<Room> room);

        public abstract Optional<Room> room();

        protected abstract MessageContextBuilder setTsid(Optional<Tsid> tsid);

        protected abstract Optional<Tsid> tsid();

        public MessageContextBuilder setMainEntities(Item item, Creature creature, Room room) {
            return this.setItem(Optional.ofNullable(item)).setCreature(Optional.ofNullable(creature))
                    .setRoom(Optional.ofNullable(room));
        }

        public abstract ImmutableBiMap.Builder<String, Entity> othersBuilder();

        public MessageContextBuilder addOther(String key, Entity entity) {
            final ImmutableBiMap.Builder<String, Entity> subBuilder = this.othersBuilder();
            subBuilder.put(key, entity);
            return this;
        }

        public abstract MessageContextBuilder setOthers(BiMap<String, Entity> others);

        public abstract MessageContext build();
    }

    public static MessageContextBuilder builder() {
        return new AutoBuilder_MessageContext_MessageContextBuilder();
    }

    public MessageContextBuilder toBuilder() {
        return new AutoBuilder_MessageContext_MessageContextBuilder(this).setTsid(Optional.<Tsid>empty());
    }

    public MessageContextBuilder toBuilder(boolean withTime) {
        final MessageContextBuilder builder = new AutoBuilder_MessageContext_MessageContextBuilder(this);
        if (withTime) {
            builder.setTsid(this.tsid());
        } else {
            builder.setTsid(Optional.empty());
        }
        return builder;
    }

    public static MessageContext inflate(final MessageContext fromContext,
            final Consumer<MessageContextBuilder> inflator) {
        MessageContextBuilder builder = MessageContext.builder();
        if (fromContext != null) {
            builder.setTsid(fromContext.tsid());
        }
        if (inflator != null) {
            inflator.accept(builder);
        }
        return builder.build();
    }

    protected Optional<Tsid> tsid() {
        return Optional.ofNullable(tsid);
    }

    public Tsid getTsid() {
        return tsid;
    }

    public IEntityID sender() {
        return sender;
    }

    public IEntityID getSender() {
        return sender;
    }

    public Optional<IEntityID> replyTo() {
        return this.replyTo;
    }

    public static TsidFactory getIdfactory() {
        return idFactory;
    }

    public IEntityID destination() {
        return destination;
    }

    public IEntityID getDestination() {
        return destination;
    }

    public Optional<IEntityID> getReplyTo() {
        return replyTo;
    }

    public Optional<Item> getItem() {
        return item;
    }

    public Optional<Item> item() {
        return this.item;
    }

    public Optional<Creature> getCreature() {
        return creature;
    }

    public Optional<Creature> creature() {
        return creature;
    }

    public Optional<Room> getRoom() {
        return room;
    }

    public Optional<Room> room() {
        return room;
    }

    public ImmutableBiMap<String, Entity> getOthers() {
        return others;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tsid, sender, destination, replyTo, item, creature, room, others);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof MessageContext))
            return false;
        MessageContext other = (MessageContext) obj;
        return Objects.equals(tsid, other.tsid) && Objects.equals(sender, other.sender)
                && Objects.equals(destination, other.destination) && Objects.equals(replyTo, other.replyTo)
                && Objects.equals(item, other.item) && Objects.equals(creature, other.creature)
                && Objects.equals(room, other.room) && Objects.equals(others, other.others);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MessageContext [tsid=").append(tsid).append(", sender=").append(sender).append(", destination=")
                .append(destination).append(", replyTo=").append(replyTo).append(", item=")
                .append(item.map(i -> i.identifier())).append(", creature=").append(creature.map(c -> c.identifier()))
                .append(", room=").append(room.map(r -> r.identifier())).append(", others=").append(others).append("]");
        return builder.toString();
    }

}
