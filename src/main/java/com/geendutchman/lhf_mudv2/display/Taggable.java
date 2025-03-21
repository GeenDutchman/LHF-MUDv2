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
    public String tag();

    public String content();

    public ImmutableSortedMap<String, String> attributes();

    public static NavigableMap<String, String> produceBasicTagAttributes() {
        NavigableMap<String, String> tagAttributes = new TreeMap<>();
        tagAttributes.put("colored", "true");
        return tagAttributes;
    }

    public final static String TAG_PATTERN = "^\\w{3}[\\w_-]+\\w$";

    public static BasicTaggable basicTaggable(Taggable other) {
        final String tag = other.tag();
        final String content = other.content();
        final NavigableMap<String, String> attributes = other.attributes();
        Preconditions.checkArgument(!tag.isBlank(), "tag must not be empty or blank");
        Preconditions.checkArgument(tag.matches(TAG_PATTERN), "tag must match '%s'", TAG_PATTERN);
        Preconditions.checkArgument(!content.isBlank(), "content must not be empty or blank");
        Preconditions.checkArgument(attributes != null, "attributes must not be null");
        return new AutoValue_Taggable_BasicTaggable(tag, content, ImmutableSortedMap.copyOfSorted(attributes));
    }

    @AutoValue
    /**
     * A concretion of Taggable
     */
    public static abstract class BasicTaggable implements Taggable {
        public static BasicTaggable customTaggable(String tag, String content,
                NavigableMap<String, String> attributes) {
            final String trimmedTag = tag.trim();
            final String trimmedContent = content.trim();
            Preconditions.checkArgument(!trimmedTag.isBlank(), "tag must not be empty or blank");
            Preconditions.checkArgument(tag.matches(TAG_PATTERN), "tag must match '%s'", TAG_PATTERN);
            Preconditions.checkArgument(!trimmedContent.isBlank(), "content must not be empty or blank");
            Preconditions.checkArgument(attributes != null, "attributes must not be null");
            return new AutoValue_Taggable_BasicTaggable(trimmedTag, trimmedContent,
                    ImmutableSortedMap.copyOf(attributes));
        }
    }

}
