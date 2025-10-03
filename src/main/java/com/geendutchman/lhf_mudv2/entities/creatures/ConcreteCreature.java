package com.geendutchman.lhf_mudv2.entities.creatures;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.springframework.lang.Nullable;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.geendutchman.lhf_mudv2.entities.item.ItemInventory;
import com.geendutchman.lhf_mudv2.events.Event;
import com.geendutchman.lhf_mudv2.events.EventBus;
import com.geendutchman.lhf_mudv2.events.EventProcessor;
import com.geendutchman.lhf_mudv2.events.Events;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

final class ConcreteCreature implements Creature {
    final private CreatureID creatureID;
    final private Examinable.Name name;
    final private ItemInventory inventory;

    private Optional<URI> locale;
    private Faction faction;

    private final ConcurrentNavigableMap<AttributeScores, Byte> scores;
    private final ConcurrentNavigableMap<AttributeScores, Byte> scoreModBonuses;
    private final ConcurrentNavigableMap<CreatureStats, Integer> vitals;

    @Nullable
    final private transient EventProcessor.EventFunction<Creature> eventFunction;

    protected static ConcreteCreature buildCreature(Examinable.Name name, ItemInventory inventory, Faction faction,
            Map<AttributeScores, Byte> scores, Map<AttributeScores, Byte> scoreModifierBonuses,
            Map<CreatureStats, Integer> vitals, @Nullable EventProcessor.EventFunction<Creature> eventFunction) {
        Preconditions.checkNotNull(name, "name should not be null");
        Preconditions.checkNotNull(inventory, "inventory should not be null");
        Preconditions.checkNotNull(scores, "scores may be empty, but must not be null");
        Preconditions.checkNotNull(scoreModifierBonuses, "score modifier bonuses may be empty, but must not be null");
        Preconditions.checkNotNull(vitals, "vitals should not be null");

        return new ConcreteCreature(name, inventory, faction, scores, scoreModifierBonuses, vitals, eventFunction);
    }

    private ConcreteCreature(Examinable.Name name, ItemInventory inventory, Faction faction,
            Map<AttributeScores, Byte> scores, Map<AttributeScores, Byte> scoreModifierBonuses,
            Map<CreatureStats, Integer> vitals, @Nullable EventProcessor.EventFunction<Creature> eventFunction) {
        this.name = name;
        this.inventory = inventory;
        this.faction = faction != null ? faction : Faction.RENEGADE;
        this.creatureID = CreatureID.make(name);
        this.eventFunction = eventFunction != null ? eventFunction : (e, b, c) -> new ProcessingResult.Unhandled();
        this.scores = new ConcurrentSkipListMap<>(scores);
        this.scoreModBonuses = new ConcurrentSkipListMap<>(scoreModifierBonuses);
        this.vitals = new ConcurrentSkipListMap<>(vitals);
        this.vitals.computeIfAbsent(CreatureStats.MAX_HEALTH, k -> this.vitals.getOrDefault(CreatureStats.HEALTH, 10));
        this.vitals.computeIfAbsent(CreatureStats.HEALTH, k -> this.vitals.getOrDefault(CreatureStats.MAX_HEALTH, 10));
        this.locale = Optional.empty();
    }

    @Override
    public Faction faction() {
        return this.faction;
    }

    @Override
    public URI processorURI() {
        return this.creatureID.uri();
    }

    @Override
    public Optional<URI> locale() {
        return this.locale;
    }

    public void setLocale(Optional<URI> nextPlace) {
        if (nextPlace == null) {
            this.locale = Optional.empty();
        } else {
            this.locale = nextPlace;
        }
    }

    @Override
    public ProcessingResult processEvent(Event event, EventBus bus) {
        if (this.eventFunction != null) {
            ProcessingResult result = this.eventFunction.apply(event, bus, this);
            if (result instanceof ProcessingResult.Handled) {
                return result;
            }
        }
        if (event != null && event instanceof Events.CreatureChangeEvent cce) {
            for (final CreatureEffect creatureEffect : cce.effects()) {
                for (final Creature.Delta delta : creatureEffect.deltas()) {
                    this.applyDelta(delta);
                }
            }
            return new ProcessingResult.Handled();
        }
        return new ProcessingResult.Unhandled();
    }

    @Override
    public CreatureID creatureID() {
        return this.creatureID;
    }

    @Override
    public Name name() {
        return this.name;
    }

    public boolean hasItem(Item item) {
        return inventory.hasItem(item);
    }

    public Optional<Item> byItemID(ItemID id) {
        return inventory.byItemID(id);
    }

    public ImmutableSet<Item> items() {
        return inventory.items();
    }

    @Override
    public ImmutableSortedMap<String, String> attributes() {
        return ImmutableSortedMap.copyOf(Taggable.produceBasicTagAttributes());
    }

    @Override
    public int currentHealth() {
        return this.vitals.getOrDefault(CreatureStats.HEALTH, 0);
    }

    @Override
    public int maximumHealth() {
        return this.vitals.getOrDefault(CreatureStats.MAX_HEALTH, 0);
    }

    @Override
    public byte getScore(final AttributeScores score) {
        if (score == null) {
            return 0;
        }
        final byte retrieved = this.scores.getOrDefault(score, (byte) 0);
        return retrieved;
    }

    private static byte addBytesCapped(byte one, byte two) {
        int sum = one + two; // as int to avoid overflow

        // Cap the result to Byte.MAX_VALUE and Byte.MIN_VALUE
        if (sum > Byte.MAX_VALUE) {
            return Byte.MAX_VALUE; // Cap to maximum byte value
        } else if (sum < Byte.MIN_VALUE) {
            return Byte.MIN_VALUE; // Cap to minimum byte value
        } else {
            return (byte) sum; // Return the valid sum
        }
    }

    @Override
    public byte getModifier(final AttributeScores mod) {
        if (mod == null) {
            return 0;
        }
        final byte score = this.getScore(mod);
        final byte calulated = AttributeScores.calulateModifier(score);
        final byte retrieved = this.scoreModBonuses.getOrDefault(mod, (byte) 0);
        return addBytesCapped(calulated, retrieved);
    }

    @Override
    public void applyDelta(Delta delta) {
        if (delta == null) {
            return;
        }
        switch (delta.kind()) {
        case FACTION:
            delta.faction().ifPresent(f -> this.faction = f);
            break;
        case INVENTORY_ITEM:
            delta.inventoryItem().ifPresent(item -> {
                this.inventory.add(item);
                item.applyDelta(Item.Delta.ofLocale(Optional.of(this.identifier().uri())));
            });
            break;

        case MODIFIER_DELTA:
            delta.modifierDelta().ifPresent(modEntry -> {
                this.scoreModBonuses.merge(modEntry.getKey(), modEntry.getValue(), ConcreteCreature::addBytesCapped);
            });
            break;
        case SCORE_DELTA:
            delta.scoreDelta().ifPresent(scoreEntry -> {
                this.scores.merge(scoreEntry.getKey(), scoreEntry.getValue(), ConcreteCreature::addBytesCapped);
            });
            break;
        default:
            break;

        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ConcreteCreature [creatureID=").append(creatureID).append(", name=").append(name)
                .append(", locale=").append(locale).append(", vitals=").append(vitals).append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(creatureID);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof ConcreteCreature))
            return false;
        ConcreteCreature other = (ConcreteCreature) obj;
        return Objects.equals(creatureID, other.creatureID);
    }

}
