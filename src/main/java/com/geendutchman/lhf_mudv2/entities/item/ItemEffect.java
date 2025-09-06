package com.geendutchman.lhf_mudv2.entities.item;

import java.io.Serializable;
import java.util.Optional;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class ItemEffect implements Examinable, Serializable {
    protected ItemEffect() {
    }

    public abstract ImmutableList<Item.Delta> deltas();

    public abstract Optional<RichOutput> applicationDescription();

    public final String content() {
        return this.name();
    }

    public final String tag() {
        return "ITEM_EFFECT";
    }

    public static Builder builder() {
        return new AutoValue_ItemEffect.Builder();
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

        abstract ImmutableList.Builder<Item.Delta> deltasBuilder();

        public final Builder addDeltas(Item.Delta... deltas) {
            deltasBuilder().add(deltas);
            return this;
        }

        public abstract Builder setDescription(Optional<RichOutput> d);

        public final Builder setDescriptionFromBuilder(RichOutput.Builder builder) {
            return this.setDescription(Optional.of(builder.build()));
        }

        public abstract ItemEffect build();

    }
}