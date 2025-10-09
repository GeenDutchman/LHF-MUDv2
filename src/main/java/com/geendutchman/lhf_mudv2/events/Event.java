package com.geendutchman.lhf_mudv2.events;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;


import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.Examinable.BasicExaminable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.display.Taggable.BasicTaggable;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.creatures.Faction;
import com.geendutchman.lhf_mudv2.entities.creatures.ResourcePoolSize;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;

public sealed interface Event {

    public abstract PlainEvent plain();

    public default UUID uuid() {
        return this.plain().uuid();
    }

    public default RichOutput description() {
        return this.plain().description();
    }

    public static record EventRouting(UUID sender, UUID destination) {
        public EventRouting {
            Preconditions.checkNotNull(sender, "send must not be null");
            Preconditions.checkNotNull(destination, "destination must not be null");
        }
    }

    public default EventRouting routing() {
        return this.plain().routing();
    }

    public record PlainEvent(EventRouting routing, UUID uuid, RichOutput description) implements Event {
        public PlainEvent {
            Preconditions.checkArgument(routing != null, "routing must be set");
            Preconditions.checkArgument(uuid != null, "uuid must not be null");
            Preconditions.checkArgument(description != null, "description must not be null");
        }

        public PlainEvent plain() {
            return this;
        }

    }

    public record ItemChangedEvent(PlainEvent plain, ItemID itemID, Examinable.Name displayName) implements Event {
        public ItemChangedEvent {
            Preconditions.checkNotNull(plain, "base event must not be null");
            Preconditions.checkNotNull(itemID, "item ID must not be null");
            Preconditions.checkNotNull(displayName, "display name should not be null");
        }

    }

    public record CreatureChangedEvent(PlainEvent plain, CreatureID creatureID) implements Event {
        public CreatureChangedEvent {
            Preconditions.checkNotNull(plain, "base event must not be null");
            Preconditions.checkNotNull(creatureID, "creature ID must not be null");
        }
    }

    public record RoomChangedEvent(PlainEvent plain, RoomID roomID) implements Event {
        public RoomChangedEvent {
            Preconditions.checkNotNull(plain, "base event must not be null");
            Preconditions.checkNotNull(roomID, "room ID must not be null");
        }
    }

    public record RoomSeenEvent(EventRouting routing, UUID uuid, BasicExaminable room,
            ImmutableList<BasicTaggable> items, ImmutableList<BasicTaggable> creatures) implements Event {
        public RoomSeenEvent {
            Preconditions.checkArgument(routing != null, "routing must be set");
            Preconditions.checkArgument(uuid != null, "uuid must not be null");
            Preconditions.checkArgument(room != null, "room as examinable must not be null");
            Preconditions.checkArgument(items != null, "Items may be empty but must not be null");
            Preconditions.checkArgument(creatures != null, "Creatures may be empty but must not be null");
        }

        @Override
        public RichOutput description() {
            RichOutput.Builder builder = RichOutput.builder()
                    .setTag(Optional.ofNullable(this.room.tag() + "-description"));
            this.room.description().ifPresent(desc -> builder.addOutput(desc));
            if (this.items != null && this.items.size() > 0) {
                RichOutput.Builder itemsBuilder = RichOutput.builder().setSequenceName("Items")
                        .setTag(Optional.ofNullable(this.room.tag() + "-items"));
                this.items.forEach(item -> itemsBuilder.addTaggable(item));
                builder.addOutput(itemsBuilder.build());
            }
            if (this.creatures != null && this.creatures.size() > 0) {
                RichOutput.Builder creaturesBuilder = RichOutput.builder().setSequenceName("Creatures")
                        .setTag(Optional.ofNullable(this.room.tag() + "-creatures"));
                ListMultimap<Taggable.Tag, BasicTaggable> creatureMapping = this.creatures.stream().collect(Multimaps
                        .toMultimap(c -> c.tag(), c -> c, MultimapBuilder.treeKeys().arrayListValues()::build));
                for (Entry<Taggable.Tag, Collection<BasicTaggable>> entry : creatureMapping.asMap().entrySet()) {
                    Collection<BasicTaggable> entities = entry.getValue();
                    if (entities.size() > 0) {
                        RichOutput.Builder entityBuilder = RichOutput.builder()
                                .setSequenceName(String.format("%s you can see", entry.getKey()));
                        entities.forEach(ent -> entityBuilder.addTaggable(ent));
                        creaturesBuilder.addOutput(entityBuilder.build());
                    }
                }
                builder.addOutput(creaturesBuilder.build());
            }
            return builder.build();
        }

        @Override
        public PlainEvent plain() {
            return new PlainEvent(this.routing, this.uuid, this.description());
        }
    }

    public record CreatureSeenEvent(PlainEvent plain, CreatureID creatureID, Faction faction,
            ResourcePoolSize healthBucket) implements Event {
        public CreatureSeenEvent {
            Preconditions.checkNotNull(plain, "base event must not be null");
            Preconditions.checkNotNull(creatureID, "creature ID must not be null");
            Preconditions.checkNotNull(faction, "faction must not be null");
            Preconditions.checkNotNull(healthBucket, "health bucket must not be null");
        }
    }

    public record ItemSeenEvent(PlainEvent plain, ItemID itemID, Examinable.Name displayName) {
        public ItemSeenEvent {
            Preconditions.checkNotNull(plain, "base event must not be null");
            Preconditions.checkNotNull(itemID, "item ID must not be null");
            Preconditions.checkNotNull(displayName, "display name should not be null");
        }
    }

    public record SpokenEvent(EventRouting routing, UUID uuid, BasicExaminable speaker,
            Optional<BasicExaminable> listener, RichOutput message) implements Event {
        public SpokenEvent {
            Preconditions.checkArgument(routing != null, "routing must be set");
            Preconditions.checkArgument(uuid != null, "uuid must not be null");
            Preconditions.checkNotNull(speaker, "speaker should not be null");
            Preconditions.checkNotNull(listener, "listener may be empty but must not be null");
            Preconditions.checkNotNull(message, "message must not be null");
        }

        @Override
        public RichOutput description() {
            RichOutput.Builder output = RichOutput.builder().setSequenceName(this.speaker().name().toString())
                    .addTaggable(this.speaker()).addString("says");
            if (this.listener().isPresent()) {
                output.addString("to").addTaggable(this.listener().get());
            }
            output.addString(":\n")
                    .addOutput(RichOutput.builder().setSequenceName("message").addOutput(this.message()).build());
            return output.build();
        }

        @Override
        public PlainEvent plain() {
            return new PlainEvent(this.routing, this.uuid, this.description());
        }
    }

}
