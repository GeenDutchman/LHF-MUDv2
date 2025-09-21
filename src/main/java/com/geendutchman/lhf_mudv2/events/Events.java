package com.geendutchman.lhf_mudv2.events;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.Examinable.BasicExaminable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.RichOutput.Builder;
import com.geendutchman.lhf_mudv2.display.RichOutputElement;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureEffect;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemEffect;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.geendutchman.lhf_mudv2.entities.room.RoomEffect;
import com.geendutchman.lhf_mudv2.events.Event.EventRouting;
import com.geendutchman.lhf_mudv2.events.Event.EventRouting.EventRoutingBuilder;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

public final class Events {

    @AutoValue
    public static sealed abstract class PlainEvent extends Event permits AutoValue_Events_PlainEvent {

        static PlainEventBuilder builder() {
            return new AutoValue_Events_PlainEvent.Builder();
        }

        @AutoValue.Builder
        public interface PlainEventBuilder {
            public abstract PlainEventBuilder setDescription(Optional<RichOutput> description);

            public abstract PlainEventBuilder setDescription(RichOutput description);

            abstract EventRoutingBuilder routingBuilder();

            public default PlainEventBuilder setRouting(Consumer<EventRoutingBuilder> setter) {
                if (setter != null) {
                    setter.accept(this.routingBuilder());
                }
                return this;
            }

            public abstract PlainEvent build();
        }
    }

    public static PlainEvent.PlainEventBuilder plainEvent() {
        return PlainEvent.builder();
    }

    @AutoValue
    public static sealed abstract class CreateItemsForCreatureEvent extends Event
            permits AutoValue_Events_CreateItemsForCreatureEvent {
        public abstract CreatureID forCreature();

        public abstract ImmutableList<ItemBuilderFactory.LockedItemBuilder> itemBuilders();

        static CreateItemsForCreatureEvent.CreateItemsForCreatureBuilder builder() {
            return new AutoValue_Events_CreateItemsForCreatureEvent.Builder();
        }

        public abstract Examinable.Name reason();

        @AutoValue.Builder
        public interface CreateItemsForCreatureBuilder {
            public abstract CreateItemsForCreatureBuilder setForCreature(CreatureID id);

            public abstract CreateItemsForCreatureBuilder setDescription(Optional<RichOutput> description);

            public abstract ImmutableList.Builder<ItemBuilderFactory.LockedItemBuilder> itemBuildersBuilder();

            public default CreateItemsForCreatureBuilder addItemBuilder(
                    ItemBuilderFactory.LockedItemBuilder... builder) {
                this.itemBuildersBuilder().add(builder);
                return this;
            }

            public default CreateItemsForCreatureBuilder addItemBuilders(
                    Collection<ItemBuilderFactory.LockedItemBuilder> builders) {
                this.itemBuildersBuilder().addAll(builders);
                return this;
            }

            public abstract CreateItemsForCreatureBuilder setReason(Examinable.Name reason);

            abstract EventRoutingBuilder routingBuilder();

            public default CreateItemsForCreatureBuilder setRouting(Consumer<EventRoutingBuilder> setter) {
                if (setter != null) {
                    setter.accept(this.routingBuilder());
                }
                return this;
            }

            public abstract CreateItemsForCreatureEvent build();

        }
    }

    public static CreateItemsForCreatureEvent.CreateItemsForCreatureBuilder itemForCreature() {
        return CreateItemsForCreatureEvent.builder();
    }

    @AutoValue
    public abstract sealed static class CreatureChangeEvent extends Event permits AutoValue_Events_CreatureChangeEvent {
        public abstract ImmutableList<CreatureEffect> effects();

        @Override
        public Optional<RichOutput> description() {
            final Builder builder = RichOutput.builder().setSequenceName("Effects on Creature")
                    .setOnEmpty(Optional.of("none"))
                    .setElementSeparator(Optional.of(RichOutputElement.ofString("\n - ")));
            this.effects().stream().filter(effect -> effect != null)
                    .forEachOrdered(effect -> builder.addExaminable(effect));
            return Optional.of(builder.build());
        }

        static CreatureChangeEvent.CreatureChangeEventBuilder builder() {
            return new AutoValue_Events_CreatureChangeEvent.Builder();
        }

        @AutoValue.Builder
        public static abstract class CreatureChangeEventBuilder {
            abstract ImmutableList.Builder<CreatureEffect> effectsBuilder();

            public abstract CreatureChangeEventBuilder setEffects(Iterable<CreatureEffect> effects);

            public abstract CreatureChangeEventBuilder setEffects(CreatureEffect... effects);

            public final CreatureChangeEventBuilder setEffects(Collection<CreatureEffect.Builder> effects) {
                return this.setEffects(effects.stream().filter(effectBuilder -> effectBuilder != null)
                        .map(effectBuilder -> effectBuilder.build()).toList());
            }

            public final CreatureChangeEventBuilder addEffect(CreatureEffect effect) {
                this.effectsBuilder().add(effect);
                return this;
            }

            public final CreatureChangeEventBuilder addEffect(CreatureEffect.Builder effect) {
                this.effectsBuilder().add(effect.build());
                return this;
            }

            abstract EventRoutingBuilder routingBuilder();

            public final CreatureChangeEventBuilder setRouting(Consumer<EventRoutingBuilder> setter) {
                if (setter != null) {
                    setter.accept(this.routingBuilder());
                }
                return this;
            }

            public abstract CreatureChangeEvent build();
        }
    }

    public static CreatureChangeEvent.CreatureChangeEventBuilder creatureChange() {
        return CreatureChangeEvent.builder();
    }

    @AutoValue
    public static sealed abstract class CreateItemsForRoomEvent extends Event
            permits AutoValue_Events_CreateItemsForRoomEvent {
        public abstract RoomID forRoom();

        public abstract ImmutableList<ItemBuilderFactory.LockedItemBuilder> itemBuilders();

        static CreateItemsForRoomEvent.CreateItemsForRoomBuilder builder() {
            return new AutoValue_Events_CreateItemsForRoomEvent.Builder();
        }

        public abstract Examinable.Name reason();

        @AutoValue.Builder
        public interface CreateItemsForRoomBuilder {
            public abstract CreateItemsForRoomBuilder setForRoom(RoomID id);

            public abstract CreateItemsForRoomBuilder setDescription(Optional<RichOutput> description);

            public abstract ImmutableList.Builder<ItemBuilderFactory.LockedItemBuilder> itemBuildersBuilder();

            public default CreateItemsForRoomBuilder addItemBuilder(ItemBuilderFactory.LockedItemBuilder... builder) {
                this.itemBuildersBuilder().add(builder);
                return this;
            }

            public default CreateItemsForRoomBuilder addItemBuilders(
                    Collection<ItemBuilderFactory.LockedItemBuilder> builders) {
                this.itemBuildersBuilder().addAll(builders);
                return this;
            }

            public abstract CreateItemsForRoomBuilder setReason(Examinable.Name reason);

            abstract EventRoutingBuilder routingBuilder();

            public default CreateItemsForRoomBuilder setRouting(Consumer<EventRoutingBuilder> setter) {
                if (setter != null) {
                    setter.accept(this.routingBuilder());
                }
                return this;
            }

            public abstract CreateItemsForRoomEvent build();

        }
    }

    public static CreateItemsForRoomEvent.CreateItemsForRoomBuilder itemForRoom() {
        return CreateItemsForRoomEvent.builder();
    }

    @AutoValue
    public static sealed abstract class CreateCreaturesForRoomEvent extends Event
            permits AutoValue_Events_CreateCreaturesForRoomEvent {
        public abstract RoomID forRoom();

        public abstract ImmutableList<CreatureBuilderFactory.Builder> creatureBuilders();

        static CreateCreaturesForRoomEvent.CreateCreaturesForRoomBuilder builder() {
            return new AutoValue_Events_CreateCreaturesForRoomEvent.Builder();
        }

        public abstract Examinable.Name reason();

        @AutoValue.Builder
        public interface CreateCreaturesForRoomBuilder {
            public abstract CreateCreaturesForRoomBuilder setForRoom(RoomID id);

            public abstract CreateCreaturesForRoomBuilder setDescription(Optional<RichOutput> description);

            public abstract ImmutableList.Builder<CreatureBuilderFactory.Builder> creatureBuildersBuilder();

            public default CreateCreaturesForRoomBuilder addCreatureBuilder(CreatureBuilderFactory.Builder... builder) {
                this.creatureBuildersBuilder().add(builder);
                return this;
            }

            public default CreateCreaturesForRoomBuilder addCreatureBuilders(
                    Collection<CreatureBuilderFactory.Builder> builders) {
                this.creatureBuildersBuilder().addAll(builders);
                return this;
            }

            public abstract CreateCreaturesForRoomBuilder setReason(Examinable.Name reason);

            abstract EventRoutingBuilder routingBuilder();

            public default CreateCreaturesForRoomBuilder setRouting(Consumer<EventRoutingBuilder> setter) {
                if (setter != null) {
                    setter.accept(this.routingBuilder());
                }
                return this;
            }

            public abstract CreateCreaturesForRoomEvent build();

        }
    }

    public static CreateCreaturesForRoomEvent.CreateCreaturesForRoomBuilder creatureForRoom() {
        return CreateCreaturesForRoomEvent.builder();
    }

    @AutoValue
    public abstract sealed static class RoomChangeEvent extends Event permits AutoValue_Events_RoomChangeEvent {
        public abstract ImmutableList<RoomEffect> effects();

        @Override
        public Optional<RichOutput> description() {
            final Builder builder = RichOutput.builder().setSequenceName("Effects on Room")
                    .setOnEmpty(Optional.of("none"))
                    .setElementSeparator(Optional.of(RichOutputElement.ofString("\n - ")));
            this.effects().stream().filter(effect -> effect != null)
                    .forEachOrdered(effect -> builder.addExaminable(effect));
            return Optional.of(builder.build());
        }

        static RoomChangeEvent.RoomChangeEventBuilder builder() {
            return new AutoValue_Events_RoomChangeEvent.Builder();
        }

        @AutoValue.Builder
        public static abstract class RoomChangeEventBuilder {
            abstract ImmutableList.Builder<RoomEffect> effectsBuilder();

            public abstract RoomChangeEventBuilder setEffects(Iterable<RoomEffect> effects);

            public abstract RoomChangeEventBuilder setEffects(RoomEffect... effects);

            public final RoomChangeEventBuilder setEffects(Collection<RoomEffect.Builder> effects) {
                return this.setEffects(effects.stream().filter(effectBuilder -> effectBuilder != null)
                        .map(effectBuilder -> effectBuilder.build()).toList());
            }

            public final RoomChangeEventBuilder addEffect(RoomEffect effect) {
                this.effectsBuilder().add(effect);
                return this;
            }

            public final RoomChangeEventBuilder addEffect(RoomEffect.Builder effect) {
                this.effectsBuilder().add(effect.build());
                return this;
            }

            abstract EventRoutingBuilder routingBuilder();

            public final RoomChangeEventBuilder setRouting(Consumer<EventRoutingBuilder> setter) {
                if (setter != null) {
                    setter.accept(this.routingBuilder());
                }
                return this;
            }

            public abstract RoomChangeEvent build();
        }
    }

    public static RoomChangeEvent.RoomChangeEventBuilder roomChange() {
        return RoomChangeEvent.builder();
    }

    @AutoValue
    public static sealed abstract class SeeEvent extends Event permits AutoValue_Events_SeeEvent {
        // TODO: some way to record who is watching
        // public abstract EntityReference<Entity> observer();

        @Override
        public final Optional<RichOutput> description() {
            return Optional.empty();
        }

        static SeeEventBuilder builder() {
            return new AutoValue_Events_SeeEvent.Builder();
        }

        @AutoValue.Builder
        public interface SeeEventBuilder {
            // public abstract SeeEventBuilder setObserver(EntityReference<Entity>
            // observer);

            abstract EventRoutingBuilder routingBuilder();

            public default SeeEventBuilder setRouting(Consumer<EventRoutingBuilder> setter) {
                if (setter != null) {
                    setter.accept(this.routingBuilder());
                }
                return this;
            }

            public abstract SeeEvent build();
        }
    }

    public static SeeEvent.SeeEventBuilder seeEvent() {
        return SeeEvent.builder();
    }

    @AutoValue
    public static sealed abstract class ViewedEvent extends Event permits AutoValue_Events_ViewedEvent {
        public abstract BasicExaminable observed();

        @Override
        public Optional<RichOutput> description() {
            return this.observed().description();
        }

        static ViewedEventBuilder builder() {
            return new AutoValue_Events_ViewedEvent.Builder();
        }

        @AutoValue.Builder
        public interface ViewedEventBuilder {
            public abstract ViewedEventBuilder setObserved(BasicExaminable observed);

            public default ViewedEventBuilder setObserved(Examinable observed) {
                return this.setObserved(observed.basicExaminable());
            }

            abstract EventRoutingBuilder routingBuilder();

            public default ViewedEventBuilder setRouting(Consumer<EventRoutingBuilder> setter) {
                if (setter != null) {
                    setter.accept(this.routingBuilder());
                }
                return this;
            }

            public abstract ViewedEvent build();
        }
    }

    public static ViewedEvent.ViewedEventBuilder viewedEvent() {
        return ViewedEvent.builder();
    }

    @AutoValue
    public static sealed abstract class SayEvent extends Event permits AutoValue_Events_SayEvent {
        public abstract RichOutput message();

        public abstract BasicExaminable speaker();

        public abstract Optional<BasicExaminable> listener();

        @Override
        public final Optional<RichOutput> description() {
            RichOutput.Builder output = RichOutput.builder().setSequenceName(this.speaker().name().toString())
                    .addTaggable(this.speaker()).addString("says");
            if (this.listener().isPresent()) {
                output.addString("to").addTaggable(this.listener().get());
            }
            output.addString(":\n")
                    .addOutput(RichOutput.builder().setSequenceName("message").addOutput(this.message()).build());
            return Optional.of(output.build());
        }

        public abstract SayEventBuilder toBuilder();

        public static SayEventBuilder builder() {
            return new AutoValue_Events_SayEvent.Builder();
        }

        @AutoValue.Builder
        public interface SayEventBuilder {
            abstract EventRoutingBuilder routingBuilder();

            public abstract SayEventBuilder setMessage(RichOutput message);

            abstract SayEventBuilder setSpeaker(BasicExaminable speaker);

            public default SayEventBuilder setSpeaker(Entity speaker) {
                this.routingBuilder().setReplyToSender(speaker);
                this.setSpeaker(speaker.basicExaminable());
                return this;
            }

            abstract SayEventBuilder setListener(Optional<BasicExaminable> listener);

            public default SayEventBuilder setListener(Entity listener) {
                this.routingBuilder().setDestination(listener);
                this.setListener(Optional.of(listener.basicExaminable()));
                return this;
            }

            public default SayEventBuilder setRouting(Consumer<EventRoutingBuilder> setter) {
                if (setter != null) {
                    setter.accept(this.routingBuilder());
                }
                return this;
            }

            public abstract SayEvent build();
        }
    }

    public static SayEvent.SayEventBuilder sayEvent() {
        return SayEvent.builder();
    }

    @AutoValue
    public abstract sealed static class ItemChangeEvent extends Event permits AutoValue_Events_ItemChangeEvent {
        public abstract ImmutableList<ItemEffect> effects();

        @Override
        public Optional<RichOutput> description() {
            final Builder builder = RichOutput.builder().setSequenceName("Effects on Item")
                    .setOnEmpty(Optional.of("none"))
                    .setElementSeparator(Optional.of(RichOutputElement.ofString("\n - ")));
            this.effects().stream().filter(effect -> effect != null)
                    .forEachOrdered(effect -> builder.addExaminable(effect));
            return Optional.of(builder.build());
        }

        static ItemChangeEvent.ItemChangeEventBuilder builder() {
            return new AutoValue_Events_ItemChangeEvent.Builder();
        }

        @AutoValue.Builder
        public static abstract class ItemChangeEventBuilder {
            abstract ImmutableList.Builder<ItemEffect> effectsBuilder();

            public abstract ItemChangeEventBuilder setEffects(Iterable<ItemEffect> effects);

            public abstract ItemChangeEventBuilder setEffects(ItemEffect... effects);

            public final ItemChangeEventBuilder setEffects(Collection<ItemEffect.Builder> effects) {
                return this.setEffects(effects.stream().filter(effectBuilder -> effectBuilder != null)
                        .map(effectBuilder -> effectBuilder.build()).toList());
            }

            public final ItemChangeEventBuilder addEffect(ItemEffect effect) {
                this.effectsBuilder().add(effect);
                return this;
            }

            public final ItemChangeEventBuilder addEffect(ItemEffect.Builder effect) {
                this.effectsBuilder().add(effect.build());
                return this;
            }

            abstract EventRoutingBuilder routingBuilder();

            public final ItemChangeEventBuilder setRouting(Consumer<EventRoutingBuilder> setter) {
                if (setter != null) {
                    setter.accept(this.routingBuilder());
                }
                return this;
            }

            public abstract ItemChangeEvent build();
        }
    }

    public static ItemChangeEvent.ItemChangeEventBuilder itemChange() {
        return ItemChangeEvent.builder();
    }

    public static final class AddressedBuilder {
        private EventRoutingBuilder routing = EventRouting.builder();

        public AddressedBuilder setDestination(URI destination) {
            this.routing.setDestination(destination);
            return this;
        }

        public AddressedBuilder setSender(URI sender) {
            this.routing.setSender(sender);
            return this;
        }

        public AddressedBuilder setReplyTo(URI replyTo) {
            this.routing.setReplyTo(replyTo);
            return this;
        }

        public AddressedBuilder setReplyToSender(URI sender) {
            this.routing.setReplyToSender(sender);
            return this;
        }

        public AddressedBuilder setDestinationAndSender(URI destination, URI sender) {
            this.routing.setDestinationAndSender(destination, sender);
            return this;
        }

        public AddressedBuilder reply(EventRouting other) {
            if (other != null) {
                this.setDestination(other.replyTo()).setReplyToSender(other.destination());
            }
            return this;
        }

        public void copyTo(EventRoutingBuilder t) {
            if (t != null) {
                t.setSender(this.routing.sender()).setReplyTo(this.routing.replyTo())
                        .setDestination(this.routing.destination());
            }
        }

        public PlainEvent.PlainEventBuilder plainEvent() {
            return Events.plainEvent().setRouting(this::copyTo);
        }

        public SeeEvent.SeeEventBuilder seeEvent() {
            return Events.seeEvent().setRouting(this::copyTo);
        }

        public ViewedEvent.ViewedEventBuilder viewedEvent() {
            return Events.viewedEvent().setRouting(this::copyTo);
        }

        public SayEvent.SayEventBuilder sayEvent() {
            return Events.sayEvent().setRouting(this::copyTo);
        }

    }

    public static AddressedBuilder addressed() {
        return new AddressedBuilder();
    }

}
