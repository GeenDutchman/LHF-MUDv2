package com.geendutchman.lhf_mudv2.entities.creature;

import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.ResourcePoolSize;
import com.geendutchman.lhf_mudv2.entities.item.ItemContainerSubject;
import com.google.common.truth.ComparableSubject;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.IntegerSubject;
import com.google.common.truth.OptionalSubject;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;

public final class CreatureSubject extends Subject {
    public static Factory<CreatureSubject, Creature> creatures() {
        return CreatureSubject::new;
    }

    public static CreatureSubject assertThat(Creature creature) {
        return Truth.assertAbout(creatures()).that(creature);
    }

    private final Creature creature;

    private CreatureSubject(FailureMetadata metadata, Creature creature) {
        super(metadata, creature);
        this.creature = creature;
    }

    public StringSubject name() {
        return check("name()").that(this.creature.name().toString());
    }

    public StringSubject faction() {
        return check("faction()").that(this.creature.faction().toString());
    }

    public StringSubject content() {
        return check("content()").that(this.creature.content());
    }

    public IntegerSubject currentHealth() {
        return check("currentHealth()").that(this.creature.currentHealth());
    }

    public IntegerSubject maximumHealth() {
        return check("maximumHealth()").that(this.creature.maximumHealth());
    }

    public ComparableSubject<ResourcePoolSize> healthBucket() {
        return check("healthBucket()").that(this.creature.healthBucket());
    }

    public ItemContainerSubject items() {
        return this.check("items()").about(ItemContainerSubject.itemContainers()).that(creature);
    }

    public OptionalSubject locale() {
        return this.check("locale()").that(this.creature.locale());
    }

}
