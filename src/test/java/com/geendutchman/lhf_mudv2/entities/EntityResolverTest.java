package com.geendutchman.lhf_mudv2.entities;

import java.net.URI;
import java.util.SortedSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemBuilderFactory;
import com.google.common.truth.Truth;

@SpringBootTest
public class EntityResolverTest {
    @Test
    void testResolve(@Autowired EntityResolver resolver, @Autowired ItemBuilderFactory itemFactory) {
        Item itemOne = itemFactory.builder().setName("dingus").build();
        IEntityID id = itemOne.identifier();
        URI uri = id.uri();
        SortedSet<Entity> found = resolver.resolve(uri);
        Truth.assertWithMessage("found the wrong number of entities in '%s", found).that(found).hasSize(1);
        Entity thing = found.first();
        Truth.assertThat(thing.name()).isEqualTo(itemOne.name());
    }
}
