package com.geendutchman.lhf_mudv2.entities.item;

import java.io.Serializable;
import java.net.URI;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.geendutchman.lhf_mudv2.dice.Difficulty;
import com.geendutchman.lhf_mudv2.dice.DifficultyMods;
import com.geendutchman.lhf_mudv2.dice.Plain;
import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.entities.Entity;
import com.geendutchman.lhf_mudv2.entities.IEntityID;
import com.geendutchman.lhf_mudv2.events.EventBus;
import com.geendutchman.lhf_mudv2.events.EventProcessor;
import com.google.auto.value.AutoBuilder;
import com.google.auto.value.AutoOneOf;
import com.google.common.base.Preconditions;

public interface Item extends Entity {

    public record ItemID(EntityID delegate) implements IEntityID {
        public ItemID {
            Preconditions.checkNotNull(delegate, "ItemID should not have null delegate");
            Preconditions.checkState(delegate.entityClass().equals("items"),
                    "an item id must be about items, but was %s", delegate.entityClass());
        }

        public static ItemID make(String name) {
            return new ItemID(new EntityID("items", name, UUID.randomUUID()));
        }

        public String entityClass() {
            return this.delegate.entityClass();
        }

        public String name() {
            return this.delegate.name();
        }

        public URI uri() {
            return this.delegate.uri();
        }

        @Override
        public UUID uuid() {
            return this.delegate.uuid();
        }

        @Override
        public String toString() {
            return this.uri().toString();
        }

    }

    /**
     * A uuid to specify the item
     */
    @Override
    public default IEntityID identifier() {
        return this.itemID();
    }

    public abstract ItemID itemID();

    /**
     * The nickname must adhere to this regex
     */
    public static final Pattern NICKNAME_RULES = Examinable.EXAMINABLE_NAME;

    /**
     * An optional nickname for the item
     */
    public abstract Optional<String> nickname();

    /**
     * Is the item visible or not?
     */
    public abstract Difficulty<Plain> visibility();

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

    public static enum ItemTag {
        ITEM;
    }

    public abstract ItemTag itemTag();

    @Override
    public default String tag() {
        final ItemTag itemTag = this.itemTag();
        if (itemTag == null) {
            return "ITEM";
        }
        return itemTag.name();
    }

    @AutoOneOf(Delta.Kind.class)
    public static abstract class Delta implements Serializable {
        public enum Kind {
            VISIBILITY, NICKNAME, LOCALE
        }

        public abstract Kind kind();

        public abstract Optional<DifficultyMods<Plain>> visibility();

        public abstract Optional<String> nickname();

        public abstract Optional<URI> locale();

        public static Delta ofVisibility(Optional<DifficultyMods<Plain>> visible) {
            return AutoOneOf_Item_Delta.visibility(visible);
        }

        public static Delta ofVisibility(DifficultyMods<Plain> visible) {
            return AutoOneOf_Item_Delta.visibility(Optional.of(visible));
        }

        public static Delta ofNickname(Optional<String> nickname) {
            return AutoOneOf_Item_Delta.nickname(nickname);
        }

        public static Delta ofLocale(Optional<URI> locale) {
            return AutoOneOf_Item_Delta.locale(locale);
        }
    }

    public abstract void applyDelta(Delta delta);

    public static class ItemComparator implements Comparator<Item>, Serializable {
        private static Comparator<Entity> delegate = Entity.getEntityComparator();

        @Override
        public int compare(Item o1, Item o2) {
            if (o1 == null || o2 == null) {
                throw new NullPointerException("Cannot compare null Items");
            }
            if (o1.equals(o2)) {
                return 0;
            }
            int displayCompare = o1.displayName().compareTo(o2.displayName());
            if (displayCompare != 0) {
                return displayCompare;
            }
            return delegate.compare(o1, o2);
        }
    }

    public static Comparator<Item> getItemComparator() {
        return new ItemComparator();
    }

    public static sealed interface BuilderStart extends Serializable permits Item.BuildItem {
        @Autowired
        public BuilderStart setItemRepository(ItemRepository itemRepository);

        @Autowired
        public BuilderStart setEventBus(EventBus eventBus);

        public Item.BuildItem setName(@NonNull String name);
    }

    public static sealed interface BuildItem extends BuilderStart permits Item.Builder {
        public String getName();

        public BuildItem setVisibility(Difficulty<Plain> visible);

        public BuildItem setNickname(String nickname);

        public BuildItem setNickname(Optional<String> nickname);

        public Optional<String> getNickname();

        public BuildItem setItemTag(Item.ItemTag tag);

        public BuildItem setLocale(Optional<URI> locale);

        public BuildItem setEventFunction(@Nullable EventProcessor.EventFunction<Item> eventProcessor);

        public LockedItemBuilder lock();

        @Autowired
        public BuildItem setItemRepository(ItemRepository itemRepository);

        @Autowired
        public BuildItem setEventBus(EventBus eventBus);

        public Item build();
    }

    public static sealed interface LockedItemBuilder extends Serializable, Comparable<LockedItemBuilder>
            permits Item.Builder {
        public Item build();

        public String getName();

        public Optional<String> getNickname();

        public UUID builderUuid();

        @Override
        public default int compareTo(LockedItemBuilder o) {
            return String.format("%s:%s:%s", this.getName(), this.getNickname().orElse(""), this.builderUuid())
                    .compareTo(String.format("%s:%s:%s", o.getName(), o.getNickname().orElse(""), o.builderUuid()));
        }

    }

    @AutoBuilder(callMethod = "buildItem", ofClass = Item.class)
    public non-sealed abstract static class Builder implements BuildItem, LockedItemBuilder {
        final private UUID builderUuid = UUID.randomUUID();

        protected Builder() {
        }

        public final UUID builderUuid() {
            return this.builderUuid;
        }

        @Override
        public final LockedItemBuilder lock() {
            return this;
        }

        abstract Item autoBuild();

        @Override
        public final Item build() {
            Preconditions.checkState(Item.EXAMINABLE_NAME.matcher(this.getName()).matches(),
                    "Item name '%s' must match '%s'", this.getName(), Item.EXAMINABLE_NAME.toString());
            if (this.getNickname().isPresent()) {
                Preconditions.checkState(Item.NICKNAME_RULES.matcher(this.getNickname().orElse("")).matches(),
                        "Item nickname '%s' must match '%s'", this.getNickname().orElse(""),
                        Item.NICKNAME_RULES.toString());
            }
            return this.autoBuild();
        }

    }

    public static Item.BuilderStart builder() {
        final Item.Builder builder = new AutoBuilder_Item_Builder();
        builder.setVisibility(Plain.noDifficulty()).setItemTag(ItemTag.ITEM);
        builder.setLocale(Optional.empty());
        return builder;
    }

    public default Item.BuildItem toBuilder() {
        return new AutoBuilder_Item_Builder().setVisibility(this.visibility()).setName(this.name())
                .setItemTag(this.itemTag()).setNickname(this.nickname());
    }

    public static Item buildItem(EventBus eventBus, ItemRepository itemRepository, String name,
            Difficulty<Plain> visibility, Optional<String> nickname, ItemTag itemTag, Optional<URI> locale,
            @Nullable EventProcessor.EventFunction<Item> eventFunction) {
        Preconditions.checkNotNull(eventBus, "event bus must not be null");
        Preconditions.checkNotNull(itemRepository, "item repository must be available to store item into");
        final ConcreteItem item = ConcreteItem.buildItem(name, visibility, nickname, itemTag, locale, eventFunction);
        eventBus.register(item);
        itemRepository.add(item);
        return item;
    }

}
