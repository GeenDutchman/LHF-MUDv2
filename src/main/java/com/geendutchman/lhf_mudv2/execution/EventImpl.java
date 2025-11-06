package com.geendutchman.lhf_mudv2.execution;

import java.util.Optional;

import com.geendutchman.lhf_mudv2.display.Examinable.BasicExaminable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable.BasicTaggable;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.creatures.Faction;
import com.geendutchman.lhf_mudv2.entities.creatures.ResourcePoolSize;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

final class EventImpl {
    private EventImpl() {
    }

    protected static record ItemChangedEventImpl(PlainEvent plain, ItemID itemID) implements Event.ItemChangedEvent {
        protected ItemChangedEventImpl {
            Preconditions.checkNotNull(plain, "base event must not be null");
            Preconditions.checkNotNull(itemID, "item id must not be null");
        }
    }

    protected static record CreatureChangedEventImpl(PlainEvent plain, CreatureID creatureID)
            implements Event.CreatureChangedEvent {
        protected CreatureChangedEventImpl {
            Preconditions.checkNotNull(plain, "base event must not be null");
            Preconditions.checkNotNull(creatureID, "creature ID must not be null");
        }
    }

    protected static record RoomChangedEventImpl(PlainEvent plain, RoomID roomID) implements Event.RoomChangedEvent {
        protected RoomChangedEventImpl {
            Preconditions.checkNotNull(plain, "base event must not be null");
            Preconditions.checkNotNull(roomID, "room ID must not be null");
        }
    }

    protected static record RoomSeenEventImpl(PlainEvent plain, BasicExaminable room,
            ImmutableList<BasicTaggable> items, ImmutableList<BasicTaggable> creatures) implements Event.RoomSeenEvent {
        public RoomSeenEventImpl {
            Preconditions.checkNotNull(plain, "base event must not be null");

            Preconditions.checkArgument(room != null, "room as examinable must not be null");
            Preconditions.checkArgument(items != null, "Items may be empty but must not be null");
            Preconditions.checkArgument(creatures != null, "Creatures may be empty but must not be null");
        }
    }

    protected static record CreatureSeenEventImpl(PlainEvent plain, BasicExaminable creature, Faction faction,
            ResourcePoolSize healthBucket) implements Event.CreatureSeenEvent {
        public CreatureSeenEventImpl {
            Preconditions.checkNotNull(plain, "base event must not be null");
            Preconditions.checkNotNull(creature, "creature examinable must not be null");
            Preconditions.checkNotNull(faction, "faction must not be null");
            Preconditions.checkNotNull(healthBucket, "health bucket must not be null");
        }
    }

    protected static record ItemSeenEventImpl(PlainEvent plain, BasicExaminable item) implements Event.ItemSeenEvent {
        public ItemSeenEventImpl {
            Preconditions.checkNotNull(plain, "base event must not be null");
            Preconditions.checkNotNull(item, "item examinable must not be null");
        }
    }

    protected static record SpokenEventImpl(PlainEvent plain, BasicTaggable speaker, Optional<BasicTaggable> listener,
            RichOutput message) implements Event.SpokenEvent {
        protected SpokenEventImpl {
            Preconditions.checkNotNull(plain, "base event must not be null");
            Preconditions.checkNotNull(speaker, "speaker should not be null");
            Preconditions.checkNotNull(listener, "listener may be empty but must not be null");
            Preconditions.checkNotNull(message, "message must not be null");
        }
    }

}
