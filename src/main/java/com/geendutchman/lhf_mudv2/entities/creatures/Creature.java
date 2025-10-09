package com.geendutchman.lhf_mudv2.entities.creatures;

import java.io.Serializable;
import java.net.URI;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import com.geendutchman.lhf_mudv2.dice.DiceSet;
import com.geendutchman.lhf_mudv2.dice.DieType;
import com.geendutchman.lhf_mudv2.dice.Plain;
import com.geendutchman.lhf_mudv2.dice.RollSet;
import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory.Builder;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureBuilderFactory.NameGenerationStrategy;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemContainer;
import com.google.common.base.Preconditions;

public interface Creature extends Entity, ItemContainer {
    public record CreatureID(EntityID delegate) implements IEntityID {
        public CreatureID {
            Preconditions.checkNotNull(delegate, "CreatureID should not have a null delegate");
            Preconditions.checkState(delegate.entityClass().equals(ENTITY_CLASS_CREATURE),
                    "a creature id should be about creatures, but was '%s'", delegate.entityClass());
        }

        public static final Taggable.Tag ENTITY_CLASS_CREATURE = new Tag("creatures");

        public static CreatureID make(Examinable.Name name) {
            return new CreatureID(new EntityID(ENTITY_CLASS_CREATURE, name, UUID.randomUUID()));
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

        public record SetLocale(Optional<URI> locale) implements Delta {
            public SetLocale {
                Preconditions.checkNotNull(locale, "locale must not be null, but may be empty");
                Preconditions.checkArgument(locale.filter(l -> l.getQuery() == null).isPresent(),
                        "locale must not have a query, but had '%s'", locale.get().getQuery());
            }
        }

        public static Delta ofFaction(Faction faction) {
            return new SetFactionDelta(faction);
        }

        public static Delta ofItem(Item item) {
            return new AddItemDelta(item);
        }

        public static Delta ofScoreDelta(AttributeScores score, byte change) {
            return new SetAttributeScoreDelta(score, change);
        }

        public static Delta ofModifierDelta(AttributeScores mod, byte change) {
            return new SetAttributeModDelta(mod, change);
        }

        public static Delta ofLocale(Optional<URI> locale) {
            return new SetLocale(locale);
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
        return builder;
    }

}
