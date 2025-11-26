package com.geendutchman.lhf_mudv2.entities.item;

import java.io.Serializable;
import java.net.URI;
import java.util.Comparator;
import java.util.Optional;

import com.geendutchman.lhf_mudv2.dice.Difficulty;
import com.geendutchman.lhf_mudv2.dice.DifficultyMods;
import com.geendutchman.lhf_mudv2.dice.Plain;
import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidFactory;
import com.google.common.base.Preconditions;

public interface Item extends Entity {

    public record ItemID(EntityID delegate) implements IEntityID {
        public ItemID {
            Preconditions.checkNotNull(delegate, "ItemID should not have null delegate");
            Preconditions.checkState(delegate.entityClass().equals(ENTITY_CLASS_ITEM),
                    "an item id must be about items, but was %s", delegate.entityClass());
        }

        public static final Taggable.Tag ENTITY_CLASS_ITEM = new Tag("items");
        protected static final TsidFactory tsidFactory = TsidFactory
                .newInstance1024(Math.abs("items".hashCode() % 1024));

        public static ItemID make(Examinable.Name name) {
            return new ItemID(new EntityID(ENTITY_CLASS_ITEM, name, tsidFactory.create()));
        }

        public Taggable.Tag entityClass() {
            return this.delegate.entityClass();
        }

        public Examinable.Name name() {
            return this.delegate.name();
        }

        public URI uri() {
            return this.delegate.uri();
        }

        @Override
        public Tsid tsid() {
            return this.delegate.tsid();
        }

        @Override
        public String toString() {
            return this.uri().toString();
        }

    }

    /**
     * An identifier to specify the item
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

    public sealed static interface Delta extends Serializable {

        public record SetVisibilityDelta(DifficultyMods<Plain> visibililty) implements Delta {
            public SetVisibilityDelta {
                Preconditions.checkNotNull(visibililty, "visibility delta must not be null");
            }
        }

        public record SetNicknameDelta(Optional<Examinable.Name> nickname) implements Delta {
            public SetNicknameDelta {
                Preconditions.checkNotNull(nickname, "nickname may be empty but must not be null");
            }
        }

        public record SetLocale(Optional<IEntityID> locale) implements Delta {
            public SetLocale {
                Preconditions.checkNotNull(locale, "locale must not be null, but may be empty");
            }
        }

        public static Delta ofVisibility(DifficultyMods<Plain> visible) {
            return new SetVisibilityDelta(visible);
        }

        public static Delta ofNickname(Optional<Examinable.Name> nickname) {
            return new SetNicknameDelta(nickname);
        }

        public static Delta ofNickname(String name) {
            Examinable.Name eName = new Examinable.Name(name);
            return Delta.ofNickname(Optional.of(eName));
        }

        public static Delta ofLocale(Optional<IEntityID> locale) {
            return new SetLocale(locale);
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
        return ItemBuilderFactory.builder().setName(this.name()).setVisibility(this.visibility())
                .setItemTag(this.itemTag()).setNickname(this.nickname());
    }

}
