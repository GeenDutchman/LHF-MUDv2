package com.geendutchman.lhf_mudv2.entities.item;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.TreeSet;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory.LockedItemBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

public final class ItemInventory implements ItemContainer {

    public static final class Builder {
        private Examinable.Name name;
        private SequencedSet<ItemBuilderFactory.LockedItemBuilder> contents;

        private Builder() {
            this.name = null;
            this.contents = new LinkedHashSet<>();
        }

        public Examinable.Name name() {
            if (this.name == null) {
                throw new IllegalStateException("Property \"name\" has not been set");
            }
            return this.name;
        }

        public Builder setName(Examinable.Name name) {
            if (name == null) {
                throw new NullPointerException("Null name");
            }
            this.name = name;
            return this;
        }

        public Builder setName(String name) {
            Examinable.Name eName = new Examinable.Name(name);
            return this.setName(eName);
        }

        public SequencedSet<ItemBuilderFactory.LockedItemBuilder> contents() {
            if (this.contents == null) {
                throw new IllegalStateException("Property \"contents\" has not been set");
            }
            return contents;
        }

        public ItemInventory.Builder setContents(SequencedSet<ItemBuilderFactory.LockedItemBuilder> contents) {
            if (contents == null) {
                throw new NullPointerException("Null contents");
            }
            this.contents = contents;
            return this;
        }

        public final Builder addContents(ItemBuilderFactory.LockedItemBuilder... items) {
            SequencedSet<ItemBuilderFactory.LockedItemBuilder> mycontents;
            try {
                mycontents = this.contents();
            } catch (IllegalStateException e) {
                mycontents = new TreeSet<>(Comparator.<ItemBuilderFactory.LockedItemBuilder, String>comparing(
                        locked -> String.format("%s:%s:%s", locked.getName(),
                                locked.getNickname().map(nn -> nn.toString()).orElse(""), locked.builderTsid())));
            }
            for (final ItemBuilderFactory.LockedItemBuilder item : items) {
                if (item != null) {
                    mycontents.add(item);
                }
            }
            return this.setContents(mycontents);
        }

        public final ItemInventory build(ItemBuilderFactory itemFactory) {
            Preconditions.checkNotNull(itemFactory, "item factory should be provided and not be null");
            Preconditions.checkState(this.name != null, "name should not be null");
            Preconditions.checkState(this.contents != null, "contents may be empty, but must not be null");
            final ItemInventory built = new ItemInventory(this.name);
            for (final LockedItemBuilder lockedItemBuilder : contents) {
                if (lockedItemBuilder == null) {
                    continue;
                }
                Item builtItem = lockedItemBuilder.build(itemFactory);
                built.add(builtItem);
            }
            return built;
        }
    }

    public static Builder builder() {
        return new Builder().setName("Inventory");
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
    public ImmutableSet<Item> items() {
        return ImmutableSet.copyOf(this.cargo.values());
    }

    public ItemInventory.Builder toBuilder() {
        Builder builder = ItemInventory.builder().setName(name);
        for (final Item itemReference : cargo.values()) {
            if (itemReference == null) {
                continue;
            }
            final ItemBuilderFactory.BuildItem itemBuilder = ItemBuilderFactory.builder().setName(itemReference.name())
                    .setItemTag(itemReference.itemTag()).setNickname(itemReference.nickname())
                    .setVisibility(itemReference.visibility());
            builder.addContents(itemBuilder.lock());
        }
        return builder;
    }

}
