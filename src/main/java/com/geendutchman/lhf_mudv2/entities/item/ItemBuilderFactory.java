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

import com.geendutchman.lhf_mudv2.commands.Command;
import com.geendutchman.lhf_mudv2.commands.CommandBus;
import com.geendutchman.lhf_mudv2.commands.CommandProcessor;
import com.geendutchman.lhf_mudv2.commands.LHFCommand;
import com.geendutchman.lhf_mudv2.dice.Difficulty;
import com.geendutchman.lhf_mudv2.dice.Plain;
import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureEffect;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.RoomEffect;
import com.geendutchman.lhf_mudv2.events.EventBus;
import com.geendutchman.lhf_mudv2.events.EventProcessor;
import com.google.auto.value.AutoBuilder;
import com.google.common.collect.ImmutableList;

@Component
public final class ItemBuilderFactory implements CommandProcessor {

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
            factory.bus.registerCommandProcessor(built);
            factory.eventBus.register(built);
            factory.repository.add(built);
            return built;
        }

    }

    private final ItemRepository repository;
    private final CommandBus bus;
    private final EventBus eventBus;
    private final URI processorURI;

    @Autowired
    public ItemBuilderFactory(ItemRepository repository, CommandBus bus, EventBus eventBus) {
        this.repository = repository;
        this.bus = bus;
        this.eventBus = eventBus;
        this.processorURI = UriComponentsBuilder.fromPath("/builderFactory/items").build().toUri();
        this.bus.registerCommandProcessor(this);
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
    public CommandResult processCommand(Command command) {
        if (command != null && command instanceof LHFCommand.CreateItemsForCreatureCommand cifcc) {
            Item built = cifcc.itemBuilder().build(this);
            Creature.Delta addItem = Creature.Delta.ofItem(built);
            CreatureEffect addItemEffect = CreatureEffect.builder().addDeltas(addItem).setName("Adding Item")
                    .setApplicationDescriptionFromBuilder(RichOutput.builder().addTaggable(cifcc.forCreature())
                            .addString("now has").addTaggable(built))
                    .setDescriptionFromBuilder(RichOutput.builder().addString("Adds item to creature")).build();
            LHFCommand.ChangeCreatureCommand crc = new LHFCommand.ChangeCreatureCommand(
                    new Command.CommandRouting(processorURI, cifcc.forCreature().uri()), UUID.randomUUID(),
                    ImmutableList.of(addItemEffect));
            return bus.send(crc);
        } else if (command != null && command instanceof LHFCommand.CreateItemsForRoomCommand cifrc) {
            Item built = cifrc.itemBuilder().build(this);
            Room.Delta addItem = Room.Delta.ofItem(built);
            RoomEffect addItemEffect = RoomEffect.builder().addDeltas(addItem).setName("Adding Item")
                    .setApplicationDescriptionFromBuilder(
                            RichOutput.builder().addTaggable(built).addString("is now in the room"))
                    .setDescriptionFromBuilder(RichOutput.builder().addString("Adds item to room")).build();
            LHFCommand.ChangeRoomCommand crc = new LHFCommand.ChangeRoomCommand(
                    new Command.CommandRouting(processorURI, cifrc.forRoom().uri()), UUID.randomUUID(),
                    ImmutableList.of(addItemEffect));
            return bus.send(crc);
        } else {
            return CommandProcessor.super.processCommand(command);
        }
    }

    @Bean
    public Item aRock() {
        return ItemBuilderFactory.builder().setName("defaultRock").build(this);
    }

}