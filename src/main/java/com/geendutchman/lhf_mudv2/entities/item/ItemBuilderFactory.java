package com.geendutchman.lhf_mudv2.entities.item;

import java.io.Serializable;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.entities.item.Item.ItemTag;
import com.geendutchman.lhf_mudv2.events.EventBus;
import com.google.auto.value.AutoBuilder;

@Component
public final class ItemBuilderFactory {
    public static sealed interface BuilderStart extends Serializable permits ItemBuilderFactory.BuildItem {
        public ItemBuilderFactory.BuildItem setName(String name);
    }

    public static sealed interface BuildItem extends BuilderStart permits ItemBuilderFactory.Builder {

        public BuildItem setVisible(boolean visible);

        public BuildItem setNickname(Optional<String> nickname);

        public BuildItem setItemTag(Item.ItemTag tag);

        public BuildItem setLocale(Optional<URI> locale);

        public LockedItemBuilder lock();

        public Item build();
    }

    public static sealed interface LockedItemBuilder extends Serializable permits ItemBuilderFactory.Builder {
        public Item build();
    }

    @AutoBuilder(callMethod = "buildItem", ofClass = ConcreteItem.class)
    abstract non-sealed static class Builder implements BuildItem, LockedItemBuilder {
        final private UUID builderUuid = UUID.randomUUID();

        @Autowired
        private transient ItemRepository repository;

        @Autowired
        private transient EventBus eventBus;

        final void setRepository(ItemRepository repository) {
            this.repository = repository;
        }

        final void setEventBus(EventBus eventBus) {
            this.eventBus = eventBus;
        }

        public final UUID builderUuid() {
            return this.builderUuid;
        }

        @Override
        public final LockedItemBuilder lock() {
            return this;
        }

        abstract ConcreteItem autoBuild();

        @Override
        public final Item build() {
            final ConcreteItem built = this.autoBuild();
            this.eventBus.register(built);
            return this.repository.addItem(built);
        }

    }

    private final ItemRepository repository;
    private final EventBus bus;

    @Autowired
    public ItemBuilderFactory(ItemRepository repository, EventBus bus) {
        this.repository = repository;
        this.bus = bus;
    }

    @Bean
    @Scope("prototype")
    public ItemBuilderFactory.BuilderStart builder() {
        final ItemBuilderFactory.Builder builder = new AutoBuilder_ItemBuilderFactory_Builder();
        builder.setEventBus(bus);
        builder.setRepository(repository);
        builder.setVisible(true).setItemTag(ItemTag.ITEM);
        builder.setLocale(Optional.empty());
        return builder;
    }

    @Bean
    public Item aRock() {
        return this.builder().setName("defaultRock").build();
    }

}