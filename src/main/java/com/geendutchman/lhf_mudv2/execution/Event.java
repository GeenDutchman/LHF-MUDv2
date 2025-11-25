package com.geendutchman.lhf_mudv2.execution;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.geendutchman.lhf_mudv2.display.Examinable.BasicExaminable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.RichOutputElement;
import com.geendutchman.lhf_mudv2.display.Taggable.BasicTaggable;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.creatures.Faction;
import com.geendutchman.lhf_mudv2.entities.creatures.ResourcePoolSize;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;

public sealed interface Event extends Message {

    public default PlainEvent plain() {
        return new PlainEvent(this.uuid(), this.description());
    }

    public RichOutput description();

    public record PlainEvent(UUID uuid, RichOutput description) implements Event {
        public PlainEvent {
            Preconditions.checkNotNull(uuid, "uuid must not be null");
            Preconditions.checkArgument(description != null, "description must not be null");
        }

        public PlainEvent plain() {
            return this;
        }

        public static PlainEvent asDescribed(RichOutput description) {
            return new PlainEvent(UUID.randomUUID(), description);
        }

    }

    public record ItemChangedEvent(UUID uuid, ItemID itemID, RichOutput description) implements Event {
        public ItemChangedEvent {
            Preconditions.checkNotNull(uuid, "uuid must not be null");
            Preconditions.checkNotNull(itemID, "item id must not be null");
            if (description == null) {
                description = RichOutput.builder().addString(itemID.name().toString()).addString("has changed.")
                        .build();
            }
        }

        public ItemChangedEvent(Item item) {
            this(UUID.randomUUID(), item.itemID(),
                    RichOutput.builder().addTaggable(item).addString("has changed.").build());
        }

        public static ItemChangedEvent ofItem(Item item) {
            Preconditions.checkNotNull(item, "item should not be null");
            return new ItemChangedEvent(UUID.randomUUID(), item.itemID(),
                    RichOutput.builder().addTaggable(item).addString("has changed.").build());
        }
    }

    public record CreatureChangedEvent(UUID uuid, CreatureID creatureID, RichOutput description) implements Event {
        public CreatureChangedEvent {
            Preconditions.checkNotNull(uuid, "uuid must not be null");

            Preconditions.checkNotNull(creatureID, "creature id must not be null");
            if (description == null) {
                description = RichOutput.builder().addString(creatureID.name().toString()).addString("has changed.")
                        .build();
            }
        }

        public static CreatureChangedEvent ofCreature(Creature creature) {
            Preconditions.checkNotNull(creature, "creature should not be null");
            return new CreatureChangedEvent(UUID.randomUUID(), creature.creatureID(),
                    RichOutput.builder().addTaggable(creature).addString("has changed.").build());
        }

        public static CreatureChangedEvent ofCreatureWithChangeDescription(Creature creature, RichOutput description) {
            Preconditions.checkNotNull(creature, "creature should not be null");
            return new CreatureChangedEvent(UUID.randomUUID(), creature.creatureID(), description);
        }
    }

    public record RoomChangedEvent(UUID uuid, RoomID roomID, RichOutput description) implements Event {
        public RoomChangedEvent {
            Preconditions.checkNotNull(uuid, "uuid must not be null");

            Preconditions.checkNotNull(roomID, "room id must not be null");
            if (description == null) {
                description = RichOutput.builder().addString(roomID.name().toString()).addString("has changed.")
                        .build();
            }
        }

        public static RoomChangedEvent ofRoom(Room room) {
            Preconditions.checkNotNull(room, "room should not be null");
            return new RoomChangedEvent(UUID.randomUUID(), room.roomID(),
                    RichOutput.builder().addTaggable(room).addString("has changed.").build());
        }

        public static RoomChangedEvent ofRoomWithChangeDescription(Room room, RichOutput description) {
            Preconditions.checkNotNull(room, "room should not be null");
            return new RoomChangedEvent(UUID.randomUUID(), room.roomID(), description);
        }
    }

    public final class RoomSeenEvent implements Event {
        private final UUID uuid = UUID.randomUUID();
        private final BasicExaminable room;
        private final RoomID roomID;
        private final ImmutableSet<BasicTaggable> items;
        private final ImmutableSet<BasicTaggable> creatures;

        public RoomSeenEvent(Room room, IEntityQuery<Item> filterItems, IEntityQuery<Creature> filterCreatures) {
            Preconditions.checkNotNull(room, "room should not be null");
            this.roomID = room.roomID();
            this.room = room.basicExaminable();
            this.items = room.items().stream().filter(filterItems != null ? filterItems : i -> i != null)
                    .map(i -> i.basicTaggable()).collect(ImmutableSet.toImmutableSet());
            this.creatures = room.creatures().stream()
                    .filter(filterCreatures != null ? filterCreatures : c -> c != null).map(c -> c.basicTaggable())
                    .collect(ImmutableSet.toImmutableSet());
        }

        @Override
        public RichOutput description() {
            RichOutput.Builder builder = RichOutput.builder();
            this.room.description().ifPresent(rd -> builder.addOutput(rd));
            if (this.items != null && this.items.size() > 0) {
                RichOutput.Builder itemsBuilder = RichOutput.builder().setSequenceName("Items").setTag("Items-list")
                        .setElementSeparator(Optional.of(RichOutputElement.ofString(", ")));
                this.items.forEach(i -> itemsBuilder.addTaggable(i));
                builder.addOutput(itemsBuilder.build());
            }
            if (this.creatures != null && this.creatures.size() > 0) {
                RichOutput.Builder creaturesBuilder = RichOutput.builder().setSequenceName("Creatures")
                        .setTag("Creatures-list");
                ListMultimap<String, BasicTaggable> creatureMapping = this.creatures.stream()
                        .collect(Multimaps.toMultimap(t -> t.attributes().getOrDefault("faction", "UNKNOWN"), t -> t,
                                MultimapBuilder.treeKeys().arrayListValues()::build));
                for (Entry<String, Collection<BasicTaggable>> entry : creatureMapping.asMap().entrySet()) {
                    Collection<BasicTaggable> entities = entry.getValue();
                    if (entities.size() > 0) {
                        RichOutput.Builder entitiyBuilder = RichOutput.builder()
                                .setElementSeparator(Optional.of(RichOutputElement.ofString(", ")))
                                .setSequenceName(String.format("%s you can see", entry.getKey().toString()));
                        entities.forEach(ent -> entitiyBuilder.addTaggable(ent));
                        creaturesBuilder.addOutput(entitiyBuilder.build());
                    }
                }
                builder.addOutput(creaturesBuilder.build());
            }
            return builder.build();
        }

        @Override
        public UUID uuid() {
            return this.uuid;
        }

        public BasicExaminable getRoom() {
            return room;
        }

        public RoomID getRoomID() {
            return roomID;
        }

        public ImmutableSet<BasicTaggable> getItems() {
            return items;
        }

        public ImmutableSet<BasicTaggable> getCreatures() {
            return creatures;
        }

        @Override
        public int hashCode() {
            return Objects.hash(room, items, creatures);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof RoomSeenEvent))
                return false;
            RoomSeenEvent other = (RoomSeenEvent) obj;
            return Objects.equals(room, other.room) && Objects.equals(items, other.items)
                    && Objects.equals(creatures, other.creatures);
        }

    }

    public final class CreatureSeenEvent implements Event {
        private final UUID uuid = UUID.randomUUID();
        private final BasicExaminable creature;
        private final CreatureID creatureID;
        private final Faction faction;
        private final ResourcePoolSize healthBucket;

        public CreatureSeenEvent(Creature creature) {
            Preconditions.checkNotNull(creature, "creature must not be null");
            this.creature = creature.basicExaminable();
            this.creatureID = creature.creatureID();
            this.faction = creature.faction();
            this.healthBucket = creature.healthBucket();
        }

        @Override
        public RichOutput description() {
            return this.creature.description()
                    .orElseGet(() -> RichOutput.builder().addString("A creature named").addTaggable(creature)
                            .addString("of the").addPolymorphic(faction).addString("faction, whose health is")
                            .addPolymorphic(healthBucket.toString()).addString(".").build());
        }

        public UUID uuid() {
            return this.uuid;
        }

        public BasicExaminable getCreature() {
            return creature;
        }

        public CreatureID getCreatureID() {
            return creatureID;
        }

        public Faction getFaction() {
            return faction;
        }

        public ResourcePoolSize getHealthBucket() {
            return healthBucket;
        }

        @Override
        public int hashCode() {
            return Objects.hash(creature, creatureID, faction, healthBucket);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof CreatureSeenEvent))
                return false;
            CreatureSeenEvent other = (CreatureSeenEvent) obj;
            return Objects.equals(creature, other.creature) && Objects.equals(creatureID, other.creatureID)
                    && faction == other.faction && healthBucket == other.healthBucket;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("CreatureSeenEvent [creature=").append(creature).append(", faction=").append(faction)
                    .append(", healthBucket=").append(healthBucket).append("]");
            return builder.toString();
        }

    }

    public final class ItemSeenEvent implements Event {
        private final UUID uuid = UUID.randomUUID();
        private final BasicExaminable item;
        private final ItemID itemID;

        public ItemSeenEvent(Item item) {
            Preconditions.checkNotNull(item, "item must not be null");
            this.item = item.basicExaminable();
            this.itemID = item.itemID();
        }

        @Override
        public RichOutput description() {
            return this.item.description().orElse(RichOutput.builder().addString("The item").addTaggable(item).build());
        }

        public UUID uuid() {
            return this.uuid;
        }

        public BasicExaminable getItem() {
            return item;
        }

        public ItemID getItemID() {
            return itemID;
        }

        @Override
        public int hashCode() {
            return Objects.hash(item, itemID);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof ItemSeenEvent))
                return false;
            ItemSeenEvent other = (ItemSeenEvent) obj;
            return Objects.equals(item, other.item) && Objects.equals(itemID, other.itemID);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ItemSeenEvent [item=").append(item).append("]");
            return builder.toString();
        }

    }

    public record SpokenEvent(UUID uuid, RichOutput message, BasicTaggable speaker, Optional<BasicTaggable> listener)
            implements Event {
        public SpokenEvent {
            Preconditions.checkNotNull(uuid, "uuid must not be null");
            Preconditions.checkNotNull(message, "message should not be null");
            Preconditions.checkNotNull(speaker, "someone should be speaking the message");
            Preconditions.checkNotNull(listener, "listener could be empty but must not be null");
        }

        public static SpokenEvent speaking(BasicTaggable speaker, RichOutput message) {
            return new SpokenEvent(UUID.randomUUID(), message, speaker, Optional.empty());
        }

        public static SpokenEvent speakingTo(BasicTaggable speaker, RichOutput message, BasicTaggable hearer) {
            return new SpokenEvent(UUID.randomUUID(), message, speaker, Optional.ofNullable(hearer));
        }

        @Override
        public RichOutput description() {
            RichOutput.Builder builder = RichOutput.builder().addTaggable(speaker).addString("says");
            this.listener.ifPresent(hearer -> builder.addString("to").addTaggable(hearer));
            builder.addString(":").addOutput(this.message);
            return builder.build();
        }
    }

}
