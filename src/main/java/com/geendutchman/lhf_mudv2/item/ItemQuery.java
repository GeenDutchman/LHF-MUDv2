package com.geendutchman.lhf_mudv2.item;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.geendutchman.lhf_mudv2.item.Item.ItemID;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class ItemQuery implements Predicate<Item>, Serializable {
    public abstract Optional<ItemID> itemID();

    public abstract Optional<String> name();

    public abstract ImmutableList<Pattern> namePatterns();

    public abstract Optional<Boolean> isVisible();

    public abstract boolean checkOnlyName();

    public abstract ImmutableList<Pattern> toStringPatterns();

    public final static Builder builder() {
        final Builder builder = new AutoValue_ItemQuery.Builder().setIsVisible(true).setCheckOnlyName(false)
                .setToStringPatterns(ImmutableList.of());
        builder.namePatternsBuilder();
        return builder;
    }

    abstract Builder toBuilder();

    public final ItemQuery withNamePattern(String pattern) {
        return toBuilder().addNamePattern(pattern).build();
    }

    public final ItemQuery withCheckOnlyName(boolean check) {
        return toBuilder().setCheckOnlyName(check).build();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder setItemID(Optional<ItemID> itemID);

        public abstract Builder setName(Optional<String> name);

        public abstract Builder setName(String name);

        abstract ImmutableList.Builder<Pattern> namePatternsBuilder();

        public final Builder addNamePattern(Pattern pattern) {
            this.namePatternsBuilder().add(pattern);
            return this;
        }

        public final Builder addNamePattern(String pattern) {
            this.namePatternsBuilder().add(Pattern.compile(pattern));
            return this;
        }

        public abstract Builder setIsVisible(boolean isVisible);

        public abstract Builder setIsVisible(Optional<Boolean> isVisible);

        public abstract Builder setCheckOnlyName(boolean check);

        public abstract Builder setToStringPatterns(ImmutableList<Pattern> patterns);

        public abstract ItemQuery build();

    }

    private final boolean testNames(Item t) {
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
        if (t == null) {
            return false;
        }
        if (this.itemID().isPresent()) {
            if (this.itemID().get().equals(t.itemID())) {
                return true;
            }
        }
        if (this.isVisible().isPresent()) {
            if (t.isVisible() != this.isVisible().get()) {
                return false;
            }
        }
        if (!this.testNames(t)) {
            return false;
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