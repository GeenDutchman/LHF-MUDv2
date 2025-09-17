package com.geendutchman.lhf_mudv2.entities.room;

import java.io.Serializable;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.geendutchman.lhf_mudv2.entities.item.ItemInventory;
import com.geendutchman.lhf_mudv2.events.EventBus;
import com.geendutchman.lhf_mudv2.events.EventProcessor;
import com.google.auto.value.AutoBuilder;

@Component
public final class RoomBuilderFactory implements EventProcessor {

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

        public BuildRoom setEventFunction(@Nullable EventProcessor.EventFunction<Room> eventFunction);

        public Room build(RoomBuilderFactory factory);
    }

    @AutoBuilder(callMethod = "buildRoom", ofClass = ConcreteRoom.class)
    public abstract non-sealed static class Builder implements BuildRoom {
        final private UUID builderUuid = UUID.randomUUID();

        protected Builder() {
        }

        public final UUID builderUuid() {
            return this.builderUuid;
        }

        public abstract ItemInventory.Builder inventoryBuilder();

        // public abstract BuildRoom setInventory(ItemInventory inv);

        @Override
        public final BuildRoom addItem(ItemBuilderFactory.LockedItemBuilder... builder) {
            final ItemInventory.Builder set = this.inventoryBuilder();
            set.addContents(builder);
            return this;
        }

        abstract ConcreteRoom autoBuild();

        @Override
        public final Room build(RoomBuilderFactory factory) {
            if (factory == null) {
                throw new NullPointerException("factory must not be null");
            }
            final ConcreteRoom built = this.autoBuild();
            factory.repository.add(built);
            factory.bus.register(built);
            return built;
        }
    }

    private final RoomRepository repository;
    private final EventBus bus;
    private final URI processorURI;

    @Autowired
    public RoomBuilderFactory(RoomRepository repository, EventBus bus) {
        this.repository = repository;
        this.bus = bus;
        this.processorURI = UriComponentsBuilder.fromPath("/builderFactory/rooms").path(UUID.randomUUID().toString())
                .build().toUri();
        this.bus.register(this);
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
    public RoomBuilderFactory.BuilderStart builder() {
        final RoomBuilderFactory.Builder builder = new AutoBuilder_RoomBuilderFactory_Builder();
        return builder;
    }
}
