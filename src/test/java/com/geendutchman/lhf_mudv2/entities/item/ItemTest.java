package com.geendutchman.lhf_mudv2.entities.item;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.geendutchman.lhf_mudv2.dice.DifficultyMods;
import com.geendutchman.lhf_mudv2.dice.Plain;

@SpringBootTest
public class ItemTest {
    @Test
    void testDisplayName(@Autowired ItemBuilderFactory factory) {
        Item itemOne = ItemBuilderFactory.builder().setName("dingus").build(factory);
        Item itemTwo = ItemBuilderFactory.builder().setName("dongus").setNickname("alakazam").build(factory);
        ItemSubject.assertThat(itemOne).isNotEqualTo(itemTwo);
        ItemSubject.assertThat(itemOne).displayName().isEqualTo("dingus");
        ItemSubject.assertThat(itemTwo).displayName().isEqualTo("alakazam");
        ItemSubject.assertThat(itemTwo).name().isEqualTo("dongus");
    }

    @Test
    void testDelta(@Autowired ItemBuilderFactory factory) {
        final Item itemOne = ItemBuilderFactory.builder().setName("Thingy").build(factory);
        ItemSubject.assertThat(itemOne).nickname().isEmpty();
        ItemSubject.assertThat(itemOne).visibility().sum().isEqualTo(0);
        Item.Delta delta = Item.Delta.ofNickname("Dingus");
        itemOne.applyDelta(delta);
        ItemSubject.assertThat(itemOne).nickname().hasValue("Dingus");
        ItemSubject.assertThat(itemOne).visibility().sum().isEqualTo(0);

        Item.Delta delta2 = Item.Delta.ofVisibility(new DifficultyMods.FlavoredBonus<>(Plain.UNFLAVORED, 30));
        itemOne.applyDelta(delta2);
        ItemSubject.assertThat(itemOne).visibility().sum().isAtLeast(30);
    }

    @Test
    void testRelativePath() {
        final URI relative = URI.create("items/123345");
        LoggerFactory.getLogger(getClass()).atInfo().addKeyValue("relative", relative)
                .addKeyValue("fresh", URI.create("items/*")).log("made URIs");
    }

}
