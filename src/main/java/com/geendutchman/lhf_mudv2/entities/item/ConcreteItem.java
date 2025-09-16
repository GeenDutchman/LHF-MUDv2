package com.geendutchman.lhf_mudv2.entities.item;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import org.springframework.lang.Nullable;

import com.geendutchman.lhf_mudv2.dice.Difficulty;
import com.geendutchman.lhf_mudv2.dice.Plain;
import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.events.Event;
import com.geendutchman.lhf_mudv2.events.EventBus;
import com.geendutchman.lhf_mudv2.events.EventProcessor;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

final class ConcreteItem implements Item {
    final private ItemID itemID;
    final private Examinable.Name name;
    final private ItemTag itemTag;
    @Nullable
    final private transient EventProcessor.EventFunction<Item> eventFunction;
    private Difficulty<Plain> visibility;
    private Optional<Examinable.Name> nickname;
    private Optional<URI> locale = Optional.empty();

    protected static ConcreteItem buildItem(Examinable.Name name, Difficulty<Plain> visibility,
            Optional<Examinable.Name> nickname, ItemTag itemTag, Optional<URI> locale,
            @Nullable EventProcessor.EventFunction<Item> eventFunction) {
        Preconditions.checkNotNull(name, "name should not be null");
        Preconditions.checkNotNull(nickname, "nickname can be empty but should not be null");
        Preconditions.checkNotNull(visibility, "visibility difficulty can be zero but must not be null");
        Preconditions.checkNotNull(locale, "locale is null, did you mean empty?");
        ConcreteItem item = new ConcreteItem(name, visibility, nickname, itemTag, eventFunction);
        return item;
    }

    private ConcreteItem(Examinable.Name name, Difficulty<Plain> visibility, Optional<Examinable.Name> nickname,
            ItemTag itemTag, @Nullable EventProcessor.EventFunction<Item> eventFunction) {
        this.name = name;
        this.itemTag = itemTag;
        this.eventFunction = eventFunction != null ? eventFunction : (e, b, i) -> new ProcessingResult.Unhandled();
        this.itemID = ItemID.make(name.toString());
        this.visibility = visibility;
        this.nickname = nickname;
    }

    @Override
    public URI processorURI() {
        return this.itemID.uri();
    }

    @Override
    public Optional<URI> locale() {
        return this.locale;
    }

    public void setLocale(Optional<URI> nextPlace) {
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
        switch (delta.kind()) {
        case NICKNAME:
            this.nickname = delta.nickname();
            break;
        case VISIBILITY:
            delta.visibility().ifPresent(mod -> this.visibility = mod.apply(this.visibility));
            break;
        case LOCALE:
            this.locale = delta.locale();
            break;
        default:
            break;
        }
    }

    @Override
    public ProcessingResult processEvent(Event event, EventBus bus) {
        return this.eventFunction != null ? this.eventFunction.apply(event, bus, this)
                : new ProcessingResult.Unhandled();
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
    public ImmutableSortedMap<String, String> attributes() {
        return ImmutableSortedMap.copyOf(Taggable.produceBasicTagAttributes());
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