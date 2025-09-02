package com.geendutchman.lhf_mudv2.entities.room;

import java.util.Optional;
import java.util.regex.Pattern;

import com.geendutchman.lhf_mudv2.entities.IEntityID;
import com.geendutchman.lhf_mudv2.entities.IEntityQuery;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class RoomQuery implements IEntityQuery<Room> {
    public abstract Optional<ItemQuery> hasItemLike();

    public final static Builder builder() {
        final Builder builder = new AutoValue_RoomQuery.Builder().setToStringPatterns(ImmutableList.of());
        builder.namePatternsBuilder();
        return builder;
    }

    abstract RoomQuery.Builder toRoomQueryBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder setIdentifier(Optional<IEntityID> identifier);

        public abstract Builder setIdentifier(RoomID identifier);

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

        public abstract Builder setHasItemLike(Optional<ItemQuery> itemQuery);

        public abstract RoomQuery build();

        public IEntityQuery<Room> buildInterface() {
            return this.build();
        }
    }

    @Override
    public Optional<Boolean> testOtherFactors(Room t) {
        if (this.hasItemLike().isEmpty()) {
            return Optional.empty();
        }
        final ItemQuery filter = this.hasItemLike().get();
        return Optional.of(t.inventory().queryOne(filter).isPresent());
    }

    @Override
    public final boolean test(Room t) {
        return IEntityQuery.super.test(t);
    }
}
