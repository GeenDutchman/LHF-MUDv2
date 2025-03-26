package com.geendutchman.lhf_mudv2.item;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class ItemTest {
    @Test
    void testDisplayName() {
        Item itemOne = Item.builder().setName("dingus").build();
        Item itemTwo = Item.builder().setName("dongus").setNickname(Optional.of("alakazam")).build();
        ItemSubject.assertThat(itemOne).isNotEqualTo(itemTwo);
        ItemSubject.assertThat(itemOne).displayName().isEqualTo("dingus");
        ItemSubject.assertThat(itemTwo).displayName().isEqualTo("alakazam");
        ItemSubject.assertThat(itemTwo).name().isEqualTo("dongus");
    }

    @Test
    void testDelta() {
        final Item itemOne = Item.builder().setName("Thingy").build();
        ItemSubject.assertThat(itemOne).nickname().isEmpty();
        ItemSubject.assertThat(itemOne).visibility().isTrue();
        Item.Delta delta = Item.Delta.ofNickname(Optional.of("Dingus"));
        itemOne.applyDelta(delta);
        ItemSubject.assertThat(itemOne).nickname().hasValue("Dingus");
        ItemSubject.assertThat(itemOne).visibility().isTrue();

        Item.Delta delta2 = Item.Delta.ofVisibility(false);
        itemOne.applyDelta(delta2);
        ItemSubject.assertThat(itemOne).visibility().isFalse();
    }
}
