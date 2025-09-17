package com.geendutchman.lhf_mudv2.entities.item;

import java.io.Serializable;
import java.net.URI;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import com.geendutchman.lhf_mudv2.dice.Difficulty;
import com.geendutchman.lhf_mudv2.dice.DifficultyMods;
import com.geendutchman.lhf_mudv2.dice.Plain;
import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
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
     * An optional nickname for the item
     */
    public abstract Optional<Examinable.Name> nickname();

    /**
     * Is the item visible or not?
     */
    public abstract Difficulty<Plain> visibility();

    /**
     * Returns either the nickname if present, or the actual name
     */
    public default Examinable.Name displayName() {
        return this.nickname().orElse(this.name());
    }

    @Override
    public default String content() {
        return this.displayName().toString();
    }

    public default boolean isStateful() {
        return false;
    }

    public static enum ItemTag {
        ITEM;

        public final Taggable.Tag tag = new Taggable.Tag(this.name());

        public Taggable.Tag asTag() {
            return this.tag;
        }
    }

    public abstract ItemTag itemTag();

    @Override
    public default Tag tag() {
        final ItemTag itemTag = this.itemTag();
        if (itemTag == null) {
            return ItemTag.ITEM.tag;
        }
        return itemTag.asTag();
    }

    @AutoOneOf(Delta.Kind.class)
    public static abstract class Delta implements Serializable {
        public enum Kind {
            VISIBILITY, NICKNAME, LOCALE
        }

        public abstract Kind kind();

        public abstract Optional<DifficultyMods<Plain>> visibility();

        public abstract Optional<Examinable.Name> nickname();

        public abstract Optional<URI> locale();

        public static Delta ofVisibility(Optional<DifficultyMods<Plain>> visible) {
            return AutoOneOf_Item_Delta.visibility(visible);
        }

        public static Delta ofVisibility(DifficultyMods<Plain> visible) {
            return AutoOneOf_Item_Delta.visibility(Optional.of(visible));
        }

        public static Delta ofNickname(Optional<Examinable.Name> nickname) {
            return AutoOneOf_Item_Delta.nickname(nickname);
        }

        public static Delta ofNickname(String name) {
            Examinable.Name eName = new Examinable.Name(name);
            return Delta.ofNickname(Optional.of(eName));
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

    public default ItemBuilderFactory.BuildItem toBuilder() {
        return new AutoBuilder_ItemBuilderFactory_Builder().setVisibility(this.visibility()).setName(this.name())
                .setItemTag(this.itemTag()).setNickname(this.nickname());
    }

}
