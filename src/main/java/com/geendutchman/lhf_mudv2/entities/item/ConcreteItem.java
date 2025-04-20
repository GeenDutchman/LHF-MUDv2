package com.geendutchman.lhf_mudv2.entities.item;

import java.net.URI;
import java.util.Optional;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.events.Event;
import com.geendutchman.lhf_mudv2.events.EventBus;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

class ConcreteItem implements Item {
    final private ItemID itemID = ItemID.make();
    final private String name;
    final private ItemTag itemTag;
    private boolean visible = true;
    private Optional<String> nickname;
    private Optional<URI> locale;

    protected static ConcreteItem buildItem(String name, boolean visible, Optional<String> nickname, ItemTag itemTag,
            Optional<URI> locale) {
        Preconditions.checkArgument(EXAMINABLE_NAME.asMatchPredicate().test(name),
                "name '%s' must match expression: %s", name, EXAMINABLE_NAME);
        if (nickname.isPresent()) {
            Preconditions.checkArgument(NICKNAME_RULES.asMatchPredicate().test(nickname.get()),
                    "nickname '%s' must match expression: %s", nickname.get(), NICKNAME_RULES);
        }
        Preconditions.checkNotNull(locale, "locale is null, did you mean empty?");
        ConcreteItem item = new ConcreteItem(name, visible, nickname, itemTag);
        return item;
    }

    public ItemBuilderFactory.Builder toBuilder() {
        return new AutoBuilder_ItemBuilderFactory_Builder(this);
    }

    private ConcreteItem(String name, boolean visible, Optional<String> nickname, ItemTag itemTag) {
        this.name = name;
        this.itemTag = itemTag;
        this.visible = visible;
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
            this.visible = delta.visibility();
            break;
        case LOCALE:
            this.locale = delta.locale();
            break;
        default:
            break;
        }
    }

    @Override
    public void processEvent(Event event, EventBus bus) {
        // TODO: inner handlers
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public Optional<String> nickname() {
        return this.nickname;
    }

    @Override
    public ItemID itemID() {
        return this.itemID;
    }

    @Override
    public String name() {
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
    public String tag() {
        return this.itemTag.name();
    }

    @Override
    public ImmutableSortedMap<String, String> attributes() {
        return ImmutableSortedMap.copyOf(Taggable.produceBasicTagAttributes());
    }

}