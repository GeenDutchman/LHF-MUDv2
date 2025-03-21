package com.geendutchman.lhf_mudv2.item;

import java.util.NavigableMap;
import java.util.Optional;
import java.util.UUID;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSortedMap;

public interface Item extends Examinable {
    /**
     * A uuid to specify the item
     */
    public abstract UUID uuid();

    /**
     * The nickname must adhere to this regex
     */
    public static final String NICKNAME_RULES = Examinable.EXAMINABLE_NAME;

    /**
     * An optional nickname for the item
     */
    public abstract Optional<String> nickname();

    /**
     * Returns either the nickname if present, or the actual name
     */
    public default String displayName() {
        return this.nickname().orElse(this.name());
    }

    @Override
    public default String content() {
        return this.displayName();
    }

    public default boolean isStateful() {
        return false;
    }

    /**
     * Represents an item that does not have state
     */
    @AutoValue
    public static abstract class ImmutableItem implements Item {
        final private UUID uuid = UUID.randomUUID();

        final public UUID uuid() {
            return this.uuid;
        }

        @Override
        final public boolean isStateful() {
            return false;
        }

        public static Builder builder() {
            return new AutoValue_Item_ImmutableItem.Builder().setTag("Item")
                    .setAttributes(Taggable.produceBasicTagAttributes());
        }

        @AutoValue.Builder
        public static abstract class Builder {
            final private UUID builderUUID = UUID.randomUUID();

            final public UUID getBuilderUUID() {
                return this.builderUUID;
            }

            public abstract Builder setName(String name);

            public abstract Builder setNickname(Optional<String> nickname);

            abstract Builder setTag(String tag);

            abstract Builder setAttributes(NavigableMap<String, String> attributes);

            abstract ImmutableSortedMap.Builder<String, String> attributesBuilder();

            public final Builder putAttribute(String key, String value) {
                this.attributesBuilder().put(key, value);
                return this;
            }

            public abstract Builder setDescription(Optional<RichOutput> description);

            abstract ImmutableItem autoBuild();

            public ImmutableItem build() {
                final ImmutableItem built = this.autoBuild();
                return built;
            }
        }
    }
}
