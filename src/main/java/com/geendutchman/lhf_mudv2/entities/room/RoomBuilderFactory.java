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
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemInventory;
import com.google.auto.value.AutoBuilder;

@Component
public final class RoomBuilderFactory {

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
                built.applyDelta(Room.Delta.ofCreatureToAdd(builtCreature));
            }
            factory.repository.add(built);
            return built;
        }
    }

    private final CreatureBuilderFactory creatureFactory;
    private final ItemBuilderFactory itemFactory;
    private final RoomRepository repository;

    @Autowired
    public RoomBuilderFactory(CreatureBuilderFactory creatureFactory, ItemBuilderFactory itemFactory,
            RoomRepository repository) {
        this.creatureFactory = creatureFactory;
        this.itemFactory = itemFactory;
        this.repository = repository;
    }

    @Bean({ "roombuilder", "roomBuilder" })
    @Scope("prototype")
    public static RoomBuilderFactory.BuilderStart builder() {
        final RoomBuilderFactory.Builder builder = new AutoBuilder_RoomBuilderFactory_Builder();
        return builder;
    }
}
