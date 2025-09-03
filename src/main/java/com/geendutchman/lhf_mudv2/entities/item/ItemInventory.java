package com.geendutchman.lhf_mudv2.entities.item;

import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.springframework.lang.NonNull;

import com.geendutchman.lhf_mudv2.entities.item.Item.BuildItem;
import com.google.auto.value.AutoBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

public final class ItemInventory implements ItemContainer.MutableItemContainer<ItemReference> {

    @AutoBuilder(callMethod = "buildInventory", ofClass = ItemInventory.class)
    public abstract static class Builder {

        protected Builder() {
        }

        public static Builder builder() {
            return ItemInventory.builder();
        }

        public abstract String name();

        public abstract Builder setName(String name);

        public abstract NavigableSet<Item.LockedItemBuilder> contents();

        public abstract Builder setContents(@NonNull NavigableSet<Item.LockedItemBuilder> contents);

        public final Builder addContents(Item.LockedItemBuilder... items) {
            NavigableSet<Item.LockedItemBuilder> mycontents;
            try {
                mycontents = this.contents();
            } catch (IllegalStateException e) {
                mycontents = new TreeSet<>(Comparator.<Item.LockedItemBuilder, String>comparing(locked -> String
                        .format("%s:%s:%s", locked.getName(), locked.getNickname().orElse(""), locked.builderUuid())));
            }
            for (final Item.LockedItemBuilder item : items) {
                if (item != null) {
                    mycontents.add(item);
                }
            }
            return this.setContents(mycontents);
        }

        protected abstract ItemInventory autoBuild();

        public final ItemInventory build() {
            final ItemInventory built = this.autoBuild();
            return built;
        }
    }

    public static Builder builder() {
        return new AutoBuilder_ItemInventory_Builder().setName("Inventory")
                .setContents(new TreeSet<Item.LockedItemBuilder>(
                        Comparator.<Item.LockedItemBuilder, String>comparing(locked -> String.format("%s:%s:%s",
                                locked.getName(), locked.getNickname().orElse(""), locked.builderUuid()))));
    }

    public static ItemInventory buildInventory(String name, NavigableSet<Item.LockedItemBuilder> contents) {
        Preconditions.checkState(EXAMINABLE_NAME.matcher(name).matches(), "Inventory name '%s' does not match '%s'",
                name, EXAMINABLE_NAME.toString());
        final ItemInventory inv = new ItemInventory(name);
        if (contents != null) {
            inv.add(contents.stream().filter(locked -> locked != null).map(locked -> locked.build()).toList());
        }
        return inv;
    }

    private ItemInventory(String name) {
        this.name = name;
    }

    private final String name;
    private final ConcurrentSkipListSet<ItemReference> cargo = new ConcurrentSkipListSet<>(Item.getItemComparator());

    @Override
    public ImmutableSortedMap<String, String> attributes() {
        return ItemInventory.BASIC_TAGGABLE_ATTRIBUTES;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public ConcurrentSkipListSet<ItemReference> cargo() {
        return this.cargo;
    }

    public ItemInventory.Builder toBuilder() {
        Builder builder = ItemInventory.builder().setName(name);
        for (final ItemReference itemReference : cargo) {
            if (itemReference == null) {
                continue;
            }
            final BuildItem itemBuilder = Item.builder().setName(itemReference.name())
                    .setItemTag(itemReference.itemTag()).setNickname(itemReference.nickname())
                    .setVisibility(itemReference.visibility());
            builder.addContents(itemBuilder.lock());
        }
        return builder;
    }

}
