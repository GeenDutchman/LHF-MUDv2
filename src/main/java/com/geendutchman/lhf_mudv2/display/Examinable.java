package com.geendutchman.lhf_mudv2.display;

import java.util.Comparator;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.regex.Pattern;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

/**
 * A unit of display that can further describe itself.
 * 
 * It has a string name, an optional RichOutput description and all the things a
 * taggable has
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
    public final static Pattern EXAMINABLE_NAME = Pattern.compile("^\\w{3,}( \\w+)*$");

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
            Preconditions.checkArgument(EXAMINABLE_NAME.asMatchPredicate().test(trimmedName), "name must match '%s'",
                    EXAMINABLE_NAME);
            return new AutoValue_Examinable_BasicExaminable(content, trimmedTag, trimmedName, description,
                    ImmutableSortedMap.copyOf(attributes));
        }

        @Override
        public abstract ImmutableSortedMap<String, String> attributes();

        @Override
        public final BasicExaminable basicExaminable() {
            return this;
        }
    }

    public static class ExaminableComparator implements Comparator<Examinable> {
        @Override
        public int compare(Examinable o1, Examinable o2) {
            if (o1 == null || o2 == null) {
                throw new NullPointerException("Cannot compare null Examinables");
            }
            if (o1.equals(o2)) {
                return 0;
            }
            int nameCompare = o1.name().compareTo(o2.name());
            if (nameCompare != 0) {
                return nameCompare;
            }
            return o1.description().toString().compareTo(o2.description().toString());
        }
    }

    public static Comparator<Examinable> getExaminableComparator() {
        return new ExaminableComparator();
    }

}
