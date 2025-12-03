package com.geendutchman.lhf_mudv2.entities.entity;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID.EntityID;
import com.github.f4b6a3.tsid.Tsid;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

public interface IEntityQuery<E extends Entity> extends Predicate<Entity>, Serializable {

    /**
     * Transforms a string to a Range
     * 
     * @param <T>             is comparable
     * @param s               string to transform
     * @param typedFromString method to change a string to a T
     * @throws IllegalArgumentException
     * @return computed range
     */
    public static <T extends Comparable<T>> Range<T> rangeFromString(final String s,
            Function<String, T> typedFromString) {
        Preconditions.checkArgument(typedFromString != null, "must provide a way to transform string to type");
        Pattern spliter = Pattern
                .compile("^(?<lowbound>[\\(\\[])(?<lower>.*(?=\\.\\.))\\.\\.(?<upper>[^)\\]]+)(?<upbound>[)\\]])$");
        Matcher matcher = spliter.matcher(s);
        Preconditions.checkArgument(matcher.matches(), "the string '%s' should match '%s'", s, spliter);
        String upper = matcher.group("upper");
        String lower = matcher.group("lower");
        T upType = typedFromString.apply(upper);
        T lowType = typedFromString.apply(lower);
        return Range.<T>range(lowType, matcher.group("lowbound") == "[" ? BoundType.CLOSED : BoundType.OPEN, upType,
                matcher.group("upbound") == "]" ? BoundType.CLOSED : BoundType.OPEN);
    }

    @AutoValue
    public abstract class EntityQuery implements IEntityQuery<Entity> {
        public abstract Optional<IEntityID> identifier();

        public abstract Optional<Examinable.Name> name();

        public abstract Optional<Pattern> namePattern();

        public abstract Optional<Pattern> toStringPattern();

        @AutoValue.Builder
        public static abstract class EntityQueryBuilder {

            public abstract EntityQueryBuilder setIdentifier(Optional<IEntityID> identifier);

            public abstract EntityQueryBuilder setIdentifier(IEntityID identifier);

            public abstract EntityQueryBuilder setName(Optional<Examinable.Name> name);

            public abstract EntityQueryBuilder setName(Examinable.Name name);

            public EntityQueryBuilder setName(String name) {
                Examinable.Name eName = new Examinable.Name(name);
                return this.setName(eName);
            }

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
                        this.setName(Optional.ofNullable(new Examinable.Name(value)));
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
                            this.setIdentifier(new EntityID(new Taggable.Tag(segments.get(0)),
                                    new Examinable.Name(segments.get(1)), Tsid.from(segments.get(2))));
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

        @Override
        public Map<String, String> toKeyValue() {
            Map<String, String> kv = new LinkedHashMap<>();
            if (this.identifier().isPresent()) {
                kv.put("identifier", this.identifier().get().toString());
            }
            if (this.name().isPresent()) {
                kv.put("name", this.name().map(n -> n.toString()).orElse(""));
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
        public boolean test(Entity toTest) {
            return this.typedTest(toTest);
        }

        @Override
        public boolean typedTest(Entity t) {
            if (t == null) {
                return false;
            }
            if (this.identifier().isPresent()) {
                if (this.identifier().get().compareTo(t.identifier()) != 0) {
                    return false;
                }
            }
            if (this.name().isPresent()) {
                if (!this.name().get().equals(t.name())) {
                    return false;
                }
            }
            if (this.namePattern().isPresent()) {
                if (!this.namePattern().get().asPredicate().test(t.name().toString())) {
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

    public default boolean test(Entity toTest) {
        try {
            @SuppressWarnings("unchecked")
            final E asE = (E) toTest;
            return this.typedTest(asE);
        } catch (ClassCastException castException) {
            return false;
        }
    }

    public abstract boolean typedTest(E t);

    public Map<String, String> toKeyValue();

}
