package com.geendutchman.lhf_mudv2.entities.item;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.springframework.lang.NonNull;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.entities.item.Item.BuildItem;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.geendutchman.lhf_mudv2.entities.item.Item.LockedItemBuilder;
import com.google.auto.value.AutoBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

public final class ItemInventory implements ItemContainer {

    @AutoBuilder(callMethod = "buildInventory", ofClass = ItemInventory.class)
    public abstract static class Builder {

        protected Builder() {
        }

        public static Builder builder() {
            return ItemInventory.builder();
        }

        public abstract Examinable.Name name();

        public abstract Builder setName(Examinable.Name name);

        public Builder setName(String name) {
            Examinable.Name eName = new Examinable.Name(name);
            return this.setName(eName);
        }

        public abstract NavigableSet<Item.LockedItemBuilder> contents();

        public abstract Builder setContents(@NonNull NavigableSet<Item.LockedItemBuilder> contents);

        public final Builder addContents(Item.LockedItemBuilder... items) {
            NavigableSet<Item.LockedItemBuilder> mycontents;
            try {
                mycontents = this.contents();
            } catch (IllegalStateException e) {
                mycontents = new TreeSet<>(Comparator.<Item.LockedItemBuilder, String>comparing(
                        locked -> String.format("%s:%s:%s", locked.getName(),
                                locked.getNickname().map(nn -> nn.toString()).orElse(""), locked.builderUuid())));
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
                .setContents(new TreeSet<Item.LockedItemBuilder>(Comparator.<Item.LockedItemBuilder, String>comparing(
                        locked -> String.format("%s:%s:%s", locked.getName(),
                                locked.getNickname().map(nn -> nn.toString()).orElse(""), locked.builderUuid()))));
    }

    public static ItemInventory buildInventory(Examinable.Name name, NavigableSet<Item.LockedItemBuilder> contents) {
        Preconditions.checkArgument(name != null, "name should not be null");
        final ItemInventory inv = new ItemInventory(name);
        if (contents != null) {
            for (final LockedItemBuilder locked : contents) {
                if (locked == null) {
                    continue;
                }
                inv.add(locked.build());
            }
        }
        return inv;
    }

    private ItemInventory(Examinable.Name name) {
        this.name = name;
    }

    private final Examinable.Name name;
    private final LinkedHashMap<ItemID, Item> cargo = new LinkedHashMap<>();

    @Override
    public ImmutableSortedMap<String, String> attributes() {
        return ItemInventory.BASIC_TAGGABLE_ATTRIBUTES;
    }

    @Override
    public Examinable.Name name() {
        return this.name;
    }

    public ItemInventory add(Item... items) {
        if (items != null) {
            for (final Item item : items) {
                if (item != null) {
                    this.cargo.put(item.itemID(), item);
                }
            }
        }
        return this;
    }

    public ItemInventory add(Collection<Item> items) {
        if (items != null) {
            for (final Item item : items) {
                if (item != null) {
                    this.cargo.put(item.itemID(), item);
                }
            }
        }
        return this;
    }

    public ItemInventory addAll(Map<ItemID, Item> items) {
        if (items != null) {
            this.cargo.putAll(items);
        }
        return this;
    }

    public Optional<Item> remove(ItemID id) {
        return Optional.ofNullable(this.cargo.remove(id));
    }

    public Optional<Item> remove(Item item) {
        return this.remove(item.itemID());
    }

    @Override
    public boolean hasItem(Item item) {
        return this.cargo.containsValue(item);
    }

    @Override
    public Optional<Item> byItemID(ItemID id) {
        return Optional.ofNullable(this.cargo.get(id));
    }

    @Override
    public Stream<Item> items() {
        return this.cargo.values().stream().sequential();
    }

    public ItemInventory.Builder toBuilder() {
        Builder builder = ItemInventory.builder().setName(name);
        for (final Item itemReference : cargo.values()) {
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
