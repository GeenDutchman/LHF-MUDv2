package com.geendutchman.lhf_mudv2.entities.item;

import java.util.Optional;
import java.util.regex.Pattern;

import com.geendutchman.lhf_mudv2.entities.IEntityID;
import com.geendutchman.lhf_mudv2.entities.IEntityQuery;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class ItemQuery implements IEntityQuery<Item> {

    public abstract Optional<Boolean> isVisible();

    public abstract boolean checkOnlyName();

    public final static Builder builder() {
        final Builder builder = new AutoValue_ItemQuery.Builder().setIsVisible(true).setCheckOnlyName(false)
                .setToStringPatterns(ImmutableList.of());
        builder.namePatternsBuilder();
        return builder;
    }

    abstract ItemQuery.Builder toItemQueryBuilder();

    public final ItemQuery withNamePattern(String pattern) {
        return toItemQueryBuilder().addNamePattern(pattern).build();
    }

    public final ItemQuery withCheckOnlyName(boolean check) {
        return toItemQueryBuilder().setCheckOnlyName(check).build();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder setIdentifier(Optional<IEntityID> identifier);

        public abstract Builder setIdentifier(ItemID identifier);

        public abstract Builder setName(Optional<String> name);

        public abstract Builder setName(String name);

        protected abstract ImmutableList.Builder<Pattern> namePatternsBuilder();

        public final Builder addNamePattern(Pattern pattern) {
            this.namePatternsBuilder().add(pattern);
            return this;
        }

        public final Builder addNamePattern(String pattern) {
            this.namePatternsBuilder().add(Pattern.compile(pattern));
            return this;
        }

        public abstract Builder setToStringPatterns(ImmutableList<Pattern> patterns);

        public abstract Builder setIsVisible(boolean isVisible);

        public abstract Builder setIsVisible(Optional<Boolean> isVisible);

        public abstract Builder setCheckOnlyName(boolean check);

        public abstract ItemQuery build();

        public IEntityQuery<Item> buildInterface() {
            return this.build();
        }

    }

    @Override
    public final boolean testNames(Item t) {
        if (!this.checkOnlyName()) {
            if (this.name().isPresent()) {
                if (!this.name().get().equals(t.displayName())) {
                    return false;
                }
            }
            for (final Pattern pattern : this.namePatterns()) {
                if (!pattern.asPredicate().test(t.displayName())) {
                    return false;
                }
            }
        } else {
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
        }

        return true;
    }

    @Override
    public final boolean test(Item t) {
        return IEntityQuery.super.test(t);
    }

}