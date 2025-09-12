package com.geendutchman.lhf_mudv2.display;

import java.io.Serializable;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

/**
 * A basic unit of display.
 * 
 * It has a tag string and a content string, as well as a map of string to
 * string of attributes
 */
public interface Taggable {

    /**
     * The tag name, like in xml, but with no alligators. This is not limited to
     * html tags, it can be anything legal in xml.
     * 
     * @return tag
     */
    public record Tag(String value) implements Serializable, Comparable<Tag>, CharSequence {
        /**
         * All tags need to adhere to: ^\\w{3}(?:[_-]?\\w)*$
         */
        public final static String TAG_PATTERN = "^\\w{3}(?:[_-]?\\w)*$";

        public Tag {
            Preconditions.checkNotNull(value, "tag value must not be null");
            Preconditions.checkArgument(value.matches(TAG_PATTERN), "tag '%s' must match '%s'", value, TAG_PATTERN);
        }

        @Override
        public int compareTo(Tag o) {
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
     * The tag name, like in xml, but with no alligators. This is not limited to
     * html tags, it can be anything legal in xml.
     * 
     * @return tag
     */
    public Tag tag();

    /**
     * The contents of the tag, like {@code<z>contents</z>}
     * 
     * @return content
     */
    public String content();

    /**
     * The attributes of the tag like {@code
     * 
    <p colored="true">
     * }
     * 
     * @return attributes
     */
    public default ImmutableSortedMap<String, String> attributes() {
        return Taggable.BASIC_TAGGABLE_ATTRIBUTES;
    }

    /**
     * Most taggables want to render in some colored way. They will at least need
     * {@code colored="true"}
     * 
     * @return
     */
    public static NavigableMap<String, String> produceBasicTagAttributes() {
        NavigableMap<String, String> tagAttributes = new TreeMap<>();
        tagAttributes.put("colored", "true");
        return tagAttributes;
    }

    public final static ImmutableSortedMap<String, String> BASIC_TAGGABLE_ATTRIBUTES = ImmutableSortedMap
            .copyOf(Taggable.produceBasicTagAttributes());

    /**
     * Transform the taggable into an immutable unit
     * 
     * @return
     */
    public default BasicTaggable basicTaggable() {
        return BasicTaggable.customTaggable(this.tag(), this.content(), this.attributes());
    }

    /**
     * Preconditions that a Taggable needs to meet
     * 
     * @param tag
     * @param content
     * @param attributes
     */
    static void taggablepreconditions(Tag tag, String content, NavigableMap<String, String> attributes) {
        Preconditions.checkNotNull(tag, "tag must not be null");
        Preconditions.checkArgument(!content.isEmpty(), "content must not be empty");
        Preconditions.checkArgument(attributes != null, "attributes must not be null");
    }

    /**
     * A concretion of Taggable
     */
    @AutoValue
    public static abstract class BasicTaggable implements Taggable, Serializable {
        public static BasicTaggable customTaggable(Tag tag, String content, NavigableMap<String, String> attributes) {
            Taggable.taggablepreconditions(tag, content, attributes);
            return new AutoValue_Taggable_BasicTaggable(tag, content, ImmutableSortedMap.copyOf(attributes));
        }

        @Override
        public abstract ImmutableSortedMap<String, String> attributes();

        @Override
        public final BasicTaggable basicTaggable() {
            return this;
        }
    }

}
