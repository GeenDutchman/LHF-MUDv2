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

    public record EntityStack(IEntityID baseId, Optional<Item> item, Optional<Creature> creature, Optional<Room> room) {
        public EntityStack {
            Preconditions.checkNotNull(baseId, "base ID should not be null");
            Preconditions.checkNotNull(item, "item may be empty but must not be null");
            Preconditions.checkNotNull(creature, "creature may be empty but must not be null");
            Preconditions.checkNotNull(room, "room may be empty but must not be null");
        }

        protected static EntityStackBuilder builder() {
            return new AutoBuilder_MessageContext_EntityStack_EntityStackBuilder();
        }

        protected EntityStackBuilder toBuilder() {
            EntityStackBuilder builder = EntityStack.builder().baseId(this.baseId);
            this.item.ifPresent(i -> builder.item(i));
            this.creature.ifPresent(c -> builder.creature(c));
            this.room.ifPresent(r -> builder.room(r));
            return builder;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("EntityStack [").append("baseId=").append(baseId).append(", item=")
                    .append(item.map(i -> i.itemID())).append(", creature=").append(creature.map(c -> c.creatureID()))
                    .append(", room=").append(room.map(r -> r.roomID())).append("]");
            return builder.toString();
        }

        @AutoBuilder
        protected interface EntityStackBuilder {
            EntityStackBuilder baseId(IEntityID base);

            IEntityID baseId();

            EntityStackBuilder item(Item item);

            Optional<Item> item();

            EntityStackBuilder creature(Creature creature);

            Optional<Creature> creature();

            EntityStackBuilder room(Room room);

            Optional<Room> room();

            EntityStack build();
        }
    }

    private static final TsidFactory idFactory = TsidFactory
            .newInstance1024(Math.abs("messageContext".hashCode() % 1024));
    private static final Name contextName = Name.fromCharSequence("MessageContext");
    private static final Tag contextTag = new Tag("MessageContext");
    private final Tsid tsid;
    private final EntityStack sender;
    private final EntityStack destination;
    private final Optional<EntityStack> replyTo;
    private final ImmutableBiMap<String, Entity> others;

    MessageContext(EntityStack sender, EntityStack destination, Optional<EntityStack> replyTo, Optional<Tsid> tsid,
            BiMap<String, Entity> others) {
        Preconditions.checkNotNull(sender, "sender must not be null");
        Preconditions.checkNotNull(destination, "destination must not be null");
        Preconditions.checkNotNull(replyTo, "replyTo may be empty but must not be null");
        this.sender = sender;
        this.destination = destination;
        this.replyTo = replyTo;
        this.tsid = tsid == null ? MessageContext.idFactory.create() : tsid.orElse(MessageContext.idFactory.create());
        this.others = others != null ? ImmutableBiMap.copyOf(others) : ImmutableBiMap.of();
    }

    @AutoBuilder
    public static abstract class MessageContextBuilder {
        public abstract MessageContextBuilder setSender(EntityStack sender);

        public abstract MessageContextBuilder setDestination(EntityStack destination);

        public abstract MessageContextBuilder setReplyTo(EntityStack replyto);

        protected abstract MessageContextBuilder setTsid(Optional<Tsid> tsid);

        protected abstract Optional<Tsid> tsid();

        protected abstract EntityStack.EntityStackBuilder senderBuilder();

        protected abstract EntityStack.EntityStackBuilder destinationBuilder();

        public MessageContextBuilder setSenderDetails(Consumer<EntityStack.EntityStackBuilder> buildit) {
            if (buildit != null) {
                buildit.accept(this.senderBuilder());
            }
            return this;
        }

        public MessageContextBuilder setDestinationDetails(Consumer<EntityStack.EntityStackBuilder> buildit) {
            if (buildit != null) {
                buildit.accept(this.destinationBuilder());
            }
            return this;
        }

        public MessageContextBuilder setSenderId(IEntityID id) {
            return this.setSenderDetails(b -> b.baseId(id));
        }

        public MessageContextBuilder setDestinationId(IEntityID id) {
            return this.setDestinationDetails(b -> b.baseId(id));
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

    public EntityStack sender() {
        return sender;
    }

    public EntityStack getSender() {
        return sender;
    }

    public Optional<EntityStack> replyTo() {
        return this.replyTo;
    }

    public static TsidFactory getIdfactory() {
        return idFactory;
    }

    public EntityStack destination() {
        return destination;
    }

    public EntityStack getDestination() {
        return destination;
    }

    public Optional<EntityStack> getReplyTo() {
        return replyTo;
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
        this.sender.item.ifPresent(i -> items.add(i));
        this.sender.creature.ifPresent(c -> items.addAll(c.items()));
        this.sender.room.ifPresent(r -> items.addAll(r.items()));
        this.destination.item.ifPresent(i -> items.add(i));
        this.destination.creature.ifPresent(c -> items.addAll(c.items()));
        this.destination.room.ifPresent(r -> items.addAll(r.items()));
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
        this.sender.creature.ifPresent(c -> creatures.add(c));
        this.sender.room.ifPresent(r -> creatures.addAll(r.creatures()));
        this.destination.creature.ifPresent(c -> creatures.add(c));
        this.destination.room.ifPresent(r -> creatures.addAll(r.creatures()));
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
        this.sender.room.ifPresent(r -> builder.put(r.roomID(), r));
        this.destination.room.ifPresent(r -> builder.put(r.roomID(), r));
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
        return String.format("%s : %s -> %s", this.tsid, this.sender.baseId, this.destination.baseId);
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
        return Objects.hash(tsid, sender, destination, replyTo, others);
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
                && Objects.equals(others, other.others);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MessageContext [tsid=").append(tsid).append(", sender=").append(sender).append(", destination=")
                .append(destination).append(", replyTo=").append(replyTo).append(", others=").append(others)
                .append("]");
        return builder.toString();
    }

}
