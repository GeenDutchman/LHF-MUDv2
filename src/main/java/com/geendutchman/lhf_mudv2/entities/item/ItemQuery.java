package com.geendutchman.lhf_mudv2.entities.item;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery;
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

    @Override
    public Map<String, String> toKeyValue() {
        Map<String, String> kv = new LinkedHashMap<>();
        EntityQuery eq = this.entityQuery();
        if (eq != null) {
            kv.putAll(eq.toKeyValue());
        }
        if (this.isVisible().isPresent()) {
            kv.put("isvisible", this.isVisible().orElse(true).toString());
        }
        if (this.nickname().isPresent()) {
            kv.put("nickname", this.nickname().orElse(""));
        }
        if (this.nicknamePattern().isPresent()) {
            kv.put("nicknamepattern", this.nicknamePattern().get().toString());
        }
        if (this.displayName().isPresent()) {
            kv.put("displayname", this.displayName().get());
        }
        if (this.displayNamePattern().isPresent()) {
            kv.put("displaynamepattern", this.displayNamePattern().get().toString());
        }
        return kv;
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

        public Builder fromKeyValue(Map<String, String> kv) {
            if (kv == null) {
                return this;
            }
            this.entityQueryBuilder().fromKeyValue(kv);
            for (final Entry<String, String> q : kv.entrySet()) {
                final String value = q.getValue();
                if (value == null) {
                    continue;
                }
                switch (q.getKey().toLowerCase()) {
                case "nickname":
                    this.setNickname(value);
                    break;
                case "nicknamepattern":
                    this.setNicknamePattern(Pattern.compile(value));
                    break;
                case "isvisible":
                    this.setIsVisible(Boolean.parseBoolean(value));
                    break;
                case "displayname":
                    this.setDisplayName(value);
                    break;
                case "displaynamepattern":
                    this.setDisplayNamePattern(value);
                    break;
                default:
                    break;
                }
            }
            return this;
        }

        public abstract ItemQuery build();

    }

    @Override
    public boolean test(Entity toTest) {
        if (toTest instanceof Item asItem) {
            return this.typedTest(asItem);
        }
        return false;
    }

    @Override
    public final boolean typedTest(Item t) {
        if (t == null) {
            return false;
        }
        final EntityQuery entityQ = this.entityQuery();
        if (entityQ != null && !entityQ.test(t)) {
            return false;
        }

        if (this.nickname().isPresent()) {
            if (t.nickname().isEmpty() || !this.nickname().get().equals(t.nickname().get().toString())) {
                return false;
            }
        }

        if (this.nicknamePattern().isPresent()) {
            if (t.nickname().isEmpty()
                    || !this.nicknamePattern().get().asPredicate().test(t.nickname().get().toString())) {
                return false;
            }
        }

        if (this.displayName().isPresent()) {
            if (!this.displayName().get().equals(t.displayName().toString())) {
                return false;
            }
        }

        if (this.displayNamePattern().isPresent()) {
            if (!this.displayNamePattern().get().asPredicate().test(t.displayName().toString())) {
                return false;
            }
        }

        // TODO: check visibility
        return true;
    }

}