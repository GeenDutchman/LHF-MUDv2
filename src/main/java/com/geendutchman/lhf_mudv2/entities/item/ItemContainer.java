package com.geendutchman.lhf_mudv2.entities.item;

import java.util.Optional;
import java.util.stream.Stream;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.display.Taggable;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.google.common.collect.ImmutableSortedMap;

public interface ItemContainer extends Examinable {

    public abstract Stream<Item> items();

    public abstract boolean hasItem(Item item);

    public abstract Optional<Item> byItemID(ItemID id);

    @Override
    public default Optional<RichOutput> description() {
        RichOutput.Builder builder = RichOutput.builder().setOnEmpty(Optional.of("It is empty"))
                .setTag(Optional.ofNullable(this.tag() + "-description"));
        this.items().forEachOrdered(item -> builder.addTaggable(item));
        return Optional.of(builder.build());
    }

    final static Taggable.Tag ITEM_CONTAINER_TAG = new Taggable.Tag("Items");

    @Override
    public default Taggable.Tag tag() {
        return ITEM_CONTAINER_TAG;
    }

    @Override
    public default String content() {
        return this.name().toString();
    }

    public default Optional<Item> queryOneItem(ItemQuery query) {
        return this.items().sequential().filter(item -> query != null ? query.test(item) : item != null).findFirst();
    }

    public default Optional<Item> queryOneItem(IEntityQuery<? super Item> query) {
        return this.items().sequential().filter(item -> query != null ? query.test(item) : item != null).findFirst();
    }

    @Override
    public abstract ImmutableSortedMap<String, String> attributes();

    public default ItemContainer queryItems(IEntityQuery<? super Item> query) {
        ImmutableSortedMap.Builder<ItemID, Item> builder = ImmutableSortedMap.naturalOrder();
        this.items().sequential().filter(item -> query != null ? query.test(item) : item != null)
                .forEach(item -> builder.put(item.itemID(), item));
        ImmutableSortedMap<ItemID, Item> built = builder.build();
        final Examinable.Name resultName = new Examinable.Name("ItemQueryResult");
        return new ItemContainer() {

            @Override
            public Examinable.Name name() {
                return resultName;
            }

            @Override
            public Stream<Item> items() {
                return built.values().stream().sequential();
            }

            @Override
            public boolean hasItem(Item item) {
                return built.containsValue(item);
            }

            @Override
            public Optional<Item> byItemID(ItemID id) {
                return Optional.ofNullable(built.get(id));
            }

            @Override
            public ImmutableSortedMap<String, String> attributes() {
                return ImmutableSortedMap.of();
            }

        };
    }

}
