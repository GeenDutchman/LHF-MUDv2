package com.geendutchman.lhf_mudv2.display;

import java.util.NavigableMap;
import java.util.Optional;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

/**
 * A unit of display that can further describe itself.
 */
public interface Examinable extends Taggable {
    /**
     * An examinable must have a name, and it must match: ^\\w{3}
     * 
     * @return
     */
    public String name();

    /**
     * There's simple contents, and then there's a more formatted extra description
     * 
     * @return
     */
    public Optional<RichOutput> description();

    /**
     * Examinable names should adhere to this regex
     */
    public final static String EXAMINABLE_NAME = "^\\w{3}";

    /**
     * Transform this into an immutable unit
     * 
     * @return
     */
    public default BasicExaminable basicExaminable() {
        return BasicExaminable.customExaminable(this.name(), this.description(), this.attributes(), this.content(),
                this.tag());
    }

    /**
     * A concretion of Examinable
     */
    @AutoValue
    public static abstract class BasicExaminable implements Examinable {
        public static BasicExaminable customExaminable(String name, Optional<RichOutput> description,
                NavigableMap<String, String> attributes, String content, String tag) {
            final String trimmedName = name.trim();
            final String trimmedTag = tag.trim();
            Taggable.taggablepreconditions(trimmedTag, content, attributes);
            Preconditions.checkArgument(trimmedName.matches(EXAMINABLE_NAME), "name must match '%s'", EXAMINABLE_NAME);
            return new AutoValue_Examinable_BasicExaminable(ImmutableSortedMap.copyOf(attributes), content, trimmedTag,
                    trimmedName, description);
        }

        @Override
        public final BasicExaminable basicExaminable() {
            return this;
        }
    }
}
