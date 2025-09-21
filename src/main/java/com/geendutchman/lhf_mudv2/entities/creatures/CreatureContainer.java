package com.geendutchman.lhf_mudv2.entities.creatures;

import java.util.Optional;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

public interface CreatureContainer extends Examinable {
    public abstract ImmutableSet<Creature> creatures();

    public abstract boolean hasCreature(Creature creature);

    public abstract Optional<Creature> byCreatureID(CreatureID id);

    @Override
    public default Optional<RichOutput> description() {
        RichOutput.Builder builder = RichOutput.builder().setOnEmpty(Optional.of("It is empty"))
                .setTag(Optional.ofNullable(this.tag() + "-description"));
        this.creatures().forEach(creature -> builder.addTaggable(creature));
        return Optional.of(builder.build());
    }

    final static Taggable.Tag CREATURE_CONTAINER_TAG = new Taggable.Tag("Creatures");

    @Override
    public default Tag tag() {
        return CREATURE_CONTAINER_TAG;
    }

    @Override
    public default String content() {
        return this.name().toString();
    }

    public default Optional<Creature> queryOneCreature(CreatureQuery query) {
        return this.creatures().stream().sequential()
                .filter(creature -> query != null ? query.test(creature) : creature != null).findFirst();
    }

    public default Optional<Creature> queryOneCreature(IEntityQuery<? super Creature> query) {
        return this.creatures().stream().sequential()
                .filter(creature -> query != null ? query.test(creature) : creature != null).findFirst();
    }

    @Override
    public abstract ImmutableSortedMap<String, String> attributes();

    public default CreatureContainer queryCreatures(IEntityQuery<? super Creature> query) {
        ImmutableSortedMap.Builder<CreatureID, Creature> builder = ImmutableSortedMap.naturalOrder();
        this.creatures().stream().sequential()
                .filter(creature -> query != null ? query.test(creature) : creature != null)
                .forEach(creature -> builder.put(creature.creatureID(), creature));
        ImmutableSortedMap<CreatureID, Creature> built = builder.build();
        final Examinable.Name resultName = new Examinable.Name("CreatureQueryResult");

        return new CreatureContainer() {

            @Override
            public Name name() {
                return resultName;
            }

            @Override
            public ImmutableSet<Creature> creatures() {
                return ImmutableSet.copyOf(built.values());
            }

            @Override
            public boolean hasCreature(Creature creature) {
                return built.containsValue(creature);
            }

            @Override
            public Optional<Creature> byCreatureID(CreatureID id) {
                return Optional.ofNullable(built.get(id));
            }

            @Override
            public ImmutableSortedMap<String, String> attributes() {
                return ImmutableSortedMap.of();
            }

        };
    }

}
