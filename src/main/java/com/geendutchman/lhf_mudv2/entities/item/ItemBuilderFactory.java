package com.geendutchman.lhf_mudv2.entities.item;

import java.io.Serializable;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.dice.Difficulty;
import com.geendutchman.lhf_mudv2.dice.Plain;
import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.google.auto.value.AutoBuilder;

@Component
public final class ItemBuilderFactory {

    public static sealed interface BuilderStart extends Serializable permits BuildItem {
        public ItemBuilderFactory.BuildItem setName(Examinable.Name name);

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

        public BuildItem setLocale(Optional<IEntityID> locale);

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
            factory.repository.add(built);
            return built;
        }

    }

    private final ItemRepository repository;

    @Autowired
    public ItemBuilderFactory(ItemRepository repository) {
        this.repository = repository;
    }

    @Bean({ "itembuilder", "itemBuilder" })
    @Scope("prototype")
    public static BuilderStart builder() {
        final AutoBuilder_ItemBuilderFactory_Builder builder = new AutoBuilder_ItemBuilderFactory_Builder();
        builder.setVisibility(Plain.noDifficulty()).setItemTag(Item.ItemTag.ITEM).setLocale(Optional.empty());
        return builder;
    }

    @Bean
    public Item aRock() {
        return ItemBuilderFactory.builder().setName("defaultRock").build(this);
    }

}