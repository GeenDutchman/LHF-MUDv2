package com.geendutchman.lhf_mudv2.entities.item;

import java.io.Serializable;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.geendutchman.lhf_mudv2.dice.Difficulty;
import com.geendutchman.lhf_mudv2.dice.Plain;
import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureEffect;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.RoomEffect;
import com.geendutchman.lhf_mudv2.events.Event;
import com.geendutchman.lhf_mudv2.events.EventBus;
import com.geendutchman.lhf_mudv2.events.EventProcessor;
import com.geendutchman.lhf_mudv2.events.Events;
import com.geendutchman.lhf_mudv2.events.Events.CreatureChangeEvent;
import com.geendutchman.lhf_mudv2.events.Events.RoomChangeEvent;
import com.google.auto.value.AutoBuilder;
import com.google.common.collect.ImmutableList;

@Component
public final class ItemBuilderFactory implements EventProcessor {

    public static sealed interface BuilderStart extends Serializable permits BuildItem {
        public ItemBuilderFactory.BuildItem setName(@NonNull Examinable.Name name);

        public default ItemBuilderFactory.BuildItem setName(String name) {
            Examinable.Name eName = new Examinable.Name(name);
            return this.setName(eName);
        }
    }

    public static sealed interface BuildItem extends BuilderStart permits Builder {
        public Examinable.Name getName();

        public BuildItem setVisibility(Difficulty<Plain> visible);

        public BuildItem setNickname(Examinable.Name nickname);

        public BuildItem setNickname(Optional<Examinable.Name> nickname);

        public default BuildItem setNickname(String nickname) {
            Examinable.Name eName = new Examinable.Name(nickname);
            return this.setNickname(eName);
        }

        public Optional<Examinable.Name> getNickname();

        public BuildItem setItemTag(Item.ItemTag tag);

        public BuildItem setLocale(Optional<URI> locale);

        public BuildItem setEventFunction(@Nullable EventProcessor.EventFunction<Item> eventProcessor);

        public LockedItemBuilder lock();

        public Item build(ItemBuilderFactory factory);
    }

    public static sealed interface LockedItemBuilder extends Serializable, Comparable<LockedItemBuilder>
            permits Builder {
        public Item build(ItemBuilderFactory factory);

        public Examinable.Name getName();

        public Optional<Examinable.Name> getNickname();

        public UUID builderUuid();

        @Override
        public default int compareTo(LockedItemBuilder o) {
            return String
                    .format("%s:%s:%s", this.getName(), this.getNickname().map(aname -> aname.toString()).orElse(""),
                            this.builderUuid())
                    .compareTo(String.format("%s:%s:%s", o.getName(),
                            o.getNickname().map(oname -> oname.toString()).orElse(""), o.builderUuid()));
        }

    }

    @AutoBuilder(callMethod = "buildItem", ofClass = ConcreteItem.class)
    public non-sealed abstract static class Builder implements BuildItem, LockedItemBuilder {
        final private UUID builderUuid = UUID.randomUUID();

        protected Builder() {
        }

        public final UUID builderUuid() {
            return this.builderUuid;
        }

        @Override
        public final LockedItemBuilder lock() {
            return this;
        }

        protected abstract ConcreteItem autoBuild();

        @Override
        public final Item build(ItemBuilderFactory factory) {
            if (factory == null) {
                throw new NullPointerException("factory must be provided");
            }
            ConcreteItem built = this.autoBuild();
            factory.bus.register(built);
            factory.repository.add(built);
            return built;
        }

    }

    private final ItemRepository repository;
    private final EventBus bus;
    private final URI processorURI;

    @Autowired
    public ItemBuilderFactory(ItemRepository repository, EventBus bus) {
        this.repository = repository;
        this.bus = bus;
        this.processorURI = UriComponentsBuilder.fromPath("/builderFactory/items").build().toUri();
        this.bus.register(this);
    }

    @Bean({ "itembuilder", "itemBuilder" })
    @Scope("prototype")
    public static BuilderStart builder() {
        final AutoBuilder_ItemBuilderFactory_Builder builder = new AutoBuilder_ItemBuilderFactory_Builder();
        builder.setVisibility(Plain.noDifficulty()).setItemTag(Item.ItemTag.ITEM).setLocale(Optional.empty());
        return builder;
    }

    @Override
    public URI processorURI() {
        return this.processorURI;
    }

    @Override
    public Optional<URI> locale() {
        return Optional.of(this.processorURI);
    }

    @Override
    public ProcessingResult processEvent(Event event, EventBus bus) {
        switch (event) {
        case Events.CreateItemsForCreatureEvent cifce -> {
            ImmutableList<Creature.Delta> built = cifce.itemBuilders().stream().filter(lib -> lib != null)
                    .map(lib -> lib.build(this)).filter(item -> item != null).map(Creature.Delta::ofItem)
                    .collect(ImmutableList.toImmutableList());
            if (built.size() > 0) {
                CreatureChangeEvent forwarded = Events.creatureChange()
                        .addEffect(CreatureEffect.builder().addDeltas(built)
                                .setApplicationDescription(cifce.description()).setName(cifce.reason()))
                        .setRouting(routing -> {
                            routing.setDestination(cifce.forCreature().uri());
                            routing.setReplyTo(cifce.routing().replyTo());
                            routing.setSender(cifce.routing().sender());
                        }).build();
                bus.publish(forwarded);
            }
            return new ProcessingResult.Handled();
        }
        case Events.CreateItemsForRoomEvent cifre -> {
            ImmutableList<Room.Delta> built = cifre.itemBuilders().stream().filter(lib -> lib != null)
                    .map(lib -> lib.build(this)).filter(item -> item != null).map(Room.Delta::ofItem)
                    .collect(ImmutableList.toImmutableList());
            if (built.size() > 0) {
                RoomChangeEvent forwarded = Events
                        .roomChange().addEffect(RoomEffect.builder().addDeltas(built)
                                .setApplicationDescription(cifre.description()).setName(cifre.reason()))
                        .setRouting(routing -> {
                            routing.setDestination(cifre.forRoom().uri());
                            routing.setReplyTo(cifre.routing().replyTo());
                            routing.setSender(cifre.routing().sender());
                        }).build();
                bus.publish(forwarded);
            }
            return new ProcessingResult.Handled();
        }
        case null -> {
            return new ProcessingResult.Unhandled();
        }
        default -> {
            return new ProcessingResult.Unhandled();
        }
        }
    }

    @Bean
    public Item aRock() {
        return ItemBuilderFactory.builder().setName("defaultRock").build(this);
    }

}