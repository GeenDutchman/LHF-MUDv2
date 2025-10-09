package com.geendutchman.lhf_mudv2.display;

import java.io.Serializable;
import java.util.Optional;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

@AutoValue
public abstract class RichOutput implements Serializable {

    public abstract Optional<Examinable.Name> sequenceName();

    public abstract ImmutableList<RichOutputElement> elements();

    public abstract Optional<RichOutputElement> elementSeparator();

    public abstract boolean isAndLast();

    public abstract Optional<Taggable.Tag> tag();

    public abstract ImmutableSortedMap<String, String> attributes();

    public abstract Optional<String> onEmpty();

    public static Builder builder() {
        return new AutoValue_RichOutput.Builder().setElementSeparator(Optional.of(RichOutputElement.ofString(" ")))
                .setTag("output").setIsAndLast(false);
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setSequenceName(Optional<Examinable.Name> sequenceName);

        public Builder setSequenceName(String sequenceName) {
            Examinable.Name ename = new Examinable.Name(sequenceName);
            return this.setSequenceName(Optional.ofNullable(ename));
        }

        public abstract Builder setElements(Iterable<RichOutputElement> elements);

        public abstract Builder setElements(RichOutputElement... elements);

        public abstract Builder setElementSeparator(Optional<RichOutputElement> separator);

        public abstract Builder setIsAndLast(boolean andLast);

        public abstract Builder setTag(Optional<Taggable.Tag> tag);

        public Builder setTag(String tag) {
            Taggable.Tag ttag = new Taggable.Tag(tag);
            return this.setTag(Optional.ofNullable(ttag));
        }

        abstract ImmutableSortedMap.Builder<String, String> attributesBuilder();

        public final Builder putAttribute(String key, String value) {
            attributesBuilder().put(key, value);
            return this;
        }

        public abstract Builder setOnEmpty(Optional<String> onEmpty);

        abstract ImmutableList.Builder<RichOutputElement> elementsBuilder();

        public final Builder addElement(RichOutputElement element) {
            elementsBuilder().add(element);
            return this;
        }

        public final Builder addString(String charsequence) {
            return this.addElement(RichOutputElement.ofString(charsequence));
        }

        public final Builder addTaggable(Taggable tagged) {
            return this.addElement(RichOutputElement.ofTaggable(tagged));
        }

        public final Builder addExaminable(Examinable examinable) {
            return this.addElement(RichOutputElement.ofExaminable(examinable));
        }

        public final Builder addSignal(String signal) {
            elementsBuilder().add(RichOutputElement.ofSignal(signal));
            return this;
        }

        public final Builder addOutput(RichOutput output) {
            return this.addElement(RichOutputElement.ofNested(output));
        }

        public final Builder addPolymorphic(String string) {
            return this.addString(string);
        }

        public final Builder addPolymorphic(RichOutputElement element) {
            return this.addElement(element);
        }

        public final Builder addPolymorphic(Taggable taggable) {
            return this.addTaggable(taggable);
        }

        public final Builder addPolymorphic(Examinable examinable) {
            return this.addExaminable(examinable);
        }

        public abstract RichOutput build();

    }

    public final String printIt() {
        return StringVisitor.buildString(this);
    }

}
