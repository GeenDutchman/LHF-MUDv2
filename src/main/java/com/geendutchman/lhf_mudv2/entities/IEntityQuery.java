package com.geendutchman.lhf_mudv2.entities;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

public interface IEntityQuery<E extends Entity> extends Predicate<E>, Serializable {

    public abstract Optional<IEntityID> identifier();

    public abstract Optional<String> name();

    public abstract ImmutableList<Pattern> namePatterns();

    public abstract ImmutableList<Pattern> toStringPatterns();

    public interface Builder<E extends Entity> {
        public abstract Builder<E> setIdentifier(Optional<IEntityID> identifier);

        public abstract Builder<E> setName(Optional<String> name);

        public abstract Builder<E> setName(String name);

        abstract ImmutableList.Builder<Pattern> namePatternsBuilder();

        public default Builder<E> addNamePattern(Pattern pattern) {
            this.namePatternsBuilder().add(pattern);
            return this;
        }

        public default Builder<E> addNamePattern(String pattern) {
            this.namePatternsBuilder().add(Pattern.compile(pattern));
            return this;
        }

        public abstract Builder<E> setToStringPatterns(ImmutableList<Pattern> patterns);

        public abstract EntityQuery<E> build();

        public default IEntityQuery<E> buildInterface() {
            return this.build();
        }

    }

    @AutoValue
    public abstract class EntityQuery<E extends Entity> implements IEntityQuery<E> {

        @AutoValue.Builder
        public static abstract class EntityQueryBuilder<E extends Entity> implements IEntityQuery.Builder<E> {

        }

        abstract EntityQueryBuilder<E> toBuilder();

        public final static <E extends Entity> EntityQuery.EntityQueryBuilder<E> builder() {
            final EntityQueryBuilder<E> builder = new AutoValue_IEntityQuery_EntityQuery.Builder<>();
            builder.namePatternsBuilder();
            return builder;
        }
    }

    default boolean testNames(E t) {
        if (this.name().isPresent()) {
            if (!this.name().get().equals(t.name())) {
                return false;
            }
        }
        for (final Pattern pattern : this.namePatterns()) {
            if (!pattern.asPredicate().test(t.name())) {
                return false;
            }
        }
        return true;
    }

    default Optional<Boolean> testOtherFactors(E t) {
        return Optional.empty();
    }

    @Override
    public default boolean test(E t) {
        if (t == null) {
            return false;
        }
        if (this.identifier().isPresent()) {
            if (this.identifier().get().compareTo(t.identifier()) == 0) {
                return true;
            }
        }
        if (!this.testNames(t)) {
            return false;
        }
        final Optional<Boolean> others = this.testOtherFactors(t);
        if (others != null && others.isPresent()) {
            return others.get();
        }
        final ImmutableList<Pattern> toStringPatterns = this.toStringPatterns();
        if (toStringPatterns.size() > 0) {
            final String asStr = t.toString();
            for (final Pattern pattern : this.toStringPatterns()) {
                if (!pattern.asMatchPredicate().test(asStr)) {
                    return false;
                }
            }
        }
        return true;
    }
}
