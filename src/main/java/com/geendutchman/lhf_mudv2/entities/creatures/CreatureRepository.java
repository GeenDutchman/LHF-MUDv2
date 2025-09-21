package com.geendutchman.lhf_mudv2.entities.creatures;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

import org.springframework.stereotype.Repository;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

@Repository
public final class CreatureRepository implements CreatureContainer {
    private final ConcurrentSkipListMap<CreatureID, ConcreteCreature> beings = new ConcurrentSkipListMap<>();

    public CreatureRepository add(ConcreteCreature... creatures) {
        if (creatures != null) {
            for (final ConcreteCreature creature : creatures) {
                if (creature != null) {
                    this.beings.put(creature.creatureID(), creature);
                }
            }
        }
        return this;
    }

    public CreatureRepository add(Collection<ConcreteCreature> creatures) {
        if (creatures != null) {
            for (final ConcreteCreature creature : creatures) {
                if (creature != null) {
                    this.beings.put(creature.creatureID(), creature);
                }
            }
        }
        return this;
    }

    public CreatureRepository addAll(Map<CreatureID, ConcreteCreature> creatures) {
        if (creatures != null) {
            this.beings.putAll(creatures);
        }
        return this;
    }

    public Optional<Creature> remove(CreatureID id) {
        return Optional.ofNullable(this.beings.remove(id));
    }

    public Optional<Creature> remove(Creature creature) {
        return this.remove(creature.creatureID());
    }

    @Override
    public ImmutableSet<Creature> creatures() {
        return ImmutableSet.copyOf(this.beings.values());
    }

    @Override
    public boolean hasCreature(Creature creature) {
        return this.beings.containsValue(creature);
    }

    @Override
    public Optional<Creature> byCreatureID(CreatureID id) {
        return Optional.ofNullable(this.beings.get(id));
    }

    final static Examinable.Name CREATURE_REPO_NAME = new Examinable.Name("CreatureRepository");

    @Override
    public Name name() {
        return CREATURE_REPO_NAME;
    }

    @Override
    public ImmutableSortedMap<String, String> attributes() {
        return ImmutableSortedMap.of();
    }

}
