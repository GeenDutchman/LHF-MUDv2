package com.geendutchman.lhf_mudv2.entities.item;

import java.util.Objects;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.IterableSubject;
import com.google.common.truth.OptionalSubject;
import com.google.common.truth.StreamSubject;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Truth;

public class ItemContainerSubject extends IterableSubject {
    public static Factory<ItemContainerSubject, ItemContainer> itemContainers() {
        return ItemContainerSubject::new;
    }

    public static ItemContainerSubject assertThat(ItemContainer actual) {
        return Truth.assertAbout(itemContainers()).that(actual);
    }

    private final ItemContainer actual;

    protected ItemContainerSubject(FailureMetadata metadata, ItemContainer actual) {
        super(metadata, actual != null ? actual.items().toList() : null);
        this.actual = actual;
    }

    public StringSubject name() {
        return check("getName()").that(actual.name().toString());
    }

    public IterableSubject items() {
        return check("getItems()").that(actual.items().toList());
    }

    public StreamSubject itemsStream() {
        return check("items()").that(actual.items());
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

    public ItemContainerSubject queryAll(ItemQuery query) {
        return check("queryAll(%s)", query).about(itemContainers()).that(this.actual.queryItems(query));
    }

    public OptionalSubject queryOne(ItemQuery query) {
        return check("queryOne(%s)", query).that(this.actual.queryOneItem(query));
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
            containsExactlyElementsIn(expectedIC.items().toList());
        } else {
            super.isEqualTo(expected);
        }
    }
}
