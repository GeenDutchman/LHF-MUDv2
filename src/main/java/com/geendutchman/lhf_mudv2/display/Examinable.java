package com.geendutchman.lhf_mudv2.display;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

/**
 * A unit of display that can further describe itself.
 * 
 * It has a string name, an optional RichOutput description and all the things a
 * taggable has
 */
public interface Examinable extends Taggable {

    // A name for an Examinable
    public static record Name(String value) implements Serializable, Comparable<Name>, CharSequence {
        /**
         * Examinable names should adhere to this regex: ^\\w{3,}( \\w+)*$
         */
        public final static String NAME_PATTERN = "^\\w{3,}( \\w+)*$";

        public Name {
            Preconditions.checkNotNull(value, "name value must not be null");
            Preconditions.checkArgument(value.matches(NAME_PATTERN), "name '%s' must match '%s'", value, NAME_PATTERN);
        }

        @Override
        public int compareTo(Name o) {
            return this.value.compareTo(o.value);
        }

        @Override
        public final String toString() {
            return this.value;
        }

        @Override
        public int length() {
            return this.value.length();
        }

        @Override
        public char charAt(int index) {
            return this.value.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return this.value.subSequence(end, end);
        }

    }

    /**
     * An examinable must have a name, and it must match: ^\\w{3,}( \\w+)*$
     * 
     * @return
     */
    public Name name();

    /**
     * There's simple contents, and then there's a more formatted extra description
     * 
     * @return
     */
    public Optional<RichOutput> description();

    /**
     * Transform this into an immutable unit
     * 
     * @return
     */
    public default BasicExaminable basicExaminable() {
        return new BasicExaminable(this.name(), this.description(), this.attributes(), this.content(), this.tag());
    }

    /**
     * A concretion of Examinable
     */
    public static record BasicExaminable(Name name, Optional<RichOutput> description,
            ImmutableSortedMap<String, String> attributes, String content, Tag tag)
            implements Examinable, Serializable {
        public BasicExaminable {
            Taggable.taggablepreconditions(tag, content, attributes);
            Preconditions.checkNotNull(name, "examinable name must not be null");
        }

        @Override
        public final BasicExaminable basicExaminable() {
            return this;
        }
    }

    public static class ExaminableComparator implements Comparator<Examinable>, Serializable {
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
