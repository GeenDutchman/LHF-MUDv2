package com.geendutchman.lhf_mudv2.execution;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureContainer;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.EntityContainer;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.geendutchman.lhf_mudv2.entities.item.ItemContainer;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.geendutchman.lhf_mudv2.entities.room.RoomContainer;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidFactory;
import com.google.auto.value.AutoBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

public final class MessageContext
        implements Serializable, ItemContainer, CreatureContainer, RoomContainer, EntityContainer {
    private static final TsidFactory idFactory = TsidFactory
            .newInstance1024(Math.abs("messageContext".hashCode() % 1024));
    private static final Name contextName = Name.fromCharSequence("MessageContext");
    private static final Tag contextTag = new Tag("MessageContext");
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
    public ImmutableSortedMap<String, String> attributes() {
        return Examinable.BASIC_TAGGABLE_ATTRIBUTES;
    }

    @Override
    public Optional<Item> byItemID(ItemID id) {
        return this.items().stream().filter(i -> i != null && id != null && id.compareTo(i.identifier()) == 0)
                .findFirst();
    }

    @Override
    public boolean hasItem(Item item) {
        return this.items().contains(item);
    }

    @Override
    public ImmutableSet<Item> items() {
        ImmutableSet.Builder<Item> items = ImmutableSet.builder();
        this.item.ifPresent(i -> items.add(i));
        this.creature.ifPresent(c -> items.addAll(c.items()));
        this.room.ifPresent(r -> items.addAll(r.items()));
        this.others.values().forEach(e -> {
            if (e instanceof Item i) {
                items.add(i);
            }
        });
        return items.build();
    }

    @Override
    public Name name() {
        return MessageContext.contextName;
    }

    @Override
    public Optional<Creature> byCreatureID(CreatureID id) {
        return this.creatures().stream().filter(c -> c != null && id != null && c.identifier().compareTo(id) == 0)
                .findFirst();
    }

    @Override
    public ImmutableSet<Creature> creatures() {
        ImmutableSet.Builder<Creature> creatures = ImmutableSet.builder();
        this.creature.ifPresent(c -> creatures.add(c));
        this.room.ifPresent(r -> creatures.addAll(r.creatures()));
        this.others.values().forEach(e -> {
            if (e instanceof Creature c) {
                creatures.add(c);
            }
        });
        return creatures.build();
    }

    @Override
    public boolean hasCreature(Creature creature) {
        return this.creatures().contains(creature);
    }

    @Override
    public Optional<Room> byRoomID(RoomID id) {
        return Optional.ofNullable(this.roomMap().getOrDefault(id, null));
    }

    @Override
    public boolean hasRoom(Room room) {
        return this.roomMap().containsValue(room);
    }

    @Override
    public ImmutableBiMap<RoomID, Room> roomMap() {
        ImmutableBiMap.Builder<RoomID, Room> builder = ImmutableBiMap.builder();
        this.room.ifPresent(r -> builder.put(r.roomID(), r));
        this.others.values().forEach(e -> {
            if (e instanceof Room r) {
                builder.put(r.roomID(), r);
            }
        });
        return builder.build();
    }

    @Override
    public ImmutableSet<Room> rooms() {
        return this.roomMap().values();
    }

    @Override
    public ConcurrentNavigableMap<IEntityID, Entity> entities() {
        ConcurrentSkipListMap<IEntityID, Entity> mapping = new ConcurrentSkipListMap<>();
        this.items().forEach(i -> mapping.put(i.identifier(), i));
        this.creatures().forEach(c -> mapping.put(c.identifier(), c));
        mapping.putAll(this.roomMap());
        this.others.values().forEach(o -> mapping.put(o.identifier(), o));
        return mapping;
    }

    @Override
    public String content() {
        return String.format("%s : %s -> %s", this.tsid, this.sender, this.destination);
    }

    @Override
    public Optional<RichOutput> description() {
        return Optional.empty();
    }

    @Override
    public Tag tag() {
        return MessageContext.contextTag;
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
