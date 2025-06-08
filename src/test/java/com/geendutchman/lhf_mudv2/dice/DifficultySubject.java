package com.geendutchman.lhf_mudv2.dice;

import java.util.Optional;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.google.common.truth.BooleanSubject;
import com.google.common.truth.CustomSubjectBuilder;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.IntegerSubject;
import com.google.common.truth.MapSubject;
import com.google.common.truth.StringSubject;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;

public final class DifficultySubject<E extends Enum<E>> extends Subject {
    public static CustomSubjectBuilder.Factory<DifficultySubjectBuilder> difficulties() {
        return DifficultySubjectBuilder::new;
    }

    private final Difficulty<E> actual;

    DifficultySubject(FailureMetadata metadata, Difficulty<E> actual) {
        super(metadata, actual);
        this.actual = actual;
    }

    public static <E extends Enum<E>> DifficultySubject<E> assertThat(Difficulty<E> difficulty) {
        return Truth.assertAbout(difficulties()).that(difficulty);
    }

    public IntegerSubject sum() {
        return check("sum()").that(actual.sum());
    }

    public MapSubject dcs() {
        return check("dcs()").that(actual.dcs());
    }

    public BooleanSubject totalOnly() {
        return check("totalOnly()").that(actual.totalOnly());
    }

    public BooleanSubject testsResult(RollSet<E> roll) {
        return check("test(%s)", roll).that(actual.test(roll));
    }

    public StringSubject testsOutput(RollSet<E> roll) {
        RichOutput.Builder builder = RichOutput.builder();
        actual.test(roll, Optional.of(builder));
        return check("test(%s,RichOutput.Builder)", roll).that(builder.build().printIt());
    }
}
