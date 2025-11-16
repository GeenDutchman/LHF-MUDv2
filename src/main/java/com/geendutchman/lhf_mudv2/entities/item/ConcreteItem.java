package com.geendutchman.lhf_mudv2.entities.item;

import java.util.Objects;
import java.util.Optional;

import com.geendutchman.lhf_mudv2.dice.Difficulty;
import com.geendutchman.lhf_mudv2.dice.DifficultyMods;
import com.geendutchman.lhf_mudv2.dice.Plain;
import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.google.common.base.Preconditions;

final class ConcreteItem implements Item {
    final private ItemID itemID;
    final private Examinable.Name name;
    final private ItemTag itemTag;
    private Difficulty<Plain> visibility;
    private Optional<Examinable.Name> nickname;
    private Optional<IEntityID> locale = Optional.empty();

    protected static ConcreteItem buildItem(Examinable.Name name, Difficulty<Plain> visibility,
            Optional<Examinable.Name> nickname, ItemTag itemTag, Optional<IEntityID> locale) {
        Preconditions.checkNotNull(name, "name should not be null");
        Preconditions.checkNotNull(nickname, "nickname can be empty but should not be null");
        Preconditions.checkNotNull(visibility, "visibility difficulty can be zero but must not be null");
        Preconditions.checkNotNull(locale, "locale is null, did you mean empty?");
        ConcreteItem item = new ConcreteItem(name, visibility, nickname, itemTag);
        item.locale = locale;
        return item;
    }

    private ConcreteItem(Examinable.Name name, Difficulty<Plain> visibility, Optional<Examinable.Name> nickname,
            ItemTag itemTag) {
        this.name = name;
        this.itemTag = itemTag;
        this.itemID = ItemID.make(name);
        this.visibility = visibility;
        this.nickname = nickname;
    }

    @Override
    public Optional<IEntityID> locale() {
        return this.locale;
    }

    public void setLocale(Optional<IEntityID> nextPlace) {
        if (nextPlace == null) {
            this.locale = Optional.empty();
        } else {
            this.locale = nextPlace;
        }
    }

    @Override
    public void applyDelta(Delta delta) {
        if (delta == null) {
            return;
        }

        switch (delta) {
        case Delta.SetVisibilityDelta(DifficultyMods<Plain> visible) -> {
            if (visible != null) {
                this.visibility = visible.apply(this.visibility);
            }
        }
        case Delta.SetNicknameDelta(Optional<Examinable.Name> nickname) -> {
            this.nickname = nickname != null ? nickname : Optional.empty();
        }
        case Delta.SetLocale(Optional<IEntityID> locale) -> {
            this.locale = locale != null ? locale : Optional.empty();
        }
        case null -> {
        }
        default -> {
        }

        }

    }

    @Override
    public Difficulty<Plain> visibility() {
        return visibility;
    }

    @Override
    public Optional<Examinable.Name> nickname() {
        return this.nickname;
    }

    @Override
    public ItemID itemID() {
        return this.itemID;
    }

    @Override
    public Examinable.Name name() {
        return this.name;
    }

    @Override
    public Optional<RichOutput> description() {
        return Optional.of(RichOutput.builder().addTaggable(this).addString("Is an item").build());
    }

    public ItemTag itemTag() {
        return this.itemTag;
    }

    @Override
    public Taggable.Tag tag() {
        return this.itemTag.asTag();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ConcreteItem [itemID=").append(itemID).append(", name=").append(name).append(", itemTag=")
                .append(itemTag);
        if (nickname != null && nickname.isPresent()) {
            builder.append(", nickname=").append(nickname.map(nn -> nn.toString()).orElse(""));
        }
        if (locale != null && locale.isPresent()) {
            builder.append(", locale=").append(locale.get());
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemID);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof ConcreteItem))
            return false;
        ConcreteItem other = (ConcreteItem) obj;
        return Objects.equals(itemID, other.itemID);
    }

}