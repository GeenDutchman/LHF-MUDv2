package com.geendutchman.lhf_mudv2.entities.room;

import java.util.Optional;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class RoomEffect implements Examinable {
    protected RoomEffect() {
    }

    public abstract ImmutableList<Room.Delta> deltas();

    public abstract Optional<RichOutput> applicationDescription();

    public final String content() {
        return this.name();
    }

    public final String tag() {
        return "ROOM_EFFECT";
    }

    public static Builder builder() {
        return new AutoValue_RoomEffect.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        protected Builder() {
        }

        public abstract Builder setName(String name);

        public abstract Builder setApplicationDescription(Optional<RichOutput> d);

        public final Builder setApplicationDescriptionFromBuilder(RichOutput.Builder builder) {
            return this.setApplicationDescription(Optional.of(builder.build()));
        }

        abstract ImmutableList.Builder<Room.Delta> deltasBuilder();

        public final Builder addDeltas(Room.Delta... deltas) {
            deltasBuilder().add(deltas);
            return this;
        }

        public abstract Builder setDescription(Optional<RichOutput> d);

        public final Builder setDescriptionFromBuilder(RichOutput.Builder builder) {
            return this.setDescription(Optional.of(builder.build()));
        }

        public abstract RoomEffect build();
    }
}
