package com.geendutchman.lhf_mudv2.entities.room;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureQuery;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemInventory;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidFactory;
import com.google.auto.value.AutoBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

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

        public BuildRoom setLocale(Optional<IEntityID> locale);

        public BuildRoom setRoomDescription(Optional<RichOutput> roomDescription);

        public BuildRoom addItem(ItemBuilderFactory.LockedItemBuilder... builder);

        public BuildRoom addCreature(CreatureBuilderFactory.Builder... builder);

        public Room build(RoomBuilderFactory factory);
    }

    @AutoBuilder(callMethod = "buildRoom", ofClass = ConcreteRoom.class)
    public abstract non-sealed static class Builder implements BuildRoom {
        private final static TsidFactory idfactory = TsidFactory
                .newInstance1024(Math.abs("roomBuilder".hashCode() % 1024));
        final private Tsid builderTsid = idfactory.create();
        private ItemInventory.Builder inventoryBuilder = ItemInventory.builder();
        private SequencedSet<CreatureBuilderFactory.Builder> creatures;

        protected Builder() {
            this.inventoryBuilder = ItemInventory.builder();
            this.creatures = new LinkedHashSet<>();
        }

        public final Tsid builderTsid() {
            return this.builderTsid;
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

    public RoomBuilderFactory(@Autowired CreatureBuilderFactory creatureFactory,
            @Autowired ItemBuilderFactory itemFactory, @Autowired RoomRepository repository) {
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

    public synchronized void dualConnect(RoomID here, RoomID there, Directions hereToThere) {
        Preconditions.checkArgument(hereToThere != null, "direction here to there must not be null");

        final Optional<Room> optHereRoom = this.repository.byRoomID(here);
        final Optional<Room> optThereRoom = this.repository.byRoomID(there);

        Preconditions.checkArgument(optHereRoom.isPresent(), "location for connection start must exist: %s", here);
        Preconditions.checkArgument(optThereRoom.isPresent(), "location for connection end must exist: %s", there);

        final Room hereRoom = optHereRoom.get();
        final Room thereRoom = optThereRoom.get();

        final ImmutableSortedMap<Directions, Doorway> hereOut = hereRoom.doorways();
        final ImmutableSortedMap<Directions, Doorway> thereOut = thereRoom.doorways();

        if (hereOut.getOrDefault(hereToThere, null) == null) {
            hereRoom.applyDelta(
                    new Room.Delta.AddDoorway(hereToThere, new Doorway(there, CreatureQuery.builder().build())));
        }
        final Directions thereToHere = hereToThere.opposite();
        if (thereOut.getOrDefault(thereToHere, null) == null) {
            thereRoom.applyDelta(
                    new Room.Delta.AddDoorway(thereToHere, new Doorway(here, CreatureQuery.builder().build())));
        }
    }

    public synchronized void singleConnect(RoomID here, RoomID there, Directions hereToThere, CreatureQuery filter) {
        Preconditions.checkArgument(hereToThere != null, "direction here to there must not be null");
        final Optional<Room> optHereRoom = this.repository.byRoomID(here);
        final Optional<Room> optThereRoom = this.repository.byRoomID(there);

        Preconditions.checkArgument(optHereRoom.isPresent(), "location for connection start must exist: %s", here);
        Preconditions.checkArgument(optThereRoom.isPresent(), "location for connection end must exist: %s", there);

        Preconditions.checkArgument(filter != null, "filter may be empty of criteria but must not be null");

        final Room hereRoom = optHereRoom.get();
        final Doorway doorway = new Doorway(there, filter);
        hereRoom.applyDelta(new Room.Delta.AddDoorway(hereToThere, doorway));
    }
}
