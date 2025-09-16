package com.geendutchman.lhf_mudv2.entities.creatures;

import java.io.Serializable;
import java.net.URI;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;

import com.geendutchman.lhf_mudv2.dice.D6Set;
import com.geendutchman.lhf_mudv2.dice.DiceSet;
import com.geendutchman.lhf_mudv2.dice.DieType;
import com.geendutchman.lhf_mudv2.dice.Plain;
import com.geendutchman.lhf_mudv2.dice.RollSet;
import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.Item.LockedItemBuilder;
import com.geendutchman.lhf_mudv2.entities.item.ItemContainer;
import com.geendutchman.lhf_mudv2.entities.item.ItemInventory;
import com.geendutchman.lhf_mudv2.events.EventBus;
import com.geendutchman.lhf_mudv2.events.EventProcessor;
import com.google.auto.value.AutoBuilder;
import com.google.auto.value.AutoOneOf;

import autovalue.shaded.com.google.common.base.Preconditions;

public interface Creature extends Entity, ItemContainer {
    public record CreatureID(EntityID delegate) implements IEntityID {
        public CreatureID {
            Preconditions.checkNotNull(delegate, "CreatureID should not have a null delegate");
            Preconditions.checkState(delegate.entityClass().equals("creatures"),
                    "a creature id should be about creatures, but was '%s'", delegate.entityClass());
        }

        public static CreatureID make(String name) {
            return new CreatureID(new EntityID("creatures", name, UUID.randomUUID()));
        }

        public String entityClass() {
            return this.delegate.entityClass();
        }

        public String name() {
            return this.delegate.name();
        }

        public URI uri() {
            return this.delegate.uri();
        }

        @Override
        public UUID uuid() {
            return this.delegate.uuid();
        }

        @Override
        public String toString() {
            return this.uri().toString();
        }
    }

    @Override
    public default IEntityID identifier() {
        return this.creatureID();
    }

    public abstract CreatureID creatureID();

    final static Taggable.Tag CREATURE_TAG = new Taggable.Tag("CREATURE");

    @Override
    default Tag tag() {
        final Faction faction = this.faction();
        return faction == null ? CREATURE_TAG : faction.tag();
    }

    // Creatuers can hold items in an inventory
    public abstract ItemInventory inventory();

    public abstract Faction faction();

    @Override
    public default String content() {
        return this.name().toString();
    }

    public int currentHealth();

    public int maximumHealth();

    public default ResourcePoolSize healthBucket() {
        return ResourcePoolSize.fromInts(this.currentHealth(), this.maximumHealth());
    }

    public default RollSet<AttributeScores> attributeCheck() {
        DiceSet.DiceSetBuilder<AttributeScores> builder = DiceSet.builder();
        for (AttributeScores forScore : AttributeScores.values()) {
            if (forScore == null) {
                continue;
            }
            builder = builder.addDie(DieType.TWENTY, (byte) 1, forScore).addBonus(forScore, this.getModifier(forScore));
        }
        return builder.build().roll();
    }

    public default RollSet<AttributeScores> attributeCheck(AttributeScores forScore) {
        return DiceSet.<AttributeScores>builder().addDie(DieType.TWENTY, (byte) 1, forScore)
                .addBonus(forScore, this.getModifier(forScore)).build().roll();
    }

    public default RollSet<Plain> plainCheck(AttributeScores forScore) {
        return DiceSet.<Plain>builder().addDie(DieType.TWENTY, (byte) 1, Plain.UNFLAVORED)
                .addBonus(Plain.UNFLAVORED, this.getModifier(forScore)).build().roll();
    }

    public byte getScore(final AttributeScores score);

    public byte getModifier(final AttributeScores mod);

    @AutoOneOf(Delta.Kind.class)
    public static abstract class Delta implements Serializable {
        public enum Kind {
            FACTION, INVENTORY_ITEM, INVENTORY_ITEM_BUILDER, SCORE_DELTA, MODIFIER_DELTA;
        }

        public abstract Kind kind();

        public abstract Optional<Faction> faction();

        public abstract Optional<Item> inventoryItem();

        public abstract Optional<Item.LockedItemBuilder> inventoryItemBuilder();

        public abstract Optional<AbstractMap.SimpleImmutableEntry<AttributeScores, Byte>> scoreDelta();

        public abstract Optional<AbstractMap.SimpleImmutableEntry<AttributeScores, Byte>> modifierDelta();

        public static Delta ofFaction(Faction faction) {
            return AutoOneOf_Creature_Delta.faction(Optional.of(faction));
        }

        public static Delta ofItem(Item item) {
            return AutoOneOf_Creature_Delta.inventoryItem(Optional.of(item));
        }

        public static Delta ofItemBuilder(Item.LockedItemBuilder itemBuilder) {
            return AutoOneOf_Creature_Delta.inventoryItemBuilder(Optional.of(itemBuilder));
        }

        public static Delta ofItemBuilder(Item.BuildItem itemBuilder) {
            return AutoOneOf_Creature_Delta.inventoryItemBuilder(Optional.of(itemBuilder.lock()));
        }

        public static Delta ofScoreDelta(AttributeScores score, byte change) {
            Preconditions.checkArgument(score != null, "score should not be null");
            return AutoOneOf_Creature_Delta.scoreDelta(
                    Optional.of(new AbstractMap.SimpleImmutableEntry<AttributeScores, Byte>(score, change)));
        }

        public static Delta ofModifierDelta(AttributeScores mod, byte change) {
            Preconditions.checkArgument(mod != null, "mod should not be null");
            return AutoOneOf_Creature_Delta.modifierDelta(
                    Optional.of(new AbstractMap.SimpleImmutableEntry<AttributeScores, Byte>(mod, change)));
        }

    }

    public abstract void applyDelta(Delta delta);

    public static class CreatureComparator implements Comparator<Creature>, Serializable {
        private static Comparator<Entity> delegate = Entity.getEntityComparator();

        @Override
        public int compare(Creature o1, Creature o2) {
            if (o1 == null || o2 == null) {
                throw new NullPointerException("Cannot compare null Creatures");
            }
            if (o1.equals(o2)) {
                return 0;
            }
            return delegate.compare(o1, o2);
        }
    }

    public static Comparator<Creature> getCreatureComparator() {
        return new CreatureComparator();
    }

    @AutoBuilder(callMethod = "buildCreature", ofClass = Creature.class)
    public abstract static class Builder implements Serializable {

        @Autowired
        public abstract Builder setCreatureRepository(CreatureRepository creatureRepository);

        @Autowired
        public abstract Builder setEventBus(EventBus eventBus);

        public abstract Builder setName(Examinable.Name name);

        public Builder setName(String name) {
            Examinable.Name eName = new Examinable.Name(name);
            return this.setName(eName);
        }

        public abstract Builder setFaction(Faction faction);

        public abstract Builder setVitals(Map<CreatureStats, Integer> vitals);

        protected abstract Map<CreatureStats, Integer> vitals();

        public Builder setHealth(int maxhealth) {
            Map<CreatureStats, Integer> vitals = this.vitals();
            if (vitals == null) {
                vitals = new EnumMap<>(CreatureStats.class);
            }
            vitals.put(CreatureStats.MAX_HEALTH, maxhealth);
            vitals.put(CreatureStats.HEALTH, maxhealth);
            return this.setVitals(vitals);
        }

        public abstract ItemInventory.Builder inventoryBuilder();

        public final Builder addItem(LockedItemBuilder... builder) {
            final ItemInventory.Builder set = this.inventoryBuilder();
            set.addContents(builder);
            return this;
        }

        public abstract Builder setScores(Map<AttributeScores, Byte> scores);

        protected abstract Map<AttributeScores, Byte> scores();

        public final Builder scores4d6DropLowest() {
            Map<AttributeScores, Byte> nextScores = new EnumMap<>(AttributeScores.class);
            for (final AttributeScores scoretype : AttributeScores.values()) {
                nextScores.put(scoretype, D6Set.fourD6DropLowestAsByte());
            }
            return this.setScores(nextScores);
        }

        public abstract Builder setScoreModifierBonuses(Map<AttributeScores, Byte> bonuses);

        public abstract Builder setEventFunction(@Nullable EventProcessor.EventFunction<Creature> eventProcessor);

        public abstract Creature build();

    }

    public static Creature.Builder builder() {
        return new AutoBuilder_Creature_Builder();
    }

    public static Creature buildCreature(EventBus eventBus, CreatureRepository creatureRepository, Examinable.Name name,
            ItemInventory inventory, Faction faction, Map<AttributeScores, Byte> scores,
            Map<AttributeScores, Byte> scoreModifierBonuses, Map<CreatureStats, Integer> vitals,
            @Nullable EventProcessor.EventFunction<Creature> eventFunction) {
        Preconditions.checkNotNull(eventBus, "event bus must not be null");
        Preconditions.checkNotNull(creatureRepository, "creature repository must be available to store creature into");

        final ConcreteCreature creature = ConcreteCreature.buildCreature(name, inventory, faction, scores,
                scoreModifierBonuses, vitals, eventFunction);
        eventBus.register(creature);
        creatureRepository.add(creature);
        return creature;
    }

}
