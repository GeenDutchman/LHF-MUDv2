package com.geendutchman.lhf_mudv2.dice;

import com.google.common.truth.CustomSubjectBuilder;
import com.google.common.truth.FailureMetadata;

public final class DifficultySubjectBuilder extends CustomSubjectBuilder {
    DifficultySubjectBuilder(FailureMetadata metadata) {
        super(metadata);
    }

    public <E extends Enum<E>> DifficultySubject<E> that(Difficulty<E> actual) {
        return new DifficultySubject<>(metadata(), actual);
    }
}
