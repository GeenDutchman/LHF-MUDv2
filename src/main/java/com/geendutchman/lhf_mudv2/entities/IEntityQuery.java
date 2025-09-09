package com.geendutchman.lhf_mudv2.entities;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.google.auto.value.AutoValue;

public interface IEntityQuery<E extends Entity> extends Predicate<E>, Serializable {

    @AutoValue
    public abstract class EntityQuery implements IEntityQuery<Entity> {
        public abstract Optional<IEntityID> identifier();

        public abstract Optional<String> name();

        public abstract Optional<Pattern> namePattern();

        public abstract Optional<Pattern> toStringPattern();

        @AutoValue.Builder
        public static abstract class EntityQueryBuilder {
            public abstract EntityQueryBuilder setIdentifier(Optional<IEntityID> identifier);

            public abstract EntityQueryBuilder setIdentifier(IEntityID identifier);

            public abstract EntityQueryBuilder setName(Optional<String> name);

            public abstract EntityQueryBuilder setName(String name);

            public abstract EntityQueryBuilder setNamePattern(Optional<Pattern> pattern);

            public abstract EntityQueryBuilder setNamePattern(Pattern pattern);

            public EntityQueryBuilder setNamePattern(String pattern) {
                return this.setNamePattern(Pattern.compile(pattern));
            }

            public abstract EntityQueryBuilder setToStringPattern(Optional<Pattern> patterns);

            public abstract EntityQuery build();

        }

        public abstract EntityQueryBuilder toBuilder();

        public final static EntityQuery.EntityQueryBuilder builder() {
            final EntityQueryBuilder builder = new AutoValue_IEntityQuery_EntityQuery.Builder();
            return builder;
        }

        @Override
        public boolean test(Entity t) {
            if (t == null) {
                return false;
            }
            if (this.identifier().isPresent()) {
                if (this.identifier().get().compareTo(t.identifier()) == 0) {
                    return true;
                }
            }
            if (this.name().isPresent()) {
                if (!this.name().get().equals(t.name())) {
                    return false;
                }
            }
            if (this.namePattern().isPresent()) {
                if (!this.namePattern().get().asPredicate().test(t.name())) {
                    return false;
                }
            }

            if (this.toStringPattern().isPresent()) {
                if (!this.toStringPattern().get().asPredicate().test(t.toString())) {
                    return false;
                }
            }
            return true;
        }
    }

    public static EntityQuery.EntityQueryBuilder entityQueryBuilder() {
        final EntityQuery.EntityQueryBuilder builder = new AutoValue_IEntityQuery_EntityQuery.Builder();
        return builder;
    }

    @Override
    public default boolean test(E t) {
        return t != null;
    }

}
