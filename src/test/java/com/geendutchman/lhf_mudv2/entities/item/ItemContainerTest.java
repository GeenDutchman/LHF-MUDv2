package com.geendutchman.lhf_mudv2.entities.item;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ItemContainerTest {
    @Test
    void testQueryAll(@Autowired ItemBuilderFactory factory) {
        Item itemA = factory.builder().setName("itemA").build();
        Item itemB = factory.builder().setName("itemB").build();
        Item maskedItem = factory.builder().setName("hidden").setNickname(Optional.of("itemX")).build();
        ItemContainer container = ItemContainer.ImmutableItemContainer.builder().addItem(maskedItem).addItem(itemA)
                .addItem(itemB).setName("container").build();
        ItemQuery query = ItemQuery.builder().setCheckOnlyName(false).addNamePattern("^item").build();
        ItemContainerSubject.assertThat(container).queryAll(query).hasSize(3);
        ItemContainerSubject.assertThat(container).queryAll(query.withCheckOnlyName(true)).hasSize(2);
    }
}
