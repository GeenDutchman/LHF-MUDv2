package com.geendutchman.lhf_mudv2.item;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.geendutchman.lhf_mudv2.item.ItemContainer.Query;

public class ItemContainerTest {
    @Test
    void testQueryAll() {
        Item itemA = Item.ImmutableItem.builder().setName("itemA").build();
        Item itemB = Item.ImmutableItem.builder().setName("itemB").build();
        Item maskedItem = Item.ImmutableItem.builder().setName("hidden").setNickname(Optional.of("itemX")).build();
        ItemContainer container = ItemContainer.ImmutableItemContainer.builder().addItem(maskedItem).addItem(itemA)
                .addItem(itemB).setName("container").build();
        Query query = Query.builder().setCheckOnlyName(false).addNamePattern("^item").build();
        ItemContainerSubject.assertThat(container).queryAll(query).hasSize(3);
        ItemContainerSubject.assertThat(container).queryAll(query.withCheckOnlyName(true)).hasSize(2);
    }
}
