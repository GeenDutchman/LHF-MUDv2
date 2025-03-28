package com.geendutchman.lhf_mudv2.dice;

import java.util.Objects;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

import com.geendutchman.lhf_mudv2.display.Taggable;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

public record Roll<E extends Enum<E>>(Dice<E> dice, int roll, Optional<String> note, Optional<Roll<E>> origin)
        implements Comparable<Roll<?>>, Taggable {
    public Roll {
        Objects.requireNonNull(dice, "dice must not be null");
        Objects.requireNonNull(note, "optional note must be empty, not null");
        Objects.requireNonNull(origin, "optional origin must be empty, not null");
    }

    public Roll(Dice<E> dice, int roll) {
        this(dice, roll, Optional.empty(), Optional.empty());
    }

    public int getOriginRoll() {
        if (this.origin.isPresent()) {
            return this.origin.get().getOriginRoll();
        }
        return this.roll;
    }

    protected Roll<E> annotate(final IntUnaryOperator operation, String note) {
        Objects.requireNonNull(operation, "operation must not be null");
        Objects.requireNonNull(note, "annotation note must not be null");
        Preconditions.checkArgument(!note.isBlank(), "annotation must not be blank");
        return new Roll<>(dice, operation.applyAsInt(roll), Optional.of(note), Optional.of(this));
    }

    public Roll<E> negative() {
        if (this.roll <= 0) {
            return this;
        }
        return this.annotate(rolled -> rolled * -1, "negative");
    }

    public Roll<E> positive() {
        if (this.roll >= 0) {
            return this;
        }
        return this.annotate(rolled -> rolled * -1, "positive");
    }

    public Roll<E> doubled() {
        if (this.roll == 0) {
            return this;
        }
        return this.annotate(rolled -> rolled * 2, "doubled");
    }

    public Roll<E> half() {
        if (this.roll == 0) {
            return this;
        }
        return this.annotate(rolled -> rolled / 2, "halved");
    }

    public Roll<E> none() {
        if (this.roll == 0) {
            return this;
        }
        return this.annotate(rolled -> 0, "negated");
    }

    public static Optional<Roll<?>> advantage(Roll<?>... rolls) {
        Optional<Roll<?>> toReturn = Optional.empty();
        for (final Roll<?> roll : rolls) {
            if (roll == null) {
                continue;
            }
            if (toReturn.isEmpty() || toReturn.get().roll() < roll.roll()) {
                toReturn = Optional.of(roll);
            }
        }
        return toReturn;
    }

    public static Optional<Roll<?>> disadvantage(Roll<?>... rolls) {
        Optional<Roll<?>> toReturn = Optional.empty();
        for (final Roll<?> roll : rolls) {
            if (roll == null) {
                continue;
            }
            if (toReturn.isEmpty() || toReturn.get().roll() > roll.roll()) {
                toReturn = Optional.of(roll);
            }
        }
        return toReturn;
    }

    @Override
    public int compareTo(Roll<?> o) {
        if (o == null) {
            throw new NullPointerException("cannot compare to a null roll");
        }
        return this.roll - o.roll;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.origin.isPresent()) {
            sb.append(this.origin.get().toString());
            sb.append(" -->");
        } else {
            sb.append(this.dice.toString());
        }
        if (this.note.isPresent()) {
            sb.append(" ").append(this.note.get()).append(" ");
        }
        sb.append("(").append(this.roll).append(")");
        return sb.toString();
    }

    @Override
    public String tag() {
        return "ROLL_RESULT";
    }

    @Override
    public String content() {
        return this.toString();
    }

    @Override
    public ImmutableSortedMap<String, String> attributes() {
        return this.dice.attributes();
    }
}