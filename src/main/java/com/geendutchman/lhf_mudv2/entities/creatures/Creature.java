package com.geendutchman.lhf_mudv2.entities.creatures;

import java.io.Serializable;
import java.net.URI;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Optional;

import com.geendutchman.lhf_mudv2.dice.DiceSet;
import com.geendutchman.lhf_mudv2.dice.DieType;
import com.geendutchman.lhf_mudv2.dice.Plain;
import com.geendutchman.lhf_mudv2.dice.RollSet;
import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory.Builder;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory.NameGenerationStrategy;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemContainer;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

public interface Creature extends Entity, ItemContainer {
    public record CreatureID(EntityID delegate) implements IEntityID {
        public CreatureID {
            Preconditions.checkNotNull(delegate, "CreatureID should not have a null delegate");
            Preconditions.checkState(delegate.entityClass().equals(ENTITY_CLASS_CREATURE),
                    "a creature id should be about creatures, but was '%s'", delegate.entityClass());
        }

        public static CreatureID fromString(final String value) {
            try {
                final EntityID delegate = EntityID.fromString(value);
                final CreatureID id = new CreatureID(delegate);
                return id;
            } catch (IllegalArgumentException | IllegalStateException e) {
                throw new IllegalArgumentException("Cannot create Creature ID", e);
            }
        }

        public static final Taggable.Tag ENTITY_CLASS_CREATURE = new Tag("creatures");
        protected static final TsidFactory tsidFactory = TsidFactory
                .newInstance1024(Math.abs("creatures".hashCode() % 1024));

        public static CreatureID make(Examinable.Name name) {
            return new CreatureID(new EntityID(ENTITY_CLASS_CREATURE, name, tsidFactory.create()));
        }

        public Taggable.Tag entityClass() {
            return this.delegate.entityClass();
        }

        public Examinable.Name name() {
            return this.delegate.name();
        }

        public URI uri() {
            return this.delegate.uri();
        }

        @Override
        public Tsid tsid() {
            return this.delegate.tsid();
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

    @Override
    public default Optional<RichOutput> description() {
        return Optional.of(RichOutput.builder().addString("A creature named").addPolymorphic(this.name().toString())
                .addString("of the").addPolymorphic(this.faction()).addString("faction, whose health is")
                .addPolymorphic(this.healthBucket().toString()).addString(".").build());
    }

    @Override
    public default ImmutableSortedMap<String, String> properties() {
        return ImmutableSortedMap.<String, String>naturalOrder().putAll(Entity.super.properties())
                .put("faction", this.faction().toString()).put("healthBucket", this.healthBucket().toString()).build();
    }

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

    public static sealed interface Delta extends Serializable {
        public record SetFactionDelta(Faction faction) implements Delta {
            public SetFactionDelta {
                Preconditions.checkNotNull(faction, "faction must not be null");
            }
        }

        public record AddItemDelta(Item inventoryItem) implements Delta {
            public AddItemDelta {
                Preconditions.checkNotNull(inventoryItem, "inventory item not null");
            }
        }

        public record RemoveItemDelta(Item item) implements Delta {
            public RemoveItemDelta {
                Preconditions.checkNotNull(item, "item to remove must not be null");
            }
        }

        public record SetAttributeScoreDelta(AttributeScores attr, byte amount) implements Delta {
            public SetAttributeScoreDelta {
                Preconditions.checkNotNull(attr, "attribute must not be null");
            }
        }

        public record SetAttributeModDelta(AttributeScores attr, byte amount) implements Delta {
            public SetAttributeModDelta {
                Preconditions.checkNotNull(attr, "attribute must not be null");
            }
        }

        public record SetLocale(Optional<IEntityID> locale) implements Delta {
            public SetLocale {
                Preconditions.checkNotNull(locale, "locale must not be null, but may be empty");
            }
        }

        public record SetProperty(String key, String value) implements Delta {
            public SetProperty {
                Preconditions.checkNotNull(key, "key must not be null");
            }
        }

        public static Delta ofFaction(Faction faction) {
            return new SetFactionDelta(faction);
        }

        public static Delta ofItemToAdd(Item item) {
            return new AddItemDelta(item);
        }

        public static Delta ofItemToRemove(Item item) {
            return new RemoveItemDelta(item);
        }

        public static Delta ofScoreDelta(AttributeScores score, byte change) {
            return new SetAttributeScoreDelta(score, change);
        }

        public static Delta ofModifierDelta(AttributeScores mod, byte change) {
            return new SetAttributeModDelta(mod, change);
        }

        public static Delta ofLocale(Optional<IEntityID> locale) {
            return new SetLocale(locale);
        }

        public static Delta ofProperty(String key, String value) {
            return new SetProperty(key, value);
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

    public default CreatureBuilderFactory.Builder toBuilder() {
        Builder builder = CreatureBuilderFactory.builder().setHealth(this.maximumHealth()).useRandomName();
        final String[] splits = this.name().toString().split(" ");
        if (splits.length > 1) {
            for (int i = splits.length - 1; i >= 0; i--) {
                if (i == 0) {
                    builder.useRandomName();
                    break;
                }
                String last = splits[i];
                if (last.matches("Count\\d+")) {
                    continue;
                }
                try {
                    Examinable.Name made = new Examinable.Name(last);
                    builder.setNameGenerationStrategy(
                            new NameGenerationStrategy(NameGenerationStrategy.Kind.PIN_LASTNAME, Optional.of(made)));
                    break;
                } catch (IllegalArgumentException e) {
                    continue;
                }
            }
        }
        for (AttributeScores score : AttributeScores.values()) {
            byte retrieved = this.getScore(score);
            builder.setScore(score, retrieved);
        }
        for (Entry<String, String> propEntry : this.properties().entrySet()) {
            builder.addProperty(propEntry.getKey(), propEntry.getValue());
        }
        return builder;
    }

}
