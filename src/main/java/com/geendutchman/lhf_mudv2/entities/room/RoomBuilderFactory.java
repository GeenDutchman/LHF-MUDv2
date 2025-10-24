package com.geendutchman.lhf_mudv2.entities.room;

import java.io.Serializable;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.geendutchman.lhf_mudv2.commands.CommandBus;
import com.geendutchman.lhf_mudv2.commands.CommandProcessor;
import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemInventory;
import com.geendutchman.lhf_mudv2.events.EventBus;
import com.geendutchman.lhf_mudv2.events.EventProcessor;
import com.google.auto.value.AutoBuilder;

@Component
public final class RoomBuilderFactory implements CommandProcessor {

    public static sealed interface BuilderStart extends Serializable permits BuildRoom {
        public BuildRoom setName(Examinable.Name name);

        public default BuildRoom setName(String name) {
            Examinable.Name eName = new Examinable.Name(name);
            return this.setName(eName);
        }

    }

    public static sealed interface BuildRoom extends BuilderStart permits Builder {
        public Examinable.Name getName();

        public BuildRoom setLocale(Optional<URI> locale);

        public BuildRoom setRoomDescription(Optional<RichOutput> roomDescription);

        public BuildRoom addItem(ItemBuilderFactory.LockedItemBuilder... builder);

        public BuildRoom addCreature(CreatureBuilderFactory.Builder... builder);

        public BuildRoom setEventFunction(@Nullable EventProcessor.EventFunction<Room> eventFunction);

        public Room build(RoomBuilderFactory factory);
    }

    @AutoBuilder(callMethod = "buildRoom", ofClass = ConcreteRoom.class)
    public abstract non-sealed static class Builder implements BuildRoom {
        final private UUID builderUuid = UUID.randomUUID();
        private ItemInventory.Builder inventoryBuilder = ItemInventory.builder();
        private SequencedSet<CreatureBuilderFactory.Builder> creatures;

        protected Builder() {
            this.inventoryBuilder = ItemInventory.builder();
            this.creatures = new LinkedHashSet<>();
        }

        public final UUID builderUuid() {
            return this.builderUuid;
        }

        public ItemInventory.Builder inventoryBuilder() {
            return this.inventoryBuilder;
        }

        protected abstract BuildRoom setInventory(ItemInventory inv);

        @Override
        public final BuildRoom addItem(ItemBuilderFactory.LockedItemBuilder... builder) {
            final ItemInventory.Builder set = this.inventoryBuilder();
            set.addContents(builder);
            return this;
        }

        @Override
        public BuildRoom addCreature(CreatureBuilderFactory.Builder... builders) {
            for (CreatureBuilderFactory.Builder builder : creatures) {
                if (builder == null) {
                    continue;
                }
                this.creatures.add(builder);
            }
            return this;
        }

        abstract ConcreteRoom autoBuild();

        @Override
        public final Room build(RoomBuilderFactory factory) {
            if (factory == null) {
                throw new NullPointerException("factory must not be null");
            }
            ItemInventory items = this.inventoryBuilder.build(factory.itemFactory);
            this.setInventory(items);
            final ConcreteRoom built = this.autoBuild();
            this.setInventory(ItemInventory.builder().build(factory.itemFactory)); // clear it
            for (final CreatureBuilderFactory.Builder builder : creatures) {
                if (builder == null) {
                    continue;
                }
                Creature builtCreature = builder.build(factory.creatureFactory);
                built.applyDelta(Room.Delta.ofCreature(builtCreature));
            }
            factory.repository.add(built);
            factory.bus.registerCommandProcessor(built);
            factory.eventBus.register(built);
            return built;
        }
    }

    private final CreatureBuilderFactory creatureFactory;
    private final ItemBuilderFactory itemFactory;
    private final RoomRepository repository;
    private final CommandBus bus;
    private final EventBus eventBus;
    private final URI processorURI;

    @Autowired
    public RoomBuilderFactory(CreatureBuilderFactory creatureFactory, ItemBuilderFactory itemFactory,
            RoomRepository repository, CommandBus bus, EventBus eventBus) {
        this.creatureFactory = creatureFactory;
        this.itemFactory = itemFactory;
        this.repository = repository;
        this.bus = bus;
        this.eventBus = eventBus;
        this.processorURI = UriComponentsBuilder.fromPath("/builderFactory/rooms").build().toUri();
        this.bus.registerCommandProcessor(this);
    }

    @Override
    public URI processorURI() {
        return this.processorURI;
    }

    @Override
    public Optional<URI> locale() {
        return Optional.of(this.processorURI);
    }

    @Bean({ "roombuilder", "roomBuilder" })
    @Scope("prototype")
    public static RoomBuilderFactory.BuilderStart builder() {
        final RoomBuilderFactory.Builder builder = new AutoBuilder_RoomBuilderFactory_Builder();
        return builder;
    }
}
