package com.geendutchman.lhf_mudv2.item;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.geendutchman.lhf_mudv2.item.Item.ImmutableItem;

public class ItemTest {
    @Test
    void testDisplayName() {
        Item itemOne = ImmutableItem.builder().setName("dingus").build();
        Item itemTwo = ImmutableItem.builder().setName("dongus").setNickname(Optional.of("alakazam")).build();
        ItemSubject.assertThat(itemOne).isNotEqualTo(itemTwo);
        ItemSubject.assertThat(itemOne).displayName().isEqualTo("dingus");
        ItemSubject.assertThat(itemTwo).displayName().isEqualTo("alakazam");
        ItemSubject.assertThat(itemTwo).name().isEqualTo("dongus");
    }
}
