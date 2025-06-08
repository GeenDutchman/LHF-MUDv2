package com.geendutchman.lhf_mudv2.entities.item;

import static com.geendutchman.lhf_mudv2.dice.DifficultySubject.difficulties;

import com.geendutchman.lhf_mudv2.dice.DifficultySubject;
import com.geendutchman.lhf_mudv2.dice.Plain;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.OptionalSubject;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;

public final class ItemSubject extends Subject {
    public static Factory<ItemSubject, Item> items() {
        return ItemSubject::new;
    }

    public static ItemSubject assertThat(Item item) {
        return Truth.assertAbout(items()).that(item);
    }

    private final Item item;

    private ItemSubject(FailureMetadata metadata, Item item) {
        super(metadata, item);
        this.item = item;
    }

    public OptionalSubject nickname() {
        return check("nickname()").that(this.item.nickname());
    }

    public StringSubject displayName() {
        return check("displayName()").that(this.item.displayName());
    }

    public StringSubject name() {
        return check("name()").that(this.item.name());
    }

    public StringSubject itemID() {
        return check("itemID().uuid()").that(this.item.identifier().uuid().toString());
    }

    public DifficultySubject<Plain> visibility() {
        return check("visibility()").about(difficulties()).that(this.item.visibility());
    }
}
