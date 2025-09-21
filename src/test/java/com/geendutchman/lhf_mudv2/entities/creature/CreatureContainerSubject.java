package com.geendutchman.lhf_mudv2.entities.creature;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureContainer;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureQuery;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.IterableSubject;
import com.google.common.truth.OptionalSubject;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Truth;

public class CreatureContainerSubject extends IterableSubject {
    public static Factory<CreatureContainerSubject, CreatureContainer> creatureContainers() {
        return CreatureContainerSubject::new;
    }

    public static CreatureContainerSubject assertThat(CreatureContainer actual) {
        return Truth.assertAbout(creatureContainers()).that(actual);
    }

    private final CreatureContainer actual;

    protected CreatureContainerSubject(FailureMetadata metadata, CreatureContainer actual) {
        super(metadata, actual != null ? actual.creatures() : null);
        this.actual = actual;
    }

    public StringSubject name() {
        return check("getName()").that(actual.name().toString());
    }

    public IterableSubject creatures() {
        return check("getCreatures()").that(actual.creatures());
    }

    public CreatureContainerSubject queryAll(CreatureQuery query) {
        return check("queryAll(%s)", query).about(creatureContainers()).that(this.actual.queryCreatures(query));
    }

    public OptionalSubject queryOne(CreatureQuery query) {
        return check("queryOne(%s)", query).that(this.actual.queryOneCreature(query));
    }

    public void hasCreature(Creature creature) {
        this.creatures().contains(creature);
    }

    public void doesNotHaveCreature(Creature creature) {
        this.creatures().doesNotContain(creature);
    }

    @Override
    public void isEqualTo(@Nullable Object expected) {
        @SuppressWarnings("UndefinedEquals") // method contract requires testing iterables for equality
        boolean equal = Objects.equals(actual, expected);
        if (equal) {
            return;
        }

        if (expected instanceof CreatureContainer expectedIC) {
            containsExactlyElementsIn(expectedIC.creatures());
        } else {
            super.isEqualTo(expected);
        }
    }

}
