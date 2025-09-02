package com.geendutchman.lhf_mudv2.entities.item;

import java.util.Objects;

import com.google.common.truth.CustomSubjectBuilder;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.IterableSubject;
import com.google.common.truth.OptionalSubject;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Truth;

public class ItemContainerSubject<I extends Item> extends IterableSubject {
    public static CustomSubjectBuilder.Factory<ItemContainerSubjectBuilder> itemContainers() {
        return ItemContainerSubjectBuilder::new;
    }

    public static <I extends Item> ItemContainerSubject<I> assertThat(ItemContainer<I> actual) {
        return Truth.assertAbout(itemContainers()).that(actual);
    }

    private final ItemContainer<I> actual;

    protected ItemContainerSubject(FailureMetadata metadata, ItemContainer<I> actual) {
        super(metadata, actual != null ? actual.items() : null);
        this.actual = actual;
    }

    public StringSubject name() {
        return check("getName()").that(actual.name());
    }

    public IterableSubject items() {
        return check("getItems()").that(actual.items());
    }

    // public void itemIsAdded(Item item) {
    // check("addItem(%s)", item).that(actual.addItem(item)).isTrue();
    // this.items().contains(item);
    // }

    // public void itemIsNotAdded(IItem item) {
    // check("addItem(%s)", item).that(actual.addItem(item)).isFalse();
    // this.items().doesNotContain(item);
    // }

    // public void itemIsRemoved(IItem item) {
    // check("removeItem(%s)", item).that(actual.removeItem(item)).isTrue();
    // this.items().doesNotContain(item);
    // }

    public ItemContainerSubject<I> queryAll(ItemQuery query) {
        return check("queryAll(%s)", query).about(itemContainers()).that(this.actual.queryAll(query));
    }

    public OptionalSubject queryOne(ItemQuery query) {
        return check("queryOne(%s)", query).that(this.actual.queryOne(query));
    }

    public void hasItem(Item item) {
        this.items().contains(item);
    }

    public void doesNotHaveItem(Item item) {
        this.items().doesNotContain(item);
    }

    @Override
    public void isEqualTo(Object expected) {
        @SuppressWarnings("UndefinedEquals") // method contract requires testing iterables for equality
        boolean equal = Objects.equals(actual, expected);
        if (equal) {
            return;
        }

        if (expected instanceof ItemContainer expectedIC) {
            containsExactlyElementsIn(expectedIC.items());
        } else {
            super.isEqualTo(expected);
        }
    }
}
