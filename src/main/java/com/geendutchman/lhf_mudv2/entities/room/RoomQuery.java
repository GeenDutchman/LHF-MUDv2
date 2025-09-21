package com.geendutchman.lhf_mudv2.entities.room;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;

import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class RoomQuery implements IEntityQuery<Room> {
    public abstract Optional<ItemQuery> hasItemLike();

    public abstract EntityQuery entityQuery();

    public final static Builder builder() {
        final Builder builder = new AutoValue_RoomQuery.Builder();
        return builder;
    }

    public abstract RoomQuery.Builder toRoomQueryBuilder();

    @Override
    public Map<String, String> toKeyValue() {
        Map<String, String> kv = new LinkedHashMap<>();
        EntityQuery eq = this.entityQuery();
        if (eq != null) {
            kv.putAll(eq.toKeyValue());
        }
        Optional<ItemQuery> oiq = this.hasItemLike();
        if (oiq.isPresent()) {
            oiq.get().toKeyValue().forEach((key, value) -> {
                kv.put("itemquery." + key, value);
            });
        }
        return kv;
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract EntityQuery.EntityQueryBuilder entityQueryBuilder();

        public abstract Builder setEntityQuery(EntityQuery entityQuery);

        public Builder setIdentifier(Optional<IEntityID> identifier) {
            this.entityQueryBuilder().setIdentifier(identifier);
            return this;
        }

        public Builder setIdentifier(ItemID identifier) {
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

        public Builder fromKeyValue(Map<String, String> kv) {
            if (kv == null) {
                return this;
            }
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
                switch (key) {

                default:
                    break;
                }
            }
            return this;
        }

        public abstract RoomQuery build();

        public IEntityQuery<Room> buildInterface() {
            return this.build();
        }
    }

    @Override
    public final boolean test(Room t) {
        if (t == null) {
            return false;
        }
        final EntityQuery entityQ = this.entityQuery();
        if (entityQ != null && !entityQ.test(t)) {
            return false;
        }
        if (this.hasItemLike().isPresent()) {
            if (t.queryOneItem(this.hasItemLike().get()).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
