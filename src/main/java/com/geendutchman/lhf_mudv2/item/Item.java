package com.geendutchman.lhf_mudv2.item;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.google.auto.value.AutoBuilder;
import com.google.auto.value.AutoOneOf;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

@Component
public interface Item extends Examinable, Serializable {

    static HashMap<ItemID, ConcreteItem> itemRepo = new HashMap<>();

    public record ItemID(UUID uuid) implements Comparable<ItemID> {
        public ItemID {
            Preconditions.checkNotNull(uuid, "ItemID should not have null uuid");
        }

        public static ItemID make() {
            return new ItemID(UUID.randomUUID());
        }

        @Override
        public int compareTo(ItemID o) {
            if (o == null) {
                throw new NullPointerException("cannot compare to nil ItemID");
            }
            if (this == o) {
                return 0;
            }
            return this.uuid.compareTo(o.uuid);
        }
    }

    /**
     * A uuid to specify the item
     */
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
    public abstract boolean isVisible();

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

    public interface BuilderStart extends Serializable {
        public BuildItem setName(String name);
    }

    public interface BuildItem extends Serializable {
        public BuildItem setVisible(boolean visible);

        public BuildItem setNickname(Optional<String> nickname);

        public BuildItem setItemTag(ItemTag tag);

        public Item build();
    }

    @Component
    @AutoBuilder(callMethod = "buildItem", ofClass = ConcreteItem.class)
    public abstract class Builder implements BuilderStart, BuildItem {
        final private UUID builderUuid = UUID.randomUUID();

        public final UUID builderUuid() {
            return this.builderUuid;
        }
    }

    public static BuilderStart builder() {
        final Builder builder = new AutoBuilder_Item_Builder();
        builder.setVisible(true).setItemTag(ItemTag.ITEM);
        return builder;
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
            VISIBILITY, NICKNAME
        }

        public abstract Kind kind();

        public abstract boolean visibility();

        public abstract Optional<String> nickname();

        public static Delta ofVisibility(boolean visible) {
            return AutoOneOf_Item_Delta.visibility(visible);
        }

        public static Delta ofNickname(Optional<String> nickname) {
            return AutoOneOf_Item_Delta.nickname(nickname);
        }
    }

    public abstract void applyDelta(Delta delta);

    static class ConcreteItem implements Item {
        final private ItemID itemID = ItemID.make();
        final private String name;
        final private ItemTag itemTag;
        private boolean visible = true;
        private Optional<String> nickname;

        static Item buildItem(String name, boolean visible, Optional<String> nickname, ItemTag itemTag) {
            Preconditions.checkArgument(EXAMINABLE_NAME.asMatchPredicate().test(name),
                    "name '%s' must match expression: %s", name, EXAMINABLE_NAME);
            if (nickname.isPresent()) {
                Preconditions.checkArgument(NICKNAME_RULES.asMatchPredicate().test(nickname.get()),
                        "nickname '%s' must match expression: %s", nickname.get(), NICKNAME_RULES);
            }
            ConcreteItem item = new ConcreteItem(name, visible, nickname, itemTag);
            Item.itemRepo.put(item.itemID, item);
            return item;
        }

        public Builder toBuilder() {
            return new AutoBuilder_Item_Builder(this);
        }

        private ConcreteItem(String name, boolean visible, Optional<String> nickname, ItemTag itemTag) {
            this.name = name;
            this.itemTag = itemTag;
            this.visible = visible;
            this.nickname = nickname;
        }

        // @Override
        // public void listen(Flux<Event> eventStream) {
        // System.out.println("print debugging whoot");
        // this.eventsIn = Flux.merge(this.eventsIn,
        // eventStream.subscribeOn(Schedulers.boundedElastic(), false))
        // .name("listener").checkpoint(String.format("merging for %s",
        // this.uuid)).doOnNext(event -> {
        // event.description().ifPresentOrElse(desc ->
        // System.out.println(desc.printIt()),
        // () -> System.out.println("Nu'un"));
        // this.visible = true;
        // this.sink.tryEmitNext(event);
        // }).doOnError(System.err::println).doOnSubscribe(sub -> System.out.println("We
        // have a subscriber!"))
        // .subscribeOn(Schedulers.boundedElastic(), false);
        // }

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
            default:
                break;
            }
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

    public static class ItemComparator implements Comparator<Item>, Serializable {
        private static Comparator<Examinable> delegate = Examinable.getExaminableComparator();

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
