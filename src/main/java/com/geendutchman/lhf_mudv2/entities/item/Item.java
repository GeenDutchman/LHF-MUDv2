package com.geendutchman.lhf_mudv2.entities.item;

import java.io.Serializable;
import java.net.URI;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import com.geendutchman.lhf_mudv2.dice.Difficulty;
import com.geendutchman.lhf_mudv2.dice.DifficultyMods;
import com.geendutchman.lhf_mudv2.dice.Plain;
import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.entities.Entity;
import com.geendutchman.lhf_mudv2.entities.IEntityID;
import com.google.auto.value.AutoOneOf;
import com.google.common.base.Preconditions;

public interface Item extends Entity {

    public record ItemID(EntityID delegate) implements IEntityID {
        public ItemID {
            Preconditions.checkNotNull(delegate, "ItemID should not have null delegate");
        }

        public static ItemID make() {
            return new ItemID(IEntityID.ofEntityClass("items"));
        }

        public String entityClass() {
            return this.delegate.entityClass();
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

}
