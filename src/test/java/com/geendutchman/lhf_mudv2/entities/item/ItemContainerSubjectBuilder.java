package com.geendutchman.lhf_mudv2.entities.item;

import com.google.common.truth.CustomSubjectBuilder;
import com.google.common.truth.FailureMetadata;

public final class ItemContainerSubjectBuilder extends CustomSubjectBuilder {
    ItemContainerSubjectBuilder(FailureMetadata metadata) {
        super(metadata);
    }

    public <I extends Item> ItemContainerSubject<I> that(ItemContainer<I> actual) {
        return new ItemContainerSubject<>(metadata(), actual);
    }
}
