package com.geendutchman.lhf_mudv2.entities.item;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ItemContainerTest {
    @Test
    void testQueryAll(@Autowired ItemBuilderFactory factory) {
        Item itemA = ItemBuilderFactory.builder().setName("itemA").build(factory);
        Item itemB = ItemBuilderFactory.builder().setName("itemB").build(factory);
        Item maskedItem = ItemBuilderFactory.builder().setName("hidden").setNickname("itemX").build(factory);
        ItemContainer container = ItemInventory.builder().setName("container").build(factory).add(maskedItem).add(itemA)
                .add(itemB);
        ItemQuery query = ItemQuery.builder().setDisplayNamePattern("^item").build();
        ItemContainerSubject.assertThat(container).queryAll(query).hasSize(3);
        ItemContainerSubject.assertThat(container).queryAll(query.toBuilder().setDisplayNamePattern(Optional.empty())
                .adjustEntityQuery(q -> q.setNamePattern("^item")).build()).hasSize(2);
    }
}
