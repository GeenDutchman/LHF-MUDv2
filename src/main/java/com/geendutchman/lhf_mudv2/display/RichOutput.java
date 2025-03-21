package com.geendutchman.lhf_mudv2.display;

import java.io.Serializable;
import java.util.Optional;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class RichOutput implements Serializable {
    public abstract Optional<String> sequenceName();

    public abstract ImmutableList<RichOutputElement> elements();

    public static Builder builder() {
        return new AutoValue_RichOutput.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder implements Serializable {
        public abstract Builder setSequenceName(Optional<String> sequenceName);

        public abstract Builder setSequenceName(String sequenceName);

        public abstract Builder setElements(Iterable<RichOutputElement> elements);

        public abstract Builder setElements(RichOutputElement... elements);

        abstract ImmutableList.Builder<RichOutputElement> elementsBuilder();

        public final Builder addElement(RichOutputElement element) {
            elementsBuilder().add(element);
            return this;
        }

        public final Builder addString(String charsequence) {
            elementsBuilder().add(RichOutputElement.ofString(charsequence));
            return this;
        }

        public final Builder addTaggable(Taggable tagged) {
            elementsBuilder().add(RichOutputElement.ofTaggable(tagged));
            return this;
        }

        public final Builder addSignal(String signal) {
            elementsBuilder().add(RichOutputElement.ofSignal(signal));
            return this;
        }

        public abstract RichOutput build();
    }
}
