package com.geendutchman.lhf_mudv2.entities.creatures;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;

import com.geendutchman.lhf_mudv2.dice.Difficulty;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Range;

@AutoValue
public abstract class CreatureQuery implements IEntityQuery<Creature> {
    public abstract Optional<ItemQuery> hasItemLike();

    public abstract EntityQuery entityQuery();

    public abstract Optional<Faction> faction();

    public abstract Optional<Range<Integer>> currentHealth();

    public abstract Optional<Range<Integer>> maxHealth();

    public abstract ImmutableSortedSet<ResourcePoolSize> healthBuckets();

    public abstract ImmutableSortedMap<AttributeScores, Range<Byte>> scores();

    public abstract ImmutableSortedMap<AttributeScores, Range<Byte>> modifiers();

    public abstract Optional<Difficulty<AttributeScores>> attributeDifficulty();

    @Override
    public Map<String, String> toKeyValue() {
        Map<String, String> kv = new LinkedHashMap<>();
        EntityQuery eq = this.entityQuery();
        if (eq != null) {
            kv.putAll(eq.toKeyValue());
        }
        this.hasItemLike().ifPresent(query -> {
            query.toKeyValue().forEach((k, v) -> {
                kv.put("itemquery." + k, v);
            });
        });
        this.faction().ifPresent(fact -> kv.put("faction", fact.name()));
        this.currentHealth().ifPresent(range -> kv.put("currenthealth", range.toString()));
        this.maxHealth().ifPresent(range -> kv.put("maxhealth", range.toString()));
        this.healthBuckets().stream();
        StringJoiner bucketJoiner = new StringJoiner("&").setEmptyValue("");
        for (ResourcePoolSize poolSize : this.healthBuckets()) {
            bucketJoiner.add(poolSize.name());
        }
        if (bucketJoiner.length() > 0) {
            kv.put("healthbuckets", bucketJoiner.toString());
        }
        this.scores().forEach((score, range) -> {
            kv.put("scores." + score.name(), range.toString());
        });
        this.modifiers().forEach((mod, range) -> {
            kv.put("mods." + mod.name(), range.toString());
        });
        this.attributeDifficulty().ifPresent(difficulty -> {
            difficulty.dcs().forEach((attr, val) -> {
                kv.put("difficulty." + attr.name(), val.toString());
            });
            kv.put("difficulty-totalonly", Boolean.toString(difficulty.totalOnly()));
        });
        return kv;
    }

    public final static Builder builder() {
        final Builder builder = new AutoValue_CreatureQuery.Builder();
        return builder;
    }

    public abstract CreatureQuery.Builder toCreatureQueryBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract EntityQuery.EntityQueryBuilder entityQueryBuilder();

        public abstract Builder setEntityQuery(EntityQuery entityQuery);

        public Builder setIdentifier(Optional<IEntityID> identifier) {
            this.entityQueryBuilder().setIdentifier(identifier);
            return this;
        }

        public Builder setIdentifier(CreatureID identifier) {
            this.entityQueryBuilder().setIdentifier(identifier);
            return this;
        }

        public Builder setName(String name) {
            this.entityQueryBuilder().setName(name);
            return this;
        }

        public final Builder adjustEntityQuery(Consumer<EntityQuery.EntityQueryBuilder> adjustor) {
            if (adjustor != null) {
                adjustor.accept(this.entityQueryBuilder());
            }
            return this;
        }

        public abstract Builder setHasItemLike(Optional<ItemQuery> itemQuery);

        public abstract Builder setHasItemLike(ItemQuery itemQuery);

        public Builder setHasItemLike(ItemQuery.Builder iqBuilder) {
            if (iqBuilder != null) {
                return this.setHasItemLike(iqBuilder.build());
            }
            return this;
        }

        public abstract Optional<ItemQuery> hasItemLike();

        public Builder adjustHasItemLike(Consumer<ItemQuery.Builder> adjustor) {
            if (adjustor == null) {
                return this;
            }
            ItemQuery.Builder asBuilder = this.hasItemLike().map(query -> query.toBuilder())
                    .orElse(ItemQuery.builder());
            adjustor.accept(asBuilder);
            return this.setHasItemLike(asBuilder.build());
        }

        public abstract Builder setFaction(Faction faction);

        public abstract Builder setCurrentHealth(Range<Integer> range);

        public abstract Builder setMaxHealth(Range<Integer> range);

        abstract ImmutableSortedSet.Builder<ResourcePoolSize> healthBucketsBuilder();

        public abstract Builder setHealthBuckets(Set<ResourcePoolSize> bucket);

        abstract ImmutableSortedMap.Builder<AttributeScores, Range<Byte>> scoresBuilder();

        public Builder adjustScores(Consumer<ImmutableSortedMap.Builder<AttributeScores, Range<Byte>>> adjustor) {
            if (adjustor != null) {
                adjustor.accept(this.scoresBuilder());
            }
            return this;
        }

        abstract ImmutableSortedMap.Builder<AttributeScores, Range<Byte>> modifiersBuilder();

        public Builder adjustModifiers(Consumer<ImmutableSortedMap.Builder<AttributeScores, Range<Byte>>> adjustor) {
            if (adjustor != null) {
                adjustor.accept(this.modifiersBuilder());
            }
            return this;
        }

        public abstract Builder setAttributeDifficulty(Difficulty<AttributeScores> dcs);

        public Builder fromKeyValue(final Map<String, String> kv) {
            if (kv == null) {
                return this;
            }
            ImmutableSortedMap.Builder<AttributeScores, Integer> mapBuilder = null;
            boolean totalOnly = false;
            this.entityQueryBuilder().fromKeyValue(kv);
            for (final Entry<String, String> q : kv.entrySet()) {
                final String key = q.getKey().toLowerCase();
                final String value = q.getValue();
                if (value == null) {
                    continue;
                }
                if (key.startsWith("itemquery.")) {
                    this.adjustHasItemLike(b -> {
                        if (b == null) {
                            return;
                        }
                        b.fromKeyValue(Map.of(key.replaceFirst("itemquery.", ""), value));
                    });
                    continue;
                }
                if (key.startsWith("scores.")) {
                    this.adjustScores(b -> {
                        b.put(AttributeScores.valueOf(key.replaceFirst("scores.", "").toUpperCase()),
                                IEntityQuery.rangeFromString(value, Byte::valueOf));
                    });
                    continue;
                }
                if (key.startsWith("mods.")) {
                    this.adjustModifiers(b -> {
                        b.put(AttributeScores.valueOf(key.replaceFirst("mods.", "").toUpperCase()),
                                IEntityQuery.rangeFromString(value, Byte::valueOf));
                    });
                    continue;
                }
                if (key.startsWith("difficulty.")) {
                    if (key == "difficulty-totalonly") {
                        totalOnly = Boolean.valueOf(value);
                        continue;
                    }
                    if (mapBuilder == null) {
                        mapBuilder = ImmutableSortedMap.naturalOrder();
                    }
                    mapBuilder.put(AttributeScores.valueOf(key.replaceFirst("difficulty.", "").toUpperCase()),
                            Integer.valueOf(value));
                    continue;
                }
                switch (key) {
                case "difficulty-totalonly":
                    totalOnly = Boolean.valueOf(value);
                    break;
                case "currenthealth":
                    this.setCurrentHealth(IEntityQuery.rangeFromString(value, Integer::valueOf));
                    break;
                case "maxhealth":
                    this.setMaxHealth(IEntityQuery.rangeFromString(value, Integer::valueOf));
                    break;
                case "healthbucket":
                    for (String split : value.split("&")) {
                        healthBucketsBuilder().add(ResourcePoolSize.valueOf(split));
                    }
                    break;
                case "faction":
                    this.setFaction(Faction.valueOf(value));
                    break;
                default:
                    break;
                }
            }
            if (mapBuilder != null) {
                this.setAttributeDifficulty(new Difficulty<>(mapBuilder.build(), totalOnly));
            }
            return this;
        }

        public abstract CreatureQuery build();

        public IEntityQuery<Creature> buildInterface() {
            return this.build();
        }

    }

    @Override
    public final boolean test(final Creature c) {
        if (c == null) {
            return false;
        }
        final EntityQuery entityQ = this.entityQuery();
        if (entityQ != null && !entityQ.test(c)) {
            return false;
        }
        if (this.hasItemLike().isPresent()) {
            if (c.queryOneItem(this.hasItemLike().get()).isEmpty()) {
                return false;
            }
        }
        final Optional<Faction> faction = this.faction();
        if (faction != null && faction.isPresent() && faction.get() != c.faction()) {
            return false;
        }
        final Optional<Range<Integer>> currentHealth = this.currentHealth();
        if (currentHealth != null && currentHealth.isPresent() && !currentHealth.get().contains(c.currentHealth())) {
            return false;
        }
        final Optional<Range<Integer>> maxHealth = this.maxHealth();
        if (maxHealth != null && maxHealth.isPresent() && !maxHealth.get().contains(c.maximumHealth())) {
            return false;
        }
        final ImmutableSortedSet<ResourcePoolSize> buckets = this.healthBuckets();
        if (buckets != null && buckets.size() > 0 && !buckets.contains(c.healthBucket())) {
            return false;
        }

        final ImmutableSortedMap<AttributeScores, Range<Byte>> scores = this.scores();
        if (scores != null && scores.size() > 0) {
            for (Entry<AttributeScores, Range<Byte>> scoreentry : scores.entrySet()) {
                if (!scoreentry.getValue().contains(c.getScore(scoreentry.getKey()))) {
                    return false;
                }
            }
        }

        final ImmutableSortedMap<AttributeScores, Range<Byte>> mods = this.modifiers();
        if (mods != null && mods.size() > 0) {
            for (Entry<AttributeScores, Range<Byte>> modentry : mods.entrySet()) {
                if (!modentry.getValue().contains(c.getModifier(modentry.getKey()))) {
                    return false;
                }
            }
        }

        final Optional<Difficulty<AttributeScores>> diff = this.attributeDifficulty();
        if (diff != null && diff.isPresent() && !diff.get().test(c.attributeCheck())) {
            return false;
        }

        return true;
    }
}
