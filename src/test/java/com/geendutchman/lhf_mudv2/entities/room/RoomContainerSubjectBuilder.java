package com.geendutchman.lhf_mudv2.entities.room;

import com.google.common.truth.CustomSubjectBuilder;
import com.google.common.truth.FailureMetadata;

public final class RoomContainerSubjectBuilder extends CustomSubjectBuilder {
    RoomContainerSubjectBuilder(FailureMetadata metadata) {
        super(metadata);
    }

    public <R extends Room> RoomContainerSubject<R> that(RoomContainer<R> actual) {
        return new RoomContainerSubject<>(metadata(), actual);
    }
}
