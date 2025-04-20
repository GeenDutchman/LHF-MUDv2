package com.geendutchman.lhf_mudv2.entities.item;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ItemTest {
    @Test
    void testDisplayName(@Autowired ItemBuilderFactory factory) {
        Item itemOne = factory.builder().setName("dingus").build();
        Item itemTwo = factory.builder().setName("dongus").setNickname(Optional.of("alakazam")).build();
        ItemSubject.assertThat(itemOne).isNotEqualTo(itemTwo);
        ItemSubject.assertThat(itemOne).displayName().isEqualTo("dingus");
        ItemSubject.assertThat(itemTwo).displayName().isEqualTo("alakazam");
        ItemSubject.assertThat(itemTwo).name().isEqualTo("dongus");
    }

    @Test
    void testDelta(@Autowired ItemBuilderFactory factory) {
        final Item itemOne = factory.builder().setName("Thingy").build();
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

    @Test
    void testRelativePath() {
        final URI relative = URI.create("items/123345");
        System.out.println(relative);
        System.out.println(URI.create("items/*"));
    }

}
