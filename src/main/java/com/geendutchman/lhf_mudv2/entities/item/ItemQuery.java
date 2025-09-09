package com.geendutchman.lhf_mudv2.entities.item;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import com.geendutchman.lhf_mudv2.entities.IEntityID;
import com.geendutchman.lhf_mudv2.entities.IEntityQuery;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ItemQuery implements IEntityQuery<Item> {

    public abstract Optional<Boolean> isVisible();

    public abstract Optional<String> nickname();

    public abstract Optional<Pattern> nicknamePattern();

    public abstract Optional<String> displayName();

    public abstract Optional<Pattern> displayNamePattern();

    public abstract EntityQuery entityQuery();

    public final static Builder builder() {
        final Builder builder = new AutoValue_ItemQuery.Builder().setIsVisible(true);
        return builder;
    }

    public abstract ItemQuery.Builder toBuilder();

    public final ItemQuery withDisplayName(String name) {
        return this.toBuilder().setDisplayName(name).build();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract EntityQuery.EntityQueryBuilder entityQueryBuilder();

        public abstract Builder setEntityQuery(EntityQuery entityQuery);

        public final Builder setIdentifier(Optional<IEntityID> identifier) {
            this.entityQueryBuilder().setIdentifier(identifier);
            return this;
        }

        public final Builder setIdentifier(ItemID identifier) {
            this.entityQueryBuilder().setIdentifier(identifier);
            return this;
        }

        public final Builder adjustEntityQuery(Consumer<EntityQuery.EntityQueryBuilder> adjustor) {
            if (adjustor != null) {
                adjustor.accept(this.entityQueryBuilder());
            }
            return this;
        }

        public abstract Builder setNickname(String name);

        public abstract Builder setNicknamePattern(Pattern pattern);

        public abstract Builder setDisplayName(String name);

        public abstract Builder setDisplayName(Optional<String> name);

        public abstract Builder setDisplayNamePattern(Pattern pattern);

        public abstract Builder setDisplayNamePattern(Optional<Pattern> pattern);

        public Builder setDisplayNamePattern(String pattern) {
            return this.setDisplayNamePattern(Pattern.compile(pattern));
        }

        public abstract Builder setIsVisible(boolean isVisible);

        public abstract Builder setIsVisible(Optional<Boolean> isVisible);

        public abstract ItemQuery build();

    }

    @Override
    public final boolean test(Item t) {
        if (t == null) {
            return false;
        }
        final EntityQuery entityQ = this.entityQuery();
        if (entityQ != null && !entityQ.test(t)) {
            return false;
        }

        if (this.nickname().isPresent()) {
            if (t.nickname().isEmpty() || !this.nickname().get().equals(t.nickname().get())) {
                return false;
            }
        }

        if (this.displayName().isPresent()) {
            if (!this.displayName().get().equals(t.displayName())) {
                return false;
            }
        }
        // TODO: check visibility
        return true;
    }

}