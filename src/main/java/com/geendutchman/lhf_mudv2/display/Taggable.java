package com.geendutchman.lhf_mudv2.display;

import java.io.Serializable;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

/**
 * A basic unit of display
 */
public interface Taggable extends Serializable {
    /**
     * The tag name, like in xml {@code
     * 
    <h1>} or {@code
      * 
     
    <p>
      * }, but with no alligators. This is not limited to html tags, it can be
     * anything legal in xml.
     * 
     * @return tag
     */
    public String tag();

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
    public ImmutableSortedMap<String, String> attributes();

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

    /**
     * All tags need to adhere to: ^\\w{3}[\\w_-]+\\w$
     */
    public final static String TAG_PATTERN = "^\\w{3}[\\w_-]+\\w$";

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
    static void taggablepreconditions(String tag, String content, NavigableMap<String, String> attributes) {
        Preconditions.checkArgument(tag.matches(TAG_PATTERN), "tag must match '%s'", TAG_PATTERN);
        Preconditions.checkArgument(!content.isEmpty(), "content must not be empty");
        Preconditions.checkArgument(attributes != null, "attributes must not be null");
    }

    /**
     * A concretion of Taggable
     */
    @AutoValue
    public static abstract class BasicTaggable implements Taggable {
        public static BasicTaggable customTaggable(String tag, String content,
                NavigableMap<String, String> attributes) {
            final String trimmedTag = tag.trim();
            taggablepreconditions(trimmedTag, content, attributes);
            return new AutoValue_Taggable_BasicTaggable(trimmedTag, content, ImmutableSortedMap.copyOf(attributes));
        }

        @Override
        public final BasicTaggable basicTaggable() {
            return this;
        }
    }

}
