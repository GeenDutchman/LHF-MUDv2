package com.geendutchman.lhf_mudv2.events;

import java.net.URI;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import com.geendutchman.lhf_mudv2.display.Examinable.BasicExaminable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.display.Taggable.BasicTaggable;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.creatures.Faction;
import com.geendutchman.lhf_mudv2.entities.creatures.ResourcePoolSize;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.google.auto.value.AutoBuilder;
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

        public PlainEventBuilder toBuilder() {
            return new PlainEventBuilder().setRouting(this.routing).resetDescription(b -> this.description.toBuilder());
        }

        public static PlainEventBuilder builder() {
            return new PlainEventBuilder();
        }

    }

    /**
     * Build the immutable Event
     */
    public sealed interface BuildStep permits PlainEventBuilder, AbstractEventBuilder {
        /**
         * The only thing left to do is build it
         * 
         * @return
         */
        public Event build();
    }

    /**
     * Used to add a destination to the routing that an Event needs
     */
    public sealed interface RoutingDestinationStep permits PlainEventBuilder, AbstractEventBuilder {
        /**
         * Set the destination (must not be null).
         * 
         * All that is left is to build the Event.
         * 
         * @param dest
         * @return
         */
        public BuildStep setDestination(URI dest);
    }

    /**
     * Set the Routing for the Event.
     * 
     * Usually, what is left is to build the Event.
     * 
     * @See EventRouting
     */
    public sealed interface RoutingStep permits PlainEventBuilder, AbstractEventBuilder {
        /**
         * Adjust the routing, using considering what may already be present. Note that
         * retrieving the components of the builder may trigger IllegalStateException
         * 
         * @param adjuster
         * @return
         */
        public BuildStep adjustRouting(Consumer<EventRouting.EventRoutingBuilder> adjuster);

        /**
         * Straight up set the event routing.
         * 
         * @param routing must not be null
         * @return
         */
        public BuildStep setRouting(EventRouting routing);

        /**
         * Set the routing piece-wise, describing the sender first.
         * 
         * Next describe the destination.
         * 
         * @param sender
         * @return
         */
        public RoutingDestinationStep setSender(URI sender);

    }

    /**
     * Describe the event.
     * 
     * Usually the next step is to describe the Event Routing.
     */
    public sealed interface DescriptionStep permits PlainEventBuilder, AbstractEventBuilder {
        /**
         * Adjust the event, seeing what has already been done and optionally adding to
         * it
         * 
         * @param adjuster
         * @return
         */
        public RoutingStep adjustDescription(Consumer<RichOutput.Builder> adjuster);

        /**
         * Straight up set the description
         * 
         * @param richOutput must not be null
         * @return
         */
        public RoutingStep setDescription(RichOutput richOutput);

        /**
         * Straight up set the description with a simple string
         * 
         * @param desc
         * @return
         */
        public RoutingStep setShortDescription(String desc);
    }

    public static DescriptionStep builder() {
        return new PlainEventBuilder();
    }

    public final class PlainEventBuilder implements BuildStep, DescriptionStep, RoutingStep, RoutingDestinationStep {
        private EventRouting.EventRoutingBuilder routingBuilder;

        private RichOutput.Builder descriptionBuilder;

        public PlainEventBuilder() {
        }

        protected PlainEventBuilder(Event other) {
            if (other != null) {
                this.routingBuilder = other.routing().toBuilder();
                this.descriptionBuilder = other.description().toBuilder();
            }
        }

        public static PlainEventBuilder fromEvent(Event other) {
            return new PlainEventBuilder(other);
        }

        @Override
        public PlainEventBuilder setRouting(EventRouting routing) {
            if (routing == null) {
                throw new NullPointerException("routing must not be null");
            }
            this.routingBuilder = routing.toBuilder();
            return this;
        }

        @Override
        public PlainEventBuilder adjustRouting(Consumer<EventRouting.EventRoutingBuilder> adjuster) {
            if (adjuster == null) {
                throw new NullPointerException("need to provide adjuster for the routing");
            }
            if (routingBuilder == null) {
                this.routingBuilder = EventRouting.builder();
            }
            adjuster.accept(routingBuilder);
            return this;
        }

        @Override
        public PlainEventBuilder setDestination(URI dest) {
            if (dest == null) {
                throw new NullPointerException("destination should not be null");
            }
            if (this.routingBuilder == null) {
                this.routingBuilder = EventRouting.builder();
            }
            this.routingBuilder.setDestination(dest);
            return this;
        }

        @Override
        public PlainEventBuilder setSender(URI sender) {
            if (sender == null) {
                throw new NullPointerException("sender should not be null");
            }
            if (this.routingBuilder == null) {
                this.routingBuilder = EventRouting.builder();
            }
            this.routingBuilder.setSender(sender);
            return this;
        }

        @Override
        public PlainEventBuilder adjustDescription(Consumer<RichOutput.Builder> adjuster) {
            if (adjuster == null) {
                throw new NullPointerException("need to provide adjuster for the routing");
            }
            if (descriptionBuilder == null) {
                this.descriptionBuilder = RichOutput.builder();
            }
            adjuster.accept(descriptionBuilder);
            return this;
        }

        public PlainEventBuilder resetDescription(UnaryOperator<RichOutput.Builder> adjuster) {
            if (adjuster == null) {
                throw new NullPointerException("need to provide adjuster for the routing");
            }
            RichOutput.Builder recieved = adjuster
                    .apply(this.descriptionBuilder != null ? this.descriptionBuilder : RichOutput.builder());
            if (recieved == null) {
                throw new NullPointerException("the adjuster must return a non-null RichOutput Builder");
            }
            this.descriptionBuilder = recieved;
            return this;
        }

        @Override
        public PlainEventBuilder setDescription(RichOutput richOutput) {
            if (richOutput == null) {
                throw new NullPointerException("Description builder should not be null");
            }
            this.descriptionBuilder = richOutput.toBuilder();
            return this;
        }

        @Override
        public PlainEventBuilder setShortDescription(String desc) {
            if (desc == null) {
                throw new NullPointerException("Short description must not be null");
            }
            this.descriptionBuilder = RichOutput.builder().addString(desc);
            return this;
        }

        @Override
        public PlainEvent build() {
            if (this.routingBuilder == null) {
                throw new NullPointerException("no routing information provided");
            }
            if (this.descriptionBuilder == null) {
                throw new NullPointerException("no description provided");
            }
            return new PlainEvent(this.routingBuilder.build(), UUID.randomUUID(), this.descriptionBuilder.build());
        }

    }

    public sealed interface ItemChangedEvent extends Event permits EventImpl.ItemChangedEventImpl {

        public abstract ItemID itemID();

        public interface ItemStep {
            public DescriptionStep setItem(Item item);
        }

        public static ItemStep builder() {
            return new AutoBuilder_Event_ItemChangedEvent_ItemChangedEventBuilder();
        }

        @AutoBuilder(ofClass = EventImpl.ItemChangedEventImpl.class)
        public abstract class ItemChangedEventBuilder extends AbstractEventBuilder implements ItemStep {

            public abstract ItemChangedEventBuilder setItemID(ItemID itemid);

            public ItemChangedEventBuilder setItem(Item item) {
                this.setShortDescription(String.format("%s has changed.", item.displayName().toString()));
                return this.setItemID(item.itemID());
            }

            public abstract EventImpl.ItemChangedEventImpl build();
        }

    }

    public sealed interface CreatureChangedEvent extends Event permits EventImpl.CreatureChangedEventImpl {

        public abstract CreatureID creatureID();

        public interface CreatureStep {
            public DescriptionStep setCreature(Creature creature);
        }

        public static CreatureStep builder() {
            return new AutoBuilder_Event_CreatureChangedEvent_CreatureChangedEventBuilder();
        }

        @AutoBuilder(ofClass = EventImpl.CreatureChangedEventImpl.class)
        public static abstract class CreatureChangedEventBuilder extends AbstractEventBuilder implements CreatureStep {

            public abstract CreatureChangedEventBuilder setCreatureID(CreatureID creatureID);

            public CreatureChangedEventBuilder setCreature(Creature creature) {
                this.setShortDescription(String.format("%s has changed.", creature.name().toString()));
                return this.setCreatureID(creature.creatureID());
            }

            public abstract EventImpl.CreatureChangedEventImpl build();
        }
    }

    public sealed interface RoomChangedEvent extends Event permits EventImpl.RoomChangedEventImpl {

        public abstract RoomID roomID();

        public interface RoomStep {
            public DescriptionStep setRoom(Room room);
        }

        public static RoomStep builder() {
            return new AutoBuilder_Event_RoomChangedEvent_RoomChangedEventBuilder();
        }

        @AutoBuilder(ofClass = EventImpl.RoomChangedEventImpl.class)
        public static abstract class RoomChangedEventBuilder extends AbstractEventBuilder implements RoomStep {

            public abstract RoomChangedEventBuilder setRoomID(RoomID roomid);

            public RoomChangedEventBuilder setRoom(Room room) {
                this.setShortDescription(String.format("%s has changed.", room.name().toString()));
                return this.setRoomID(room.roomID());
            }

            public abstract EventImpl.RoomChangedEventImpl build();
        }
    }

    public sealed interface RoomSeenEvent extends Event permits EventImpl.RoomSeenEventImpl {

        public abstract ImmutableList<BasicTaggable> items();

        public abstract ImmutableList<BasicTaggable> creatures();

        public abstract BasicExaminable room();

        public interface RoomSeenStep {
            public RoutingStep autoRoom(Room room, Predicate<Item> filterItems, Predicate<Creature> filterCreatures);
        }

        @AutoBuilder(ofClass = EventImpl.RoomSeenEventImpl.class)
        public static abstract class RoomSeenEventBuilder extends AbstractEventBuilder implements RoomSeenStep {

            abstract RoomSeenEventBuilder setRoom(BasicExaminable room);

            public final RoomSeenEventBuilder autoRoom(Room room, Predicate<Item> filterItems,
                    Predicate<Creature> filterCreatures) {
                this.addItemSeen(room.items(), filterItems);
                this.addCreatureSeen(room.creatures(), filterCreatures);
                return this.setRoom(room.basicExaminable());
            }

            abstract ImmutableList.Builder<BasicTaggable> itemsBuilder();

            public RoomSeenEventBuilder addItemSeen(Predicate<Item> filterItems, Item... items) {
                if (filterItems == null) {
                    throw new NullPointerException("filter for items must not be null");
                }
                ImmutableList.Builder<BasicTaggable> iBuilder = this.itemsBuilder();
                for (Item item : items) {
                    if (item == null) {
                        continue;
                    }
                    if (filterItems.test(item)) {
                        iBuilder.add(item.basicTaggable());
                    }
                }
                return this;
            }

            public RoomSeenEventBuilder addItemSeen(Collection<Item> items, Predicate<Item> filterItems) {
                this.itemsBuilder().addAll(items.stream().filter(i -> i != null).filter(filterItems)
                        .map(i -> i.basicTaggable()).iterator());
                return this;
            }

            abstract ImmutableList.Builder<BasicTaggable> creaturesBuilder();

            public RoomSeenEventBuilder addCreatureSeen(Predicate<Creature> filterCreatures, Creature... creatures) {
                if (filterCreatures == null) {
                    throw new NullPointerException("filter for creatures must not be null");
                }
                ImmutableList.Builder<BasicTaggable> iBuilder = this.creaturesBuilder();
                for (Creature c : creatures) {
                    if (c == null) {
                        continue;
                    }
                    if (filterCreatures.test(c)) {
                        iBuilder.add(c.basicTaggable());
                    }
                }
                return this;
            }

            public RoomSeenEventBuilder addCreatureSeen(Collection<Creature> creatures,
                    Predicate<Creature> filterCreatures) {
                this.creaturesBuilder().addAll(creatures.stream().filter(c -> c != null).filter(filterCreatures)
                        .map(c -> c.basicTaggable()).iterator());
                return this;
            }

            protected abstract BasicExaminable room();

            protected abstract ImmutableList<BasicTaggable> items();

            protected abstract ImmutableList<BasicTaggable> creatures();

            protected abstract EventImpl.RoomSeenEventImpl autoBuild();

            @Override
            public final EventImpl.RoomSeenEventImpl build() {
                final BasicExaminable asRoom = this.room();
                RichOutput.Builder output = RichOutput.builder().setTag(asRoom.tag() + "-description");
                output.addExaminable(asRoom);
                RichOutput.Builder itemsOutput = RichOutput.builder().setOnEmpty(Optional.of("No items are visible"))
                        .setTag(Room.ITEM_CONTAINER_TAG + "-description");
                this.items().forEach(item -> itemsOutput.addTaggable(item));
                output.addOutput(itemsOutput.build());
                ListMultimap<String, BasicTaggable> creatureMapping = this.creatures().stream()
                        .collect(Multimaps.toMultimap(t -> t.attributes().getOrDefault("faction", "UNKNOWN"), t -> t,
                                MultimapBuilder.treeKeys().arrayListValues()::build));
                for (Entry<String, Collection<BasicTaggable>> entry : creatureMapping.asMap().entrySet()) {
                    Collection<BasicTaggable> entities = entry.getValue();
                    if (entities.size() > 0) {
                        RichOutput.Builder entitiyBuilder = RichOutput.builder()
                                .setSequenceName(String.format("%s you can see", entry.getKey().toString()));
                        entities.forEach(ent -> entitiyBuilder.addTaggable(ent));
                        output.addOutput(entitiyBuilder.build());
                    }
                }
                this.setDescription(output.build());
                return this.autoBuild();
            }
        }

    }

    public sealed interface CreatureSeenEvent extends Event permits EventImpl.CreatureSeenEventImpl {

        public abstract BasicExaminable creature();

        public abstract Faction faction();

        public abstract ResourcePoolSize healthBucket();

        public static DescriptionStep builder() {
            return new AutoBuilder_Event_CreatureSeenEvent_CreatureSeenEventBuilder();
        }

        @AutoBuilder(ofClass = EventImpl.CreatureSeenEventImpl.class)
        public static abstract class CreatureSeenEventBuilder extends AbstractEventBuilder {
            protected abstract CreatureSeenEventBuilder setCreature(BasicExaminable id);

            public abstract CreatureSeenEventBuilder setFaction(Faction faction);

            public abstract CreatureSeenEventBuilder setHealthBucket(ResourcePoolSize hb);

            public CreatureSeenEventBuilder setCreature(final Creature creature) {
                final BasicExaminable asExaminable = creature.basicExaminable();
                this.setDescription(asExaminable.description()
                        .orElseGet(() -> RichOutput.builder().addString("A creature named")
                                .addPolymorphic(creature.name().toString()).addString("of the")
                                .addPolymorphic(creature.faction()).addString("faction, whose health is")
                                .addPolymorphic(creature.healthBucket().toString()).addString(".").build()));
                return this.setCreature(asExaminable).setFaction(creature.faction())
                        .setHealthBucket(creature.healthBucket());
            }

            public abstract EventImpl.CreatureSeenEventImpl build();
        }
    }

    public sealed interface ItemSeenEvent extends Event permits EventImpl.ItemSeenEventImpl {

        public abstract BasicExaminable item();

        public interface ItemStep {
            public DescriptionStep setItem(Item item);
        }

        public static ItemStep builder() {
            return new AutoBuilder_Event_ItemSeenEvent_Builder();
        }

        @AutoBuilder(ofClass = EventImpl.ItemSeenEventImpl.class)
        public static abstract class Builder extends AbstractEventBuilder implements ItemStep {

            abstract Builder setItem(BasicExaminable item);

            @Override
            public DescriptionStep setItem(Item item) {
                this.setDescription(item.description().orElse(
                        RichOutput.builder().addString("The item").addString(item.displayName().toString()).build()));
                return this.setItem(new BasicExaminable(item.displayName(), item.description(), item.attributes(),
                        item.content(), item.tag()));
            }

            public abstract EventImpl.ItemSeenEventImpl build();
        }
    }

    public sealed interface SpokenEvent extends Event permits EventImpl.SpokenEventImpl {

        public abstract BasicTaggable speaker();

        public abstract Optional<BasicTaggable> listener();

        public abstract RichOutput message();

        public interface MessageStep {
            public RoutingStep setMessage(RichOutput message);
        }

        public interface ListenerStep {
            public MessageStep whoListens(Taggable listener);

            public MessageStep broadcast();
        }

        public interface SpeakerStep {
            public ListenerStep whoSpeaks(Taggable speaker);
        }

        public static SpeakerStep builder() {
            return new AutoBuilder_Event_SpokenEvent_Builder();
        }

        @AutoBuilder(ofClass = EventImpl.SpokenEventImpl.class)
        public static abstract class Builder extends AbstractEventBuilder
                implements SpeakerStep, ListenerStep, MessageStep {

            abstract Builder setSpeaker(BasicTaggable speaker);

            protected abstract BasicTaggable speaker();

            public Builder whoSpeaks(Taggable speaker) {
                if (speaker == null) {
                    throw new NullPointerException("cannot set from null speaker");
                }
                return this.setSpeaker(speaker.basicTaggable());
            }

            abstract Builder setListener(BasicTaggable listener);

            abstract Builder setListener(Optional<BasicTaggable> listener);

            protected abstract Optional<BasicTaggable> listener();

            public Builder broadcast() {
                return this.setListener(Optional.empty());
            }

            public Builder whoListens(Taggable listener) {
                if (listener == null) {
                    throw new NullPointerException("cannot set from null listener");
                }
                return this.setListener(listener.basicTaggable());
            }

            public abstract Builder setMessage(RichOutput message);

            protected abstract RichOutput message();

            protected abstract EventImpl.SpokenEventImpl autoBuild();

            @Override
            public EventImpl.SpokenEventImpl build() {
                RichOutput.Builder output = RichOutput.builder().addTaggable(this.speaker()).addString("says");
                this.listener().ifPresent(hearer -> output.addString("to").addTaggable(hearer));
                output.addString(":").addOutput(this.message());
                this.setDescription(output.build());
                return this.autoBuild();
            }
        }

    }

}
