package com.geendutchman.lhf_mudv2.entities;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.geendutchman.lhf_mudv2.entities.IEntityID.EntityID;
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

            public EntityQueryBuilder fromKeyValue(Map<String, String> kv) {
                if (kv == null) {
                    return this;
                }
                for (final Entry<String, String> q : kv.entrySet()) {
                    final String value = q.getValue();
                    if (value == null) {
                        continue;
                    }
                    switch (q.getKey().toLowerCase()) {
                    case "name":
                        this.setName(Optional.ofNullable(value));
                        break;
                    case "namepatttern":
                        this.setNamePattern(value);
                        break;
                    case "tostringpattern":
                        this.setToStringPattern(Optional.ofNullable(Pattern.compile(value)));
                        break;
                    case "identifier":
                        UriComponents idComponents = UriComponentsBuilder.fromPath(value).build();
                        List<String> segments = idComponents.getPathSegments();
                        if (segments.size() != 3) {
                            continue;
                        }
                        try {
                            this.setIdentifier(
                                    new EntityID(segments.get(0), segments.get(1), UUID.fromString(segments.get(2))));
                        } catch (IllegalArgumentException | NullPointerException e) {
                            continue;
                        }
                        break;
                    default:
                        break;
                    }
                }
                return this;
            }

            public abstract EntityQuery build();

        }

        public abstract EntityQueryBuilder toBuilder();

        public final static EntityQuery.EntityQueryBuilder builder() {
            final EntityQueryBuilder builder = new AutoValue_IEntityQuery_EntityQuery.Builder();
            return builder;
        }

        public Map<String, String> toKeyValue() {
            Map<String, String> kv = new LinkedHashMap<>();
            if (this.identifier().isPresent()) {
                kv.put("identifier", this.identifier().get().toString());
            }
            if (this.name().isPresent()) {
                kv.put("name", this.name().orElse(""));
            }
            if (this.namePattern().isPresent()) {
                kv.put("namePattern", this.namePattern().get().toString());
            }
            if (this.toStringPattern().isPresent()) {
                kv.put("toStringPattern", this.toStringPattern().get().toString());
            }
            return kv;
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

    public Map<String, String> toKeyValue();

}
